package lk.campusslot.facility;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lk.campusslot.booking.*;
import lk.campusslot.shared.ApiException;
import java.time.*;
import java.util.*;
import static lk.campusslot.config.TimeConfig.CAMPUS_ZONE;
import jakarta.persistence.EntityManager;

@Service
public class FacilityService {
    private final FacilityRepository facilities;
    private final SlotRepository slots;
    private final BookingRepository bookings;
    private final Clock clock;
    private final EntityManager entities;
    public FacilityService(FacilityRepository facilities,SlotRepository slots,BookingRepository bookings,Clock clock,EntityManager entities) {
        this.facilities=facilities; this.slots=slots; this.bookings=bookings; this.clock=clock;
        this.entities=entities;
    }
    public record FacilityView(Long id,String name,Facility.Type type,String location,int capacity,boolean active) {}
    public record SlotView(Long id,Instant startsAt,Instant endsAt,String state) {}
    public FacilityView view(Facility f) { return new FacilityView(f.getId(),f.getName(),f.getType(),f.getLocation(),f.getCapacity(),f.isActive()); }
    @Transactional(readOnly=true)
    public List<FacilityView> list(boolean admin) { return facilities.findAllByOrderByNameAsc().stream().filter(f -> admin || f.isActive()).map(this::view).toList(); }
    @Transactional(readOnly=true)
    public List<SlotView> slots(Long id,LocalDate date) {
        var facility=facilities.findById(id).orElseThrow(() -> ApiException.missing("Facility not found."));
        Set<Long> taken=bookings.occupiedSlots(id,date);
        return slots.onDate(id,date.atStartOfDay(CAMPUS_ZONE).toInstant(),date.plusDays(1).atStartOfDay(CAMPUS_ZONE).toInstant())
            .stream().map(s -> new SlotView(s.getId(),s.getStartsAt(),s.getEndsAt(),
                !s.getStartsAt().isAfter(clock.instant()) ? "PAST" : taken.contains(s.getId()) ? "BOOKED" : s.isBlocked() || !facility.isActive() ? "BLOCKED" : "AVAILABLE")).toList();
    }
    @Transactional
    public FacilityView create(String name,Facility.Type type,String location,int capacity) { return view(facilities.save(new Facility(name,type,location,capacity))); }
    @Transactional
    public FacilityView update(Long id,String name,Facility.Type type,String location,int capacity,boolean active) {
        var f=facilities.lockById(id).orElseThrow(() -> ApiException.missing("Facility not found."));
        if (!active && bookings.hasUpcomingBookings(id,clock.instant())) throw ApiException.conflict("Cancel upcoming bookings before deactivating this facility.");
        f.update(name,type,location,capacity,active); return view(f);
    }
    @Transactional
    public int generate(Long id,LocalDate date,int openingHour,int closingHour) {
        var f=facilities.lockById(id).orElseThrow(() -> ApiException.missing("Facility not found."));
        LocalDate today=LocalDate.now(clock.withZone(CAMPUS_ZONE));
        if (date.isBefore(today) || date.isAfter(today.plusDays(30)) || openingHour>=closingHour) throw ApiException.invalid("Use a date within 30 days and a closing hour after the opening hour.");
        int count=0;
        for (int hour=openingHour; hour<closingHour; hour++) {
            Instant start=date.atTime(hour,0).atZone(CAMPUS_ZONE).toInstant();
            if (start.isAfter(clock.instant()) && !slots.existsByFacilityIdAndStartsAt(id,start)) { slots.save(new FacilitySlot(f,start)); count++; }
        }
        return count;
    }
    @Transactional
    public void block(Long id,boolean blocked) {
        var candidate=slots.findById(id).orElseThrow(() -> ApiException.missing("Session not found."));
        facilities.lockById(candidate.getFacility().getId()).orElseThrow();
        var slot=slots.lockById(id).orElseThrow();
        entities.refresh(slot);
        if (blocked && bookings.existsBySlotIdAndStatus(id,Booking.Status.CONFIRMED)) throw ApiException.conflict("Cancel the booking before blocking this session.");
        slot.setBlocked(blocked);
    }
}
