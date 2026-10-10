package rw.ac.rca.hoabs.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record CreateDoctorRequest(
        @NotBlank(message = "Staff ID is required")
        String staffId,

        @NotBlank(message = "Full name is required")
        String fullName,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^07[2389][0-9]{7}$", message = "Phone must be a valid Rwandan mobile number")
        String phone,

        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Password is required")
        String password,

        @NotNull(message = "Department ID is required")
        UUID departmentId,

        @NotBlank(message = "Specialty is required")
        String specialty,

        Integer slotDurationMinutes,

        Long consultationFeeRwf
) {}
