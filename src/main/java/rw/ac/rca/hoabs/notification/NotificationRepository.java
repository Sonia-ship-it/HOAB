package rw.ac.rca.hoabs.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByUserIdOrderBySentAtDesc(UUID userId);

    List<Notification> findByAppointmentId(UUID appointmentId);

    List<Notification> findByRecipientOrderBySentAtDesc(String recipient);
}
