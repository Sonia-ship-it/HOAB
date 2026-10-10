package rw.ac.rca.hoabs.appointment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AppointmentRequest(
        @NotNull(message = "Slot selection is required")
        UUID slotId,

        @NotBlank(message = "Reason for visit is required")
        @Size(max = 200, message = "Reason for visit cannot exceed 200 characters")
        String reason,

        String paymentProvider,

        // Optional patient ID for receptionist walk-in booking (FR-ADM-06)
        UUID patientId,

        // Optional override reason for receptionist overriding the 2-hour cut-off (FR-ADM-07)
        String overrideReason
) {}
