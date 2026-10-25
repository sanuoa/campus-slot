package lk.campusslot.config;

import lk.campusslot.auth.*;
import lk.campusslot.facility.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.Arrays;

@Component
public class Bootstrap implements CommandLineRunner {
    private final Environment env;
    private final UserRepository users;
    private final FacilityRepository facilities;
    private final SlotRepository slots;
    private final PasswordEncoder passwords;
    private final Clock clock;
    public Bootstrap(Environment env,UserRepository users,FacilityRepository facilities,SlotRepository slots,PasswordEncoder passwords,Clock clock) {
        this.env=env; this.users=users; this.facilities=facilities; this.slots=slots; this.passwords=passwords; this.clock=clock;
    }
    @Override @Transactional public void run(String... args) {
        boolean local=Arrays.asList(env.getActiveProfiles()).contains("local");
        boolean prod=Arrays.asList(env.getActiveProfiles()).contains("prod");
        if (prod && (!env.getProperty("app.mail.delivery", "smtp").equalsIgnoreCase("smtp") || local || !env.getProperty("COOKIE_SECURE","false").equals("true") || !env.getProperty("app.frontend-url","").startsWith("https://") || env.getProperty("SMTP_HOST","").isBlank())) {
            throw new IllegalStateException("Production needs HTTPS, COOKIE_SECURE=true, SMTP_HOST, SMTP delivery, and no local profile.");
        }
        if (!local && env.getProperty("app.mail.delivery", "smtp").equalsIgnoreCase("console")) {
            throw new IllegalStateException("Console verification is only allowed with the local profile.");
        }
        String adminEmail=env.getProperty("APP_ADMIN_EMAIL","");
        String adminPassword=env.getProperty("APP_ADMIN_PASSWORD","");
        if (!adminEmail.isBlank() && !adminPassword.isBlank()) {
            String normalized=CampusEmail.normalize(adminEmail);
            var existing=users.findByEmail(normalized);
            if (existing.isPresent() && existing.get().getRole()!=CampusUser.Role.ADMIN) throw new IllegalStateException("The bootstrap administrator email is already a student account.");
            if (existing.isEmpty()) {
                if (adminPassword.length()<12 || adminPassword.getBytes(StandardCharsets.UTF_8).length>72) throw new IllegalStateException("Use an admin password of at least 12 characters and at most 72 UTF-8 bytes.");
                var admin=new CampusUser("Campus administrator",normalized,passwords.encode(adminPassword));
                admin.grantAdmin(); users.save(admin);
            }
        }
        // These are fictional examples, never a claim about IIT's real facilities or timetable.
        if (local && facilities.count()==0) {
            var examples=facilities.saveAll(java.util.List.of(
                new Facility("Sample Study Room",Facility.Type.STUDY_ROOM,"Demo building · Level 1",6),
                new Facility("Sample Computer Lab",Facility.Type.COMPUTER_LAB,"Demo building · Level 2",24),
                new Facility("Sample Sports Court",Facility.Type.SPORTS_COURT,"Demo sports area",10)));
            LocalDate today=LocalDate.now(clock.withZone(TimeConfig.CAMPUS_ZONE));
            for (var facility:examples) for (int day=0;day<7;day++) for (int hour=8;hour<18;hour++) {
                Instant start=today.plusDays(day).atTime(hour,0).atZone(TimeConfig.CAMPUS_ZONE).toInstant();
                if (start.isAfter(clock.instant())) slots.save(new FacilitySlot(facility,start));
            }
        }
    }
}
