package rw.ac.rca.hoabs.identity;

import java.time.LocalDate;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String fullName,
        String phone,
        String email,
        Role role,
        UserStatus status,
        LocalDate dateOfBirth,
        String insuranceNote
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getPhone(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getDateOfBirth(),
                user.getInsuranceNote()
        );
    }
}
