package lk.campusslot.facility;

import jakarta.persistence.*;
import lk.campusslot.shared.BaseEntity;

@Entity
public class Facility extends BaseEntity {
    public enum Type { STUDY_ROOM, COMPUTER_LAB, SPORTS_COURT }
    private String name;
    @Enumerated(EnumType.STRING) private Type type;
    private String location;
    private int capacity;
    private boolean active = true;
    protected Facility() {}
    public Facility(String name, Type type, String location, int capacity) { update(name,type,location,capacity,true); }
    public void update(String name, Type type, String location, int capacity, boolean active) {
        this.name=name.trim(); this.type=type; this.location=location.trim(); this.capacity=capacity; this.active=active;
    }
    public String getName() { return name; }
    public Type getType() { return type; }
    public String getLocation() { return location; }
    public int getCapacity() { return capacity; }
    public boolean isActive() { return active; }
}
