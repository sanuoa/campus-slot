package lk.campusslot.booking;

import org.springframework.stereotype.Component;
import lk.campusslot.facility.FacilitySlot;
import lk.campusslot.shared.ApiException;
import java.time.*;
import static lk.campusslot.config.TimeConfig.CAMPUS_ZONE;

@Component
public class FutureSlotRule implements BookingRule {
    @Override public void validate(FacilitySlot slot, Instant now) {
        if (!slot.getStartsAt().isAfter(now)) throw ApiException.conflict("This session has already started.");
        if (slot.getStartsAt().atZone(CAMPUS_ZONE).toLocalDate().isAfter(now.atZone(CAMPUS_ZONE).toLocalDate().plusDays(30))) {
            throw ApiException.invalid("Book within the next 30 days.");
        }
    }
}
