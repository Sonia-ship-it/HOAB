package rw.ac.rca.hoabs.appointment;

import java.time.Instant;
import java.util.UUID;

public record AppointmentResponse(
        UUID id,
        String referenceNo,
        UUID patientId,
        String patientName,
        String patientPhone,
        UUID doctorId,
        String doctorName,
        String doctorStaffId,
        String departmentName,
        UUID slotId,
        Instant slotStartTime,
        Instant slotEndTime,
        String reason,
        AppointmentStatus status,
        String cancellationReason,
        String overrideReason,
        Instant createdAt
) {
    public static AppointmentResponse from(Appointment a) {
        return new AppointmentResponse(
                a.getId(),
                a.getReferenceNo(),
                a.getPatient().getId(),
                a.getPatient().getFullName(),
                a.getPatient().getPhone(),
                a.getDoctor().getId(),
                a.getDoctor().getUser().getFullName(),
                a.getDoctor().getStaffId(),
                a.getDoctor().getDepartment().getName(),
                a.getSlot().getId(),
                a.getSlot().getStartTime(),
                a.getSlot().getEndTime(),
                a.getReason(),
                a.getStatus(),
                a.getCancellationReason(),
                a.getOverrideReason(),
                a.getCreatedAt()
        );
    }
}
