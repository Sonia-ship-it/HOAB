package rw.ac.rca.hoabs.identity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.rca.hoabs.common.exception.ApiException;
import rw.ac.rca.hoabs.config.HoabsProperties;
import rw.ac.rca.hoabs.notification.NotificationService;
import rw.ac.rca.hoabs.security.JwtService;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final NotificationService notificationService;
    private final HoabsProperties properties;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       NotificationService notificationService,
                       HoabsProperties properties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.notificationService = notificationService;
        this.properties = properties;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        log.info("Registration attempt for phone: {}", maskPhone(req.phone()));

        if (userRepository.existsByPhone(req.phone())) {
            throw ApiException.conflict("Phone number already registered. Please log in.");
        }
        if (req.email() != null && !req.email().isBlank() && userRepository.existsByEmail(req.email())) {
            throw ApiException.conflict("Email address already registered. Please log in.");
        }

        User user = new User();
        user.setFullName(req.fullName().trim());
        user.setPhone(req.phone().trim());
        user.setEmail(req.email() != null ? req.email().trim().toLowerCase() : null);
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(Role.PATIENT);
        user.setStatus(UserStatus.ACTIVE);
        user.setDateOfBirth(req.dateOfBirth());
        user.setInsuranceNote(req.insuranceNote());

        User saved = userRepository.save(user);
        notificationService.sendWelcome(saved);

        String token = jwtService.generateToken(saved);
        return AuthResponse.of(token, jwtService.getTtlSeconds(), saved);
    }

    @Transactional(noRollbackFor = BadCredentialsException.class)
    public AuthResponse login(LoginRequest req) {
        String identifier = req.identifier().trim();
        log.info("Login attempt for identifier: {}", maskIdentifier(identifier));

        Optional<User> userOpt = userRepository.findByPhoneOrEmail(identifier);

        if (userOpt.isEmpty()) {
            // Constant-time dummy verification or generic failure
            throw new BadCredentialsException("Phone/email or password is incorrect.");
        }

        User user = userOpt.get();

        // Check if account is locked
        if (user.getStatus() == UserStatus.LOCKED) {
            if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
                log.warn("Login rejected for locked account: id={}", user.getId());
                // Generic error: never confirm account exists or is locked (FR-AUTH-05, UAT AU-005)
                throw new BadCredentialsException("Phone/email or password is incorrect.");
            } else {
                // Lock expired
                user.setStatus(UserStatus.ACTIVE);
                user.setFailedAttempts(0);
                user.setLockedUntil(null);
            }
        }

        if (user.getStatus() == UserStatus.DISABLED) {
            throw new BadCredentialsException("Phone/email or password is incorrect.");
        }

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            int newAttempts = user.getFailedAttempts() + 1;
            user.setFailedAttempts(newAttempts);

            int maxAttempts = 3;
            long durationSeconds = 1800;
            if (properties != null && properties.lockout() != null) {
                if (properties.lockout().maxAttempts() > 0) {
                    maxAttempts = properties.lockout().maxAttempts();
                }
                if (properties.lockout().duration() != null) {
                    durationSeconds = properties.lockout().duration().toSeconds();
                }
            }

            if (newAttempts >= maxAttempts) {
                user.setStatus(UserStatus.LOCKED);
                user.setLockedUntil(Instant.now().plusSeconds(durationSeconds));
                notificationService.sendSecurityAlert(user, "Account locked for 30 minutes after 3 consecutive failed login attempts.");
                log.warn("Account locked due to 3 failed attempts: id={}", user.getId());
            }

            userRepository.save(user);
            throw new BadCredentialsException("Phone/email or password is incorrect.");
        }

        // Login successful
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        String token = jwtService.generateToken(user);
        return AuthResponse.of(token, jwtService.getTtlSeconds(), user);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found"));
        return UserResponse.from(user);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return "***";
        return phone.substring(0, 3) + "***" + phone.substring(phone.length() - 3);
    }

    private String maskIdentifier(String id) {
        if (id == null || id.length() < 4) return "***";
        return id.substring(0, 2) + "***" + id.substring(id.length() - 2);
    }
}
