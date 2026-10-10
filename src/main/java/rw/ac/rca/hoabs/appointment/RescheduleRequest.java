package rw.ac.rca.hoabs.appointment;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RescheduleRequest(
        @NotNull(message = "New slot selection is required")
        UUID newSlotId,

        String overrideReason
) {}
