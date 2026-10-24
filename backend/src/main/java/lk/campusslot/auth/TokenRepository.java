package lk.campusslot.auth;

import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface TokenRepository extends JpaRepository<VerificationToken, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<VerificationToken> findByTokenHash(String hash);
}
