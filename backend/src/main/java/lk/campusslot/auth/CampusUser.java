package lk.campusslot.auth;

import jakarta.persistence.*;
import lk.campusslot.shared.BaseEntity;

@Entity
public class CampusUser extends BaseEntity {
    public enum Role { STUDENT, ADMIN }
    private String name;
    private String email;
    private String passwordHash;
    @Enumerated(EnumType.STRING) private Role role;
    private boolean verified;

    protected CampusUser() {}
    public CampusUser(String name, String email, String passwordHash) {
        this.name = name.trim(); this.email = CampusEmail.normalize(email);
        this.passwordHash = passwordHash; this.role = Role.STUDENT;
    }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public boolean isVerified() { return verified; }
    public void verify() { verified = true; }
    public void grantAdmin() { role = Role.ADMIN; verified = true; }
}
