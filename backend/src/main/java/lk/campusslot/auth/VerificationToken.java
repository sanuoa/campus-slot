package lk.campusslot.auth;

import jakarta.persistence.*;
import lk.campusslot.shared.BaseEntity;
import java.time.Instant;

@Entity
public class VerificationToken extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") private CampusUser user;
    private String tokenHash;
    private Instant expiresAt;
    protected VerificationToken() {}
    public VerificationToken(CampusUser user, String tokenHash, Instant expiresAt) {
        this.user = user; this.tokenHash = tokenHash; this.expiresAt = expiresAt;
    }
    public CampusUser getUser() { return user; }
    public Instant getExpiresAt() { return expiresAt; }
}
