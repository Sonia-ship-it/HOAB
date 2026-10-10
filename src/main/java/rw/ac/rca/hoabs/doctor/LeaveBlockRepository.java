package rw.ac.rca.hoabs.doctor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface LeaveBlockRepository extends JpaRepository<LeaveBlock, UUID> {

    List<LeaveBlock> findByDoctorId(UUID doctorId);

    @Query("SELECT lb FROM LeaveBlock lb WHERE lb.doctor.id = :doctorId " +
           "AND lb.startDate <= :targetDate AND lb.endDate >= :targetDate")
    List<LeaveBlock> findLeaveOnDate(@Param("doctorId") UUID doctorId, @Param("targetDate") LocalDate targetDate);
}
