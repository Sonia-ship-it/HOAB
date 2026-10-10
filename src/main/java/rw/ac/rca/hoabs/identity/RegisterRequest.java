package rw.ac.rca.hoabs.identity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record RegisterRequest(
        @NotBlank(message = "Full name is required")
        String fullName,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^07[2389][0-9]{7}$", message = "Phone must be a valid Rwandan mobile number (07X, 10 digits)")
        String phone,

        @Email(message = "Email must be a valid email address")
        String email,

        @NotBlank(message = "Password is required")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$", message = "Password must be at least 8 characters long and contain at least one letter and one digit")
        String password,

        LocalDate dateOfBirth,

        String insuranceNote
) {}
