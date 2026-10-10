package rw.ac.rca.hoabs.appointment;

public record CancelRequest(
        String reason,
        String overrideReason
) {}
