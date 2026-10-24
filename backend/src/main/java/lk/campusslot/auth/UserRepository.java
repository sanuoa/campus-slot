package lk.campusslot.auth;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface UserRepository extends JpaRepository<CampusUser, Long> {
    Optional<CampusUser> findByEmail(String email);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from CampusUser u where u.email = :email")
    Optional<CampusUser> lockByEmail(@Param("email") String email);
}
