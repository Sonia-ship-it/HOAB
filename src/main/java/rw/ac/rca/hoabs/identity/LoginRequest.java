package rw.ac.rca.hoabs.identity;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Phone number or email is required")
        String identifier,

        @NotBlank(message = "Password is required")
        String password
) {}
