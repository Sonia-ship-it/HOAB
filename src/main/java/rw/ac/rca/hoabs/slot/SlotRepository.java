package rw.ac.rca.hoabs.slot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SlotRepository extends JpaRepository<Slot, UUID> {

    List<Slot> findByDoctorIdAndIsOpenTrueAndStartTimeBetweenOrderByStartTimeAsc(
            UUID doctorId, Instant startTime, Instant endTime);

    Optional<Slot> findFirstByDoctorIdAndIsOpenTrueAndStartTimeAfterOrderByStartTimeAsc(
            UUID doctorId, Instant after);

    @Query("SELECT s FROM Slot s WHERE s.doctor.id = :doctorId " +
           "AND s.startTime >= :from AND s.startTime <= :to " +
           "AND s.isOpen = true ORDER BY s.startTime ASC")
    List<Slot> findOpenSlotsInRange(@Param("doctorId") UUID doctorId,
                                    @Param("from") Instant from,
                                    @Param("to") Instant to);

    @Query("SELECT s FROM Slot s WHERE s.doctor.id = :doctorId AND s.startTime >= :from")
    List<Slot> findFutureSlotsForDoctor(@Param("doctorId") UUID doctorId, @Param("from") Instant from);

    Optional<Slot> findByDoctorIdAndStartTime(UUID doctorId, Instant startTime);

    @Modifying
    @Query("DELETE FROM Slot s WHERE s.doctor.id = :doctorId AND s.isOpen = true AND s.startTime > :after AND s.id NOT IN (SELECT a.slot.id FROM Appointment a)")
    void deleteOpenSlotsWithoutAppointments(@Param("doctorId") UUID doctorId, @Param("after") Instant after);
}
