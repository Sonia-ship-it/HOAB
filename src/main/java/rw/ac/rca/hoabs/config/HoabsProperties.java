package rw.ac.rca.hoabs.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "hoabs")
public record HoabsProperties(
        Jwt jwt,
        Lockout lockout,
        Payment payment,
        Cors cors,
        boolean seedEnabled
) {
    public record Jwt(
            String secret,
            String issuer,
            Duration ttl,
            Duration idleTimeout
    ) {}

    public record Lockout(
            int maxAttempts,
            Duration duration
    ) {}

    public record Payment(
            String stubMode
    ) {}

    public record Cors(
            List<String> allowedOrigins
    ) {}
}
