package rw.ac.rca.hoabs.doctor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoctorRepository extends JpaRepository<Doctor, UUID> {

    Optional<Doctor> findByStaffId(String staffId);

    Optional<Doctor> findByUserId(UUID userId);

    boolean existsByStaffId(String staffId);

    List<Doctor> findByActiveTrue();

    @Query("SELECT d FROM Doctor d WHERE d.active = true " +
           "AND (:deptId IS NULL OR d.department.id = :deptId) " +
           "AND (:query IS NULL OR :query = '' OR " +
           "LOWER(d.user.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(d.specialty) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(d.department.name) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Doctor> searchActiveDoctors(@Param("deptId") UUID deptId, @Param("query") String query);
}
