package lk.campusslot.facility;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;

public interface SlotRepository extends JpaRepository<FacilitySlot,Long> {
    @Query("select s from FacilitySlot s join fetch s.facility where s.facility.id=:id and s.startsAt>=:from and s.startsAt<:until order by s.startsAt")
    List<FacilitySlot> onDate(@Param("id") Long id, @Param("from") Instant from, @Param("until") Instant until);
    boolean existsByFacilityIdAndStartsAt(Long id, Instant startsAt);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from FacilitySlot s where s.id=:id")
    Optional<FacilitySlot> lockById(@Param("id") Long id);
}
