package lk.campusslot.booking;

import lk.campusslot.facility.FacilitySlot;
import java.time.Instant;

/** Polymorphism: each rule validates a different part of the same booking request. */
public interface BookingRule {
    void validate(FacilitySlot slot, Instant now);
}
