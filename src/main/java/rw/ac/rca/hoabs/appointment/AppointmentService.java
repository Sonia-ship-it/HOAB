package rw.ac.rca.hoabs.appointment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.rca.hoabs.admin.AuditLog;
import rw.ac.rca.hoabs.admin.AuditLogRepository;
import rw.ac.rca.hoabs.common.exception.ApiException;
import rw.ac.rca.hoabs.doctor.Doctor;
import rw.ac.rca.hoabs.identity.Role;
import rw.ac.rca.hoabs.identity.User;
import rw.ac.rca.hoabs.identity.UserRepository;
import rw.ac.rca.hoabs.notification.NotificationService;
import rw.ac.rca.hoabs.payment.PaymentService;
import rw.ac.rca.hoabs.slot.Slot;
import rw.ac.rca.hoabs.slot.SlotRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);
    private static final Duration TWO_HOURS = Duration.ofHours(2);

    private final AppointmentRepository appointmentRepository;
    private final SlotRepository slotRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final PaymentService paymentService;
    private final AuditLogRepository auditLogRepository;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              SlotRepository slotRepository,
                              UserRepository userRepository,
                              NotificationService notificationService,
                              PaymentService paymentService,
                              AuditLogRepository auditLogRepository) {
        this.appointmentRepository = appointmentRepository;
        this.slotRepository = slotRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.paymentService = paymentService;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AppointmentResponse bookAppointment(User caller, AppointmentRequest req, String stubModeOverride) {
        User patient;
        boolean isStaff = caller.getRole() == Role.RECEPTIONIST || caller.getRole() == Role.ADMIN;

        if (isStaff && req.patientId() != null) {
            patient = userRepository.findById(req.patientId())
                    .orElseThrow(() -> ApiException.notFound("Patient not found with id: " + req.patientId()));
        } else {
            patient = caller;
        }

        // Fetch slot and perform concurrency check
        Slot slot = slotRepository.findById(req.slotId())
                .orElseThrow(() -> ApiException.notFound("Slot not found with id: " + req.slotId()));

        Doctor doctor = slot.getDoctor();
        if (!doctor.isActive()) {
            throw ApiException.badRequest("Doctor is currently inactive.");
        }

        // FR-BOOK-06 / UAT AB-004: Lead time rule (< 2 hours blocked unless staff override)
        Instant now = Instant.now();
        Duration leadTime = Duration.between(now, slot.getStartTime());
        if (leadTime.compareTo(TWO_HOURS) < 0) {
            if (!isStaff) {
                throw ApiException.badRequest("Online bookings must be made at least 2 hours in advance. For urgent same-day bookings, please contact reception.");
            } else if (req.overrideReason() == null || req.overrideReason().isBlank()) {
                throw ApiException.badRequest("Reception override requires an explicit override reason.");
            }
        }

        // FR-BOOK-05 / UAT AB-003: Booking limit rule (at most 3 upcoming Confirmed appointments)
        long upcomingCount = appointmentRepository.countUpcomingConfirmedAppointments(patient.getId(), now);
        if (upcomingCount >= 3) {
            throw ApiException.conflict("Booking limit reached: you cannot hold more than 3 upcoming Confirmed appointments at a time.");
        }

        // FR-BOOK-02 / UAT AB-002: Slot concurrency check
        if (!slot.isOpen()) {
            throw ApiException.conflict("This slot was just booked — pick another.");
        }

        // FR-ADM-08 / UAT AD-007: Log override action if applicable
        if (isStaff && leadTime.compareTo(TWO_HOURS) < 0) {
            AuditLog audit = new AuditLog();
            audit.setActor(caller);
            audit.setAction("OVERRIDE_2H_BOOKING");
            audit.setReason(req.overrideReason());
            audit.setDetails(String.format("Overrode 2-hour lead time for patient %s with doctor %s",
                    patient.getPhone(), doctor.getStaffId()));
            auditLogRepository.save(audit);
        }

        // Mark slot booked
        slot.setIsOpen(false);
        slotRepository.save(slot);

        // FR-BOOK-07 / FR-BOOK-08: Process optional booking fee
        if (doctor.getConsultationFeeRwf() > 0) {
            paymentService.processBookingFee(null, doctor.getConsultationFeeRwf(), req.paymentProvider(), stubModeOverride);
        }

        String refNo = "APT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Appointment appt = new Appointment();
        appt.setReferenceNo(refNo);
        appt.setPatient(patient);
        appt.setDoctor(doctor);
        appt.setSlot(slot);
        appt.setReason(req.reason().trim());
        appt.setStatus(AppointmentStatus.CONFIRMED);
        if (isStaff && req.overrideReason() != null) {
            appt.setOverrideReason(req.overrideReason());
        }

        Appointment saved = appointmentRepository.save(appt);

        // FR-BOOK-04 / FR-NOTIF-01 / UAT AB-009: Send confirmation notice
        notificationService.sendConfirmation(
                patient,
                doctor.getUser().getFullName(),
                doctor.getDepartment().getName(),
                slot.getStartTime(),
                saved.getReferenceNo(),
                saved.getId()
        );

        log.info("Appointment {} booked for patient {} with doctor {}", saved.getReferenceNo(), patient.getId(), doctor.getId());
        return AppointmentResponse.from(saved);
    }

    @Transactional
    public AppointmentResponse cancelAppointment(User caller, UUID appointmentId, String cancellationReason, String overrideReason) {
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> ApiException.notFound("Appointment not found"));

        boolean isStaff = caller.getRole() == Role.RECEPTIONIST || caller.getRole() == Role.ADMIN;

        // FR-APPT-08 / SEC-03 / UAT AM-006: Patient A cannot access Patient B's appointment (return 404, not 403)
        if (!appt.getPatient().getId().equals(caller.getId()) && !isStaff) {
            throw ApiException.notFound("Appointment not found");
        }

        // Check if already completed, cancelled or no-show
        if (appt.getStatus() != AppointmentStatus.CONFIRMED) {
            throw ApiException.conflict("Cannot cancel appointment in status: " + appt.getStatus());
        }

        Instant now = Instant.now();
        Duration leadTime = Duration.between(now, appt.getSlot().getStartTime());

        // FR-APPT-05 / UAT AM-003: Cancellation within 2 hours blocked online
        if (leadTime.compareTo(TWO_HOURS) < 0) {
            if (!isStaff) {
                throw ApiException.badRequest("Online cancellation is blocked within 2 hours of appointment start time. Please call hospital reception.");
            } else if (overrideReason == null || overrideReason.isBlank()) {
                throw ApiException.badRequest("Reception override requires an explicit override reason.");
            }
        }

        if (isStaff && leadTime.compareTo(TWO_HOURS) < 0) {
            AuditLog audit = new AuditLog();
            audit.setActor(caller);
            audit.setAction("OVERRIDE_2H_CANCELLATION");
            audit.setReason(overrideReason);
            audit.setDetails("Overrode 2-hour lead time to cancel appointment " + appt.getReferenceNo());
            auditLogRepository.save(audit);
        }

        AppointmentStateMachine.assertTransition(appt.getStatus(), AppointmentStatus.CANCELLED);
        appt.setStatus(AppointmentStatus.CANCELLED);
        appt.setCancellationReason(cancellationReason != null ? cancellationReason : "Cancelled by user");

        // FR-APPT-02 / UAT AM-001: Free old slot immediately
        Slot slot = appt.getSlot();
        slot.setIsOpen(true);
        slotRepository.save(slot);

        Appointment updated = appointmentRepository.save(appt);

        // FR-APPT-06 / FR-NOTIF-03 / UAT AM-008: Send cancellation notice
        notificationService.sendCancellation(
                appt.getPatient(),
                appt.getDoctor().getUser().getFullName(),
                slot.getStartTime(),
                appt.getReferenceNo(),
                appt.getId()
        );

        return AppointmentResponse.from(updated);
    }

    @Transactional
    public AppointmentResponse rescheduleAppointment(User caller, UUID appointmentId, RescheduleRequest req) {
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> ApiException.notFound("Appointment not found"));

        boolean isStaff = caller.getRole() == Role.RECEPTIONIST || caller.getRole() == Role.ADMIN;

        // FR-APPT-08 / SEC-03 / UAT AM-006: IDOR Protection (404 on mismatch)
        if (!appt.getPatient().getId().equals(caller.getId()) && !isStaff) {
            throw ApiException.notFound("Appointment not found");
        }

        if (appt.getStatus() != AppointmentStatus.CONFIRMED) {
            throw ApiException.conflict("Cannot reschedule appointment in status: " + appt.getStatus());
        }

        Instant now = Instant.now();
        Duration currentLeadTime = Duration.between(now, appt.getSlot().getStartTime());

        // FR-APPT-05 / UAT AM-004: Reschedule blocked within 2 hours
        if (currentLeadTime.compareTo(TWO_HOURS) < 0) {
            if (!isStaff) {
                throw ApiException.badRequest("Online rescheduling is blocked within 2 hours of appointment start time. Please contact reception.");
            } else if (req.overrideReason() == null || req.overrideReason().isBlank()) {
                throw ApiException.badRequest("Reception override requires an explicit override reason.");
            }
        }

        Slot newSlot = slotRepository.findById(req.newSlotId())
                .orElseThrow(() -> ApiException.notFound("Target slot not found"));

        if (!newSlot.isOpen()) {
            throw ApiException.conflict("Target slot was just booked — please choose another slot.");
        }

        Duration newLeadTime = Duration.between(now, newSlot.getStartTime());
        if (newLeadTime.compareTo(TWO_HOURS) < 0 && !isStaff) {
            throw ApiException.badRequest("Online booking or rescheduling requires at least 2 hours advance notice.");
        }

        if (isStaff && currentLeadTime.compareTo(TWO_HOURS) < 0) {
            AuditLog audit = new AuditLog();
            audit.setActor(caller);
            audit.setAction("OVERRIDE_2H_RESCHEDULE");
            audit.setReason(req.overrideReason());
            audit.setDetails("Overrode 2-hour lead time to reschedule appointment " + appt.getReferenceNo());
            auditLogRepository.save(audit);
        }

        // FR-APPT-03 / FR-APPT-04 / UAT AM-002, AM-010:
        // Free old slot and reserve new slot in ONE atomic transaction; keeps single Confirmed appointment
        Slot oldSlot = appt.getSlot();
        oldSlot.setIsOpen(true);
        slotRepository.save(oldSlot);

        newSlot.setIsOpen(false);
        slotRepository.save(newSlot);

        appt.setSlot(newSlot);
        appt.setDoctor(newSlot.getDoctor());
        Appointment saved = appointmentRepository.save(appt);

        // FR-APPT-06 / FR-NOTIF-03: Send reschedule notice
        notificationService.sendReschedule(
                appt.getPatient(),
                saved.getDoctor().getUser().getFullName(),
                newSlot.getStartTime(),
                saved.getReferenceNo(),
                saved.getId()
        );

        return AppointmentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getPatientAppointments(UUID patientId) {
        return appointmentRepository.findByPatientIdOrderByCreatedAtDesc(patientId)
                .stream()
                .map(AppointmentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentById(User caller, UUID appointmentId) {
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> ApiException.notFound("Appointment not found"));

        boolean isStaff = caller.getRole() == Role.RECEPTIONIST || caller.getRole() == Role.ADMIN;
        boolean isDoctor = caller.getRole() == Role.DOCTOR && appt.getDoctor().getUser().getId().equals(caller.getId());

        // FR-APPT-08 / SEC-03: Patient A cannot view Patient B's appointment (returns 404)
        if (!appt.getPatient().getId().equals(caller.getId()) && !isStaff && !isDoctor) {
            throw ApiException.notFound("Appointment not found");
        }

        return AppointmentResponse.from(appt);
    }
}
