package lk.campusslot.booking;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import java.time.*;
import java.util.*;
import lk.campusslot.auth.*;
import lk.campusslot.facility.*;
import lk.campusslot.shared.ApiException;
import static lk.campusslot.config.TimeConfig.CAMPUS_ZONE;
import jakarta.persistence.EntityManager;

@Service
public class BookingService {
    private final UserRepository users;
    private final FacilityRepository facilities;
    private final SlotRepository slots;
    private final BookingRepository bookings;
    private final List<BookingRule> rules;
    private final Clock clock;
    private final EntityManager entities;
    public BookingService(UserRepository users, FacilityRepository facilities, SlotRepository slots,
                          BookingRepository bookings, List<BookingRule> rules, Clock clock, EntityManager entities) {
        this.users=users; this.facilities=facilities; this.slots=slots; this.bookings=bookings; this.rules=rules; this.clock=clock;
        this.entities=entities;
    }
    public record BookingView(Long id, Long facilityId, String facilityName, Facility.Type facilityType,
                              Instant startsAt, Instant endsAt, LocalDate date, Booking.Status status,
                              String studentName, String studentEmail) {}
    public BookingView view(Booking b) {
        var s=b.getSlot(); var f=s.getFacility();
        return new BookingView(b.getId(),f.getId(),f.getName(),f.getType(),s.getStartsAt(),s.getEndsAt(),b.getBookingDate(),
                b.statusAt(clock.instant()),b.getUser().getName(),b.getUser().getEmail());
    }
    @Transactional
    public BookingView create(String email, Long slotId) {
        // Always lock in user -> facility -> slot order; different JVMs share the same PostgreSQL locks.
        var user=users.lockByEmail(email).orElseThrow();
        if (!user.isVerified() || user.getRole()!=CampusUser.Role.STUDENT) throw new ApiException(HttpStatus.FORBIDDEN,"Only verified students can book.");
        var candidate=slots.findById(slotId).orElseThrow(() -> ApiException.missing("Session not found."));
        facilities.lockById(candidate.getFacility().getId()).orElseThrow();
        var slot=slots.lockById(slotId).orElseThrow();
        entities.refresh(slot);
        Instant now=clock.instant();
        rules.forEach(rule -> rule.validate(slot,now));
        LocalDate day=slot.getStartsAt().atZone(CAMPUS_ZONE).toLocalDate();
        if (bookings.existsByUserIdAndBookingDateAndStatus(user.getId(),day,Booking.Status.CONFIRMED)) {
            throw ApiException.conflict("You already have your one-hour session for this date.");
        }
        if (bookings.existsBySlotIdAndStatus(slotId,Booking.Status.CONFIRMED)) throw ApiException.conflict("Someone has already booked this session.");
        return view(bookings.saveAndFlush(new Booking(user,slot,now)));
    }
    @Transactional(readOnly=true)
    public List<BookingView> mine(String email) { return bookings.forUser(email).stream().map(this::view).toList(); }
    @Transactional(readOnly=true)
    public List<BookingView> forDate(LocalDate date) { return bookings.forDate(date).stream().map(this::view).toList(); }
    @Transactional
    public BookingView cancel(String email, Long id, boolean admin) {
        var candidate=bookings.findById(id).orElseThrow(() -> ApiException.missing("Booking not found."));
        if (!admin && !candidate.getUser().getEmail().equals(email)) throw ApiException.missing("Booking not found.");
        users.lockByEmail(candidate.getUser().getEmail()).orElseThrow();
        facilities.lockById(candidate.getSlot().getFacility().getId()).orElseThrow();
        slots.lockById(candidate.getSlot().getId()).orElseThrow();
        // Reload after the lock, since another cancellation might have just committed.
        var booking=bookings.findById(id).orElseThrow();
        entities.refresh(booking);
        booking.cancel(clock.instant());
        bookings.flush();
        return view(booking);
    }
}
