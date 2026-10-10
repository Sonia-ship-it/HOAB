package rw.ac.rca.hoabs.department;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.UUID;

public record DepartmentDto(
        UUID id,
        @NotBlank(message = "Department name is required")
        String name,
        String description,
        @NotNull(message = "Opening time is required")
        LocalTime openingTime,
        @NotNull(message = "Closing time is required")
        LocalTime closingTime,
        Boolean active
) {
    public static DepartmentDto from(Department d) {
        return new DepartmentDto(
                d.getId(),
                d.getName(),
                d.getDescription(),
                d.getOpeningTime(),
                d.getClosingTime(),
                d.isActive()
        );
    }
}
