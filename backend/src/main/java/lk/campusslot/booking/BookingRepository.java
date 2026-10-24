package lk.campusslot.booking;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.*;
import java.util.*;

public interface BookingRepository extends JpaRepository<Booking,Long> {
    boolean existsByUserIdAndBookingDateAndStatus(Long userId, LocalDate date, Booking.Status status);
    boolean existsBySlotIdAndStatus(Long slotId, Booking.Status status);
    @Query("select b.slot.id from Booking b where b.slot.facility.id=:id and b.bookingDate=:date and b.status=lk.campusslot.booking.Booking.Status.CONFIRMED")
    Set<Long> occupiedSlots(@Param("id") Long id, @Param("date") LocalDate date);
    @Query("select b from Booking b join fetch b.slot s join fetch s.facility join fetch b.user where b.user.email=:email order by s.startsAt desc")
    List<Booking> forUser(@Param("email") String email);
    @Query("select b from Booking b join fetch b.slot s join fetch s.facility join fetch b.user where b.bookingDate=:date order by s.startsAt")
    List<Booking> forDate(@Param("date") LocalDate date);
    @Query("select count(b)>0 from Booking b where b.slot.facility.id=:id and b.status=lk.campusslot.booking.Booking.Status.CONFIRMED and b.slot.endsAt>:now")
    boolean hasUpcomingBookings(@Param("id") Long facilityId, @Param("now") Instant now);
}
