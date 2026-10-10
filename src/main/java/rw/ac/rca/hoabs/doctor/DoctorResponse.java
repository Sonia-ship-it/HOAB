package rw.ac.rca.hoabs.doctor;

import java.time.Instant;
import java.util.UUID;

public record DoctorResponse(
        UUID id,
        String staffId,
        String fullName,
        String phone,
        String email,
        String specialty,
        UUID departmentId,
        String departmentName,
        int slotDurationMinutes,
        long consultationFeeRwf,
        boolean active,
        Instant nextAvailableSlot
) {
    public static DoctorResponse from(Doctor doc, Instant nextSlot) {
        return new DoctorResponse(
                doc.getId(),
                doc.getStaffId(),
                doc.getUser().getFullName(),
                doc.getUser().getPhone(),
                doc.getUser().getEmail(),
                doc.getSpecialty(),
                doc.getDepartment().getId(),
                doc.getDepartment().getName(),
                doc.getSlotDurationMinutes(),
                doc.getConsultationFeeRwf(),
                doc.isActive(),
                nextSlot
        );
    }
}
