package rw.ac.rca.hoabs.doctor;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkingHoursRepository extends JpaRepository<WorkingHours, UUID> {
    List<WorkingHours> findByDoctorId(UUID doctorId);
    void deleteByDoctorId(UUID doctorId);
}
