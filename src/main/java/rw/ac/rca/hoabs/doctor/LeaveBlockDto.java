package rw.ac.rca.hoabs.doctor;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record LeaveBlockDto(
        UUID id,
        @NotNull(message = "Start date is required")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        LocalDate endDate,

        String reason
) {
    public static LeaveBlockDto from(LeaveBlock lb) {
        return new LeaveBlockDto(lb.getId(), lb.getStartDate(), lb.getEndDate(), lb.getReason());
    }
}
