package rw.ac.rca.hoabs.doctor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.rca.hoabs.appointment.Appointment;
import rw.ac.rca.hoabs.appointment.AppointmentRepository;
import rw.ac.rca.hoabs.appointment.AppointmentStateMachine;
import rw.ac.rca.hoabs.appointment.AppointmentStatus;
import rw.ac.rca.hoabs.common.exception.ApiException;
import rw.ac.rca.hoabs.department.Department;
import rw.ac.rca.hoabs.slot.Slot;
import rw.ac.rca.hoabs.slot.SlotGeneratorService;
import rw.ac.rca.hoabs.slot.SlotRepository;

import java.time.*;
import java.util.List;
import java.util.UUID;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final WorkingHoursRepository workingHoursRepository;
    private final LeaveBlockRepository leaveBlockRepository;
    private final SlotRepository slotRepository;
    private final AppointmentRepository appointmentRepository;
    private final SlotGeneratorService slotGeneratorService;

    public DoctorService(DoctorRepository doctorRepository,
                         WorkingHoursRepository workingHoursRepository,
                         LeaveBlockRepository leaveBlockRepository,
                         SlotRepository slotRepository,
                         AppointmentRepository appointmentRepository,
                         SlotGeneratorService slotGeneratorService) {
        this.doctorRepository = doctorRepository;
        this.workingHoursRepository = workingHoursRepository;
        this.leaveBlockRepository = leaveBlockRepository;
        this.slotRepository = slotRepository;
        this.appointmentRepository = appointmentRepository;
        this.slotGeneratorService = slotGeneratorService;
    }

    @Transactional(readOnly = true)
    public List<DoctorResponse> searchActiveDoctors(UUID departmentId, String query) {
        Instant now = Instant.now();
        return doctorRepository.searchActiveDoctors(departmentId, query)
                .stream()
                .map(doc -> {
                    Instant nextSlot = slotRepository
                            .findFirstByDoctorIdAndIsOpenTrueAndStartTimeAfterOrderByStartTimeAsc(doc.getId(), now)
                            .map(Slot::getStartTime)
                            .orElse(null);
                    return DoctorResponse.from(doc, nextSlot);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public DoctorResponse getDoctorProfile(UUID doctorId) {
        Doctor doc = doctorRepository.findById(doctorId)
                .orElseThrow(() -> ApiException.notFound("Doctor not found with id: " + doctorId));
        Instant nextSlot = slotRepository
                .findFirstByDoctorIdAndIsOpenTrueAndStartTimeAfterOrderByStartTimeAsc(doc.getId(), Instant.now())
                .map(Slot::getStartTime)
                .orElse(null);
        return DoctorResponse.from(doc, nextSlot);
    }

    @Transactional(readOnly = true)
    public DoctorScheduleDto getDoctorSchedule(UUID doctorId) {
        Doctor doc = doctorRepository.findById(doctorId)
                .orElseThrow(() -> ApiException.notFound("Doctor not found with id: " + doctorId));
        List<WorkingHoursDto> hours = workingHoursRepository.findByDoctorId(doctorId).stream()
                .map(WorkingHoursDto::from).toList();
        List<LeaveBlockDto> leaves = leaveBlockRepository.findByDoctorId(doctorId).stream()
                .map(LeaveBlockDto::from).toList();
        Instant nextSlot = slotRepository
                .findFirstByDoctorIdAndIsOpenTrueAndStartTimeAfterOrderByStartTimeAsc(doc.getId(), Instant.now())
                .map(Slot::getStartTime)
                .orElse(null);
        return new DoctorScheduleDto(DoctorResponse.from(doc, nextSlot), doc.getSlotDurationMinutes(), hours, leaves);
    }

    @Transactional
    public void setWorkingHours(UUID doctorId, List<WorkingHoursDto> hoursList, Integer slotDuration) {
        Doctor doc = doctorRepository.findById(doctorId)
                .orElseThrow(() -> ApiException.notFound("Doctor not found with id: " + doctorId));

        Department dept = doc.getDepartment();

        // FR-SCHED-06 / DR-004: Validate working hours fall within department opening hours
        for (WorkingHoursDto whDto : hoursList) {
            if (whDto.startTime().isBefore(dept.getOpeningTime()) || whDto.endTime().isAfter(dept.getClosingTime())) {
                throw ApiException.badRequest(String.format(
                        "Working hours (%s - %s) must fall within department opening hours (%s - %s).",
                        whDto.startTime(), whDto.endTime(), dept.getOpeningTime(), dept.getClosingTime()
                ));
            }
            if (!whDto.startTime().isBefore(whDto.endTime())) {
                throw ApiException.badRequest("Start time must be before end time.");
            }
        }

        if (slotDuration != null && slotDuration >= 10 && slotDuration <= 120) {
            doc.setSlotDurationMinutes(slotDuration);
            doctorRepository.save(doc);
        }

        List<WorkingHours> existingWh = workingHoursRepository.findByDoctorId(doctorId);
        workingHoursRepository.deleteAll(existingWh);
        workingHoursRepository.flush();

        for (WorkingHoursDto dto : hoursList) {
            WorkingHours wh = new WorkingHours();
            wh.setDoctor(doc);
            wh.setDayOfWeek(dto.dayOfWeek());
            wh.setStartTime(dto.startTime());
            wh.setEndTime(dto.endTime());
            workingHoursRepository.save(wh);
        }

        slotGeneratorService.regenerateFutureUnbookedSlots(doc);
    }

    @Transactional
    public LeaveBlockDto addLeaveBlock(UUID doctorId, LeaveBlockDto dto) {
        Doctor doc = doctorRepository.findById(doctorId)
                .orElseThrow(() -> ApiException.notFound("Doctor not found with id: " + doctorId));

        if (dto.startDate().isAfter(dto.endDate())) {
            throw ApiException.badRequest("Leave start date must be before or equal to end date.");
        }

        // Convert date range to Instant range in Kigali zone
        Instant rangeStart = dto.startDate().atStartOfDay(SlotGeneratorService.HOSPITAL_ZONE).toInstant();
        Instant rangeEnd = dto.endDate().plusDays(1).atStartOfDay(SlotGeneratorService.HOSPITAL_ZONE).toInstant();

        // FR-SCHED-05 / DR-003: Check for existing Confirmed appointments in this range
        List<Appointment> conflicts = appointmentRepository.findConflictsInRange(doctorId, rangeStart, rangeEnd);
        if (!conflicts.isEmpty()) {
            throw ApiException.conflict(String.format(
                    "Cannot save leave block: %d Confirmed appointment(s) exist on the selected dates. Please reschedule or resolve conflicting bookings first.",
                    conflicts.size()
            ));
        }

        LeaveBlock lb = new LeaveBlock();
        lb.setDoctor(doc);
        lb.setStartDate(dto.startDate());
        lb.setEndDate(dto.endDate());
        lb.setReason(dto.reason());
        LeaveBlock saved = leaveBlockRepository.save(lb);

        // Delete open future slots on leave days
        slotGeneratorService.regenerateFutureUnbookedSlots(doc);

        return LeaveBlockDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<Appointment> getTodayAppointments(UUID doctorId) {
        LocalDate today = LocalDate.now(SlotGeneratorService.HOSPITAL_ZONE);
        Instant dayStart = today.atStartOfDay(SlotGeneratorService.HOSPITAL_ZONE).toInstant();
        Instant dayEnd = today.plusDays(1).atStartOfDay(SlotGeneratorService.HOSPITAL_ZONE).toInstant();

        return appointmentRepository.findConfirmedAppointmentsForDoctorOnDate(doctorId, dayStart, dayEnd);
    }

    @Transactional
    public void markAppointmentOutcome(UUID doctorId, UUID appointmentId, AppointmentStatus targetStatus) {
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> ApiException.notFound("Appointment not found with id: " + appointmentId));

        if (!appt.getDoctor().getId().equals(doctorId)) {
            throw ApiException.forbidden("You cannot update another doctor's appointments.");
        }

        AppointmentStateMachine.assertTransition(appt.getStatus(), targetStatus);
        appt.setStatus(targetStatus);
        appointmentRepository.save(appt);
    }
}
