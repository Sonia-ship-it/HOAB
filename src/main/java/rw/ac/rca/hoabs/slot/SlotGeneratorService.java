package rw.ac.rca.hoabs.slot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.rca.hoabs.doctor.Doctor;
import rw.ac.rca.hoabs.doctor.LeaveBlock;
import rw.ac.rca.hoabs.doctor.LeaveBlockRepository;
import rw.ac.rca.hoabs.doctor.WorkingHours;
import rw.ac.rca.hoabs.doctor.WorkingHoursRepository;

import java.time.*;
import java.util.List;
import java.util.Optional;

@Service
public class SlotGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(SlotGeneratorService.class);
    public static final ZoneId HOSPITAL_ZONE = ZoneId.of("Africa/Kigali");

    private final SlotRepository slotRepository;
    private final WorkingHoursRepository workingHoursRepository;
    private final LeaveBlockRepository leaveBlockRepository;

    public SlotGeneratorService(SlotRepository slotRepository,
                                WorkingHoursRepository workingHoursRepository,
                                LeaveBlockRepository leaveBlockRepository) {
        this.slotRepository = slotRepository;
        this.workingHoursRepository = workingHoursRepository;
        this.leaveBlockRepository = leaveBlockRepository;
    }

    @Transactional
    public void generateSlotsForDoctor(Doctor doctor, int daysAhead) {
        if (!doctor.isActive()) {
            return;
        }

        List<WorkingHours> hoursList = workingHoursRepository.findByDoctorId(doctor.getId());
        if (hoursList.isEmpty()) {
            return;
        }

        LocalDate today = LocalDate.now(HOSPITAL_ZONE);
        Instant nowInstant = Instant.now();

        int duration = doctor.getSlotDurationMinutes() > 0 ? doctor.getSlotDurationMinutes() : 20;

        for (int i = 0; i <= daysAhead; i++) {
            LocalDate date = today.plusDays(i);

            // Check if covered by leave
            List<LeaveBlock> leaves = leaveBlockRepository.findLeaveOnDate(doctor.getId(), date);
            if (!leaves.isEmpty()) {
                // If there's leave, don't generate slots. If unbooked open slots exist, close/remove them.
                continue;
            }

            int dayOfWeek = date.getDayOfWeek().getValue(); // 1 = Monday .. 7 = Sunday
            Optional<WorkingHours> whOpt = hoursList.stream()
                    .filter(wh -> wh.getDayOfWeek() == dayOfWeek)
                    .findFirst();

            if (whOpt.isEmpty()) {
                continue;
            }

            WorkingHours wh = whOpt.get();
            LocalTime start = wh.getStartTime();
            LocalTime end = wh.getEndTime();

            LocalTime currentSlotStart = start;
            while (currentSlotStart.plusMinutes(duration).compareTo(end) <= 0) {
                LocalTime currentSlotEnd = currentSlotStart.plusMinutes(duration);

                ZonedDateTime slotStartLocal = ZonedDateTime.of(date, currentSlotStart, HOSPITAL_ZONE);
                ZonedDateTime slotEndLocal = ZonedDateTime.of(date, currentSlotEnd, HOSPITAL_ZONE);

                Instant startInstant = slotStartLocal.toInstant();
                Instant endInstant = slotEndLocal.toInstant();

                // Only generate for future slots
                if (startInstant.isAfter(nowInstant)) {
                    Optional<Slot> existing = slotRepository.findByDoctorIdAndStartTime(doctor.getId(), startInstant);
                    if (existing.isEmpty()) {
                        Slot slot = new Slot();
                        slot.setDoctor(doctor);
                        slot.setStartTime(startInstant);
                        slot.setEndTime(endInstant);
                        slot.setIsOpen(true);
                        slotRepository.save(slot);
                    }
                }

                currentSlotStart = currentSlotEnd;
            }
        }
        log.info("Completed slot generation for doctor {} ({}) up to {} days", doctor.getStaffId(), doctor.getId(), daysAhead);
    }

    @Transactional
    public void regenerateFutureUnbookedSlots(Doctor doctor) {
        Instant now = Instant.now();
        // Delete only unbooked open future slots so existing bookings remain untouched (FR-SCHED-05, DR-005)
        slotRepository.deleteOpenSlotsWithoutAppointments(doctor.getId(), now);
        slotRepository.flush();
        generateSlotsForDoctor(doctor, 14);
    }
}
