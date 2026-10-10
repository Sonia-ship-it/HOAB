package rw.ac.rca.hoabs.doctor;

import java.util.List;

public record DoctorScheduleDto(
        DoctorResponse doctor,
        int slotDurationMinutes,
        List<WorkingHoursDto> workingHours,
        List<LeaveBlockDto> leaveBlocks
) {}
