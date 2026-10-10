package rw.ac.rca.hoabs.identity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByPhone(String phone);

    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.phone = :identifier OR (u.email IS NOT NULL AND LOWER(u.email) = LOWER(:identifier))")
    Optional<User> findByPhoneOrEmail(@Param("identifier") String identifier);

    boolean existsByPhone(String phone);

    boolean existsByEmail(String email);
}
