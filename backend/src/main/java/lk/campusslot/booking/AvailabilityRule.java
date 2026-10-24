package lk.campusslot.booking;

import org.springframework.stereotype.Component;
import lk.campusslot.facility.FacilitySlot;
import lk.campusslot.shared.ApiException;
import java.time.Instant;

@Component
public class AvailabilityRule implements BookingRule {
    @Override public void validate(FacilitySlot slot, Instant now) {
        if (!slot.getFacility().isActive() || slot.isBlocked()) throw ApiException.conflict("This facility or session is unavailable.");
    }
}
