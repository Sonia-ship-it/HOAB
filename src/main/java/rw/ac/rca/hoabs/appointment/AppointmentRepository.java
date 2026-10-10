package rw.ac.rca.hoabs.appointment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    Optional<Appointment> findByReferenceNo(String referenceNo);

    List<Appointment> findByPatientIdOrderByCreatedAtDesc(UUID patientId);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.patient.id = :patientId " +
           "AND a.status = 'CONFIRMED' AND a.slot.startTime >= :now")
    long countUpcomingConfirmedAppointments(@Param("patientId") UUID patientId, @Param("now") Instant now);

    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId " +
           "AND a.status = 'CONFIRMED' AND a.slot.startTime >= :start AND a.slot.startTime <= :end " +
           "ORDER BY a.slot.startTime ASC")
    List<Appointment> findConfirmedAppointmentsForDoctorOnDate(
            @Param("doctorId") UUID doctorId,
            @Param("start") Instant start,
            @Param("end") Instant end);

    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId " +
           "AND a.status = 'CONFIRMED' AND a.slot.startTime >= :start AND a.slot.startTime <= :end")
    List<Appointment> findConflictsInRange(
            @Param("doctorId") UUID doctorId,
            @Param("start") Instant start,
            @Param("end") Instant end);

    @Query("SELECT a FROM Appointment a WHERE a.slot.startTime >= :from AND a.slot.startTime <= :to")
    List<Appointment> findAllInDateRange(@Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT a FROM Appointment a WHERE a.status = 'CONFIRMED' " +
           "AND a.slot.startTime >= :reminderFrom AND a.slot.startTime <= :reminderTo")
    List<Appointment> findAppointmentsDueFor24hReminder(
            @Param("reminderFrom") Instant reminderFrom,
            @Param("reminderTo") Instant reminderTo);
}
