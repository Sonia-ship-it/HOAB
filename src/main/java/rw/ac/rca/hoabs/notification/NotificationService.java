package rw.ac.rca.hoabs.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.rca.hoabs.identity.User;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.of("Africa/Kigali"));

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    private void recordNotification(UUID apptId, UUID userId, NotificationChannel channel, NotificationType type,
                                    String recipient, String message) {
        Notification notif = new Notification();
        notif.setAppointmentId(apptId);
        notif.setUserId(userId);
        notif.setChannel(channel);
        notif.setType(type);
        notif.setRecipient(recipient);
        notif.setMessage(message);
        notif.setStatus("SENT");
        notificationRepository.save(notif);
        log.info("Dispatched {} notice to {} via {}: {}", type, recipient, channel, message);
    }

    @Transactional
    public void sendWelcome(User patient) {
        String msg = String.format("Welcome to Amahoro General Hospital Online Booking, %s! Your account is active.", patient.getFullName());
        recordNotification(null, patient.getId(), NotificationChannel.SMS, NotificationType.CONFIRMATION, patient.getPhone(), msg);
    }

    @Transactional
    public void sendConfirmation(User patient, String doctorName, String department, Instant slotTime, String refNo, UUID apptId) {
        String formatted = FORMATTER.format(slotTime);
        String msg = String.format("Amahoro Hospital: Appointment %s confirmed with Dr. %s (%s) on %s CAT. Ref: %s",
                refNo, doctorName, department, formatted, refNo);
        recordNotification(apptId, patient.getId(), NotificationChannel.SMS, NotificationType.CONFIRMATION, patient.getPhone(), msg);
        if (patient.getEmail() != null && !patient.getEmail().isBlank()) {
            recordNotification(apptId, patient.getId(), NotificationChannel.EMAIL, NotificationType.CONFIRMATION, patient.getEmail(), msg);
        }
    }

    @Transactional
    public void sendReminder(User patient, String doctorName, String department, Instant slotTime, String refNo, UUID apptId) {
        String formatted = FORMATTER.format(slotTime);
        String msg = String.format("Amahoro Hospital Reminder: You have an appointment tomorrow at %s with Dr. %s (%s). Ref: %s",
                formatted, doctorName, department, refNo);
        recordNotification(apptId, patient.getId(), NotificationChannel.SMS, NotificationType.REMINDER, patient.getPhone(), msg);
    }

    @Transactional
    public void sendCancellation(User patient, String doctorName, Instant slotTime, String refNo, UUID apptId) {
        String formatted = FORMATTER.format(slotTime);
        String msg = String.format("Amahoro Hospital: Appointment %s with Dr. %s scheduled for %s has been cancelled.",
                refNo, doctorName, formatted);
        recordNotification(apptId, patient.getId(), NotificationChannel.SMS, NotificationType.CANCELLATION, patient.getPhone(), msg);
    }

    @Transactional
    public void sendReschedule(User patient, String doctorName, Instant newSlotTime, String refNo, UUID apptId) {
        String formatted = FORMATTER.format(newSlotTime);
        String msg = String.format("Amahoro Hospital: Appointment %s has been rescheduled to %s with Dr. %s.",
                refNo, formatted, doctorName);
        recordNotification(apptId, patient.getId(), NotificationChannel.SMS, NotificationType.RESCHEDULE, patient.getPhone(), msg);
    }

    @Transactional
    public void sendSecurityAlert(User patient, String reason) {
        String msg = String.format("Amahoro Hospital Security Alert: %s. Please contact reception if this was not you.", reason);
        recordNotification(null, patient.getId(), NotificationChannel.SMS, NotificationType.SECURITY_ALERT, patient.getPhone(), msg);
    }
}
