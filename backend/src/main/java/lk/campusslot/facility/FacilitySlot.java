package lk.campusslot.facility;

import jakarta.persistence.*;
import lk.campusslot.shared.BaseEntity;
import java.time.*;

@Entity
public class FacilitySlot extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="facility_id") private Facility facility;
    private Instant startsAt;
    private Instant endsAt;
    private boolean blocked;
    protected FacilitySlot() {}
    public FacilitySlot(Facility facility, Instant startsAt) {
        this.facility=facility; this.startsAt=startsAt; this.endsAt=startsAt.plus(Duration.ofHours(1));
    }
    public Facility getFacility() { return facility; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean value) { blocked=value; }
}
