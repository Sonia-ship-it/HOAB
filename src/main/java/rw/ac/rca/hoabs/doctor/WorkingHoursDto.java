package rw.ac.rca.hoabs.doctor;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.UUID;

public record WorkingHoursDto(
        UUID id,
        @NotNull(message = "Day of week is required")
        @Min(1) @Max(7)
        int dayOfWeek,

        @NotNull(message = "Start time is required")
        LocalTime startTime,

        @NotNull(message = "End time is required")
        LocalTime endTime
) {
    public static WorkingHoursDto from(WorkingHours wh) {
        return new WorkingHoursDto(wh.getId(), wh.getDayOfWeek(), wh.getStartTime(), wh.getEndTime());
    }
}
