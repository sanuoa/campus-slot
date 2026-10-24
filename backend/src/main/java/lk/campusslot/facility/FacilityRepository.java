package lk.campusslot.facility;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;

public interface FacilityRepository extends JpaRepository<Facility,Long> {
    List<Facility> findAllByOrderByNameAsc();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Facility f where f.id=:id")
    Optional<Facility> lockById(@Param("id") Long id);
}
