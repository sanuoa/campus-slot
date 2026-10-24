package lk.campusslot;

import org.junit.jupiter.api.Test;
import java.time.*;
import lk.campusslot.auth.*;
import lk.campusslot.facility.*;
import lk.campusslot.booking.*;
import lk.campusslot.shared.ApiException;
import static org.assertj.core.api.Assertions.*;

class DomainTest {
    private final Instant start=Instant.parse("2026-10-25T03:30:00Z");
    private FacilitySlot slot() { return new FacilitySlot(new Facility("Room",Facility.Type.STUDY_ROOM,"Level 1",6),start); }
    private Booking booking() { return new Booking(new CampusUser("Student","student@iit.ac.lk","hash"),slot(),start.minusSeconds(3600)); }
    @Test void onlyTheExactEmailDomainIsAllowed() {
        assertThat(CampusEmail.normalize("  Student@IIT.AC.LK ")).isEqualTo("student@iit.ac.lk");
        for (String email:new String[]{"s@gmail.com","s@iit.ac.lk.evil.com","s@fakeiit.ac.lk","s@student.iit.ac.lk","s@@iit.ac.lk"}) {
            assertThatThrownBy(() -> CampusEmail.normalize(email)).isInstanceOf(ApiException.class);
        }
    }
    @Test void oneHourRunsFromScheduledStart() {
        assertThat(slot().getEndsAt()).isEqualTo(start.plusSeconds(3600));
        assertThat(booking().statusAt(start.plusSeconds(3599))).isEqualTo(Booking.Status.CONFIRMED);
        assertThat(booking().statusAt(start.plusSeconds(3600))).isEqualTo(Booking.Status.COMPLETED);
    }
    @Test void completionDoesNotNeedASchedulerToHaveBeenRunning() {
        assertThat(booking().statusAt(start.plus(Duration.ofDays(10)))).isEqualTo(Booking.Status.COMPLETED);
    }
    @Test void bookingDateUsesSriLankaMidnight() {
        var lateSlot=new FacilitySlot(slot().getFacility(),Instant.parse("2026-10-24T20:30:00Z"));
        var b=new Booking(new CampusUser("Student","s@iit.ac.lk","hash"),lateSlot,start.minusSeconds(3600));
        assertThat(b.getBookingDate()).isEqualTo(LocalDate.of(2026,10,25));
    }
    @Test void cancellationIsOnlyPossibleBeforeStart() {
        assertThatThrownBy(() -> booking().cancel(start)).isInstanceOf(ApiException.class);
        var b=booking(); b.cancel(start.minusSeconds(1));
        assertThat(b.statusAt(start.plusSeconds(7200))).isEqualTo(Booking.Status.CANCELLED);
    }
    @Test void aStartedSlotCannotBeBooked() {
        assertThatThrownBy(() -> new FutureSlotRule().validate(slot(),start)).isInstanceOf(ApiException.class);
    }
    @Test void aBlockedSlotCannotBeBooked() {
        var s=slot(); s.setBlocked(true);
        assertThatThrownBy(() -> new AvailabilityRule().validate(s,start.minusSeconds(1))).isInstanceOf(ApiException.class);
    }
}
