package lk.campusslot.auth;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import lk.campusslot.shared.ApiException;

@Service
public class AuthService {
    private final UserRepository users;
    private final TokenRepository tokens;
    private final PasswordEncoder passwords;
    private final VerificationSender sender;
    private final Clock clock;
    private final String frontendUrl;
    public AuthService(UserRepository users, TokenRepository tokens, PasswordEncoder passwords,
                       VerificationSender sender, Clock clock, @Value("${app.frontend-url}") String frontendUrl) {
        this.users = users; this.tokens = tokens; this.passwords = passwords;
        this.sender = sender; this.clock = clock; this.frontendUrl = frontendUrl;
    }
    @Transactional
    public void register(String name, String email, String password) {
        email = CampusEmail.normalize(email);
        // BCrypt works on bytes, not Java characters.
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) throw ApiException.invalid("Password must fit within 72 UTF-8 bytes.");
        if (users.findByEmail(email).isPresent()) throw ApiException.conflict("An account already exists for this email.");
        var user = users.saveAndFlush(new CampusUser(name, email, passwords.encode(password)));
        sendVerification(user);
    }
    @Transactional
    public void resend(String email, String password) {
        // Require the account password, serialize resends, and avoid disclosing account status.
        var user = users.lockByEmail(CampusEmail.normalize(email)).orElse(null);
        if (user == null || !passwords.matches(password, user.getPasswordHash()) || user.isVerified()) return;
        var latest = tokens.findFirstByUserIdOrderByExpiresAtDesc(user.getId());
        if (latest.isPresent() && latest.get().getExpiresAt().minus(Duration.ofHours(24)).plusSeconds(60).isAfter(clock.instant())) return;
        tokens.deleteByUserId(user.getId());
        tokens.flush();
        sendVerification(user);
    }
    private void sendVerification(CampusUser user) {
        var random = new byte[32]; new SecureRandom().nextBytes(random);
        String token = HexFormat.of().formatHex(random);
        tokens.save(new VerificationToken(user, hash(token), clock.instant().plus(Duration.ofHours(24))));
        sender.send(user.getEmail(), frontendUrl + "/?verify=" + token);
    }
    @Transactional
    public void verify(String token) {
        if (token == null || !token.matches("[a-f0-9]{64}")) throw ApiException.invalid("Invalid verification link.");
        var stored = tokens.findByTokenHash(hash(token)).orElseThrow(() -> ApiException.invalid("Invalid or already used verification link."));
        if (!stored.getExpiresAt().isAfter(clock.instant())) throw ApiException.invalid("Verification link has expired.");
        stored.getUser().verify(); tokens.delete(stored);
    }
    static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}
