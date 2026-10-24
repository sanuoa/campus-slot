package lk.campusslot.booking;

import jakarta.persistence.*;
import java.time.*;
import lk.campusslot.shared.*;
import lk.campusslot.auth.CampusUser;
import lk.campusslot.facility.FacilitySlot;
import static lk.campusslot.config.TimeConfig.CAMPUS_ZONE;

@Entity
public class Booking extends BaseEntity {
    public enum Status { CONFIRMED, CANCELLED, COMPLETED }
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id") private CampusUser user;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="slot_id") private FacilitySlot slot;
    private LocalDate bookingDate;
    @Enumerated(EnumType.STRING) private Status status = Status.CONFIRMED;
    private Instant createdAt;
    protected Booking() {}
    public Booking(CampusUser user, FacilitySlot slot, Instant now) {
        this.user=user; this.slot=slot; this.bookingDate=slot.getStartsAt().atZone(CAMPUS_ZONE).toLocalDate(); this.createdAt=now;
    }
    public CampusUser getUser() { return user; }
    public FacilitySlot getSlot() { return slot; }
    public LocalDate getBookingDate() { return bookingDate; }
    public Instant getCreatedAt() { return createdAt; }
    /** Deriving completion works even when the server was offline at the end of the hour. */
    public Status statusAt(Instant now) {
        if (status == Status.CANCELLED) return status;
        return !now.isBefore(slot.getEndsAt()) ? Status.COMPLETED : Status.CONFIRMED;
    }
    public void cancel(Instant now) {
        if (status == Status.CANCELLED) return;
        if (!now.isBefore(slot.getStartsAt())) throw ApiException.conflict("A session can only be cancelled before it starts.");
        status=Status.CANCELLED;
    }
}
