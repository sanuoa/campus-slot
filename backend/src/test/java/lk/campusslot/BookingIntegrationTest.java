package lk.campusslot;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.time.*;
import java.util.concurrent.*;
import java.util.*;
import lk.campusslot.auth.*;
import lk.campusslot.booking.*;
import lk.campusslot.facility.*;
import lk.campusslot.shared.ApiException;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Uses real PostgreSQL. CI enables this suite against a disposable service database. */
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("local")
@EnabledIfEnvironmentVariable(named="RUN_DB_TESTS",matches="true")
class BookingIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired FacilityRepository facilities;
    @Autowired SlotRepository slots;
    @Autowired BookingRepository bookings;
    @Autowired BookingService service;
    @Autowired PasswordEncoder passwords;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;
    @Autowired CapturingSender sender;
    @Autowired AuthService auth;
    private static final Instant NOW=Instant.parse("2026-10-24T01:30:00Z");
    private static final String PASSWORD="A test password 123!";
    @TestConfiguration static class Config {
        @Bean @Primary Clock testClock() { return Clock.fixed(NOW,ZoneOffset.UTC); }
        @Bean @Primary CapturingSender sender() { return new CapturingSender(); }
    }
    static class CapturingSender implements VerificationSender {
        String url;
        boolean fail;
        @Override public void send(String email,String verificationUrl) {
            if (fail) throw new ApiException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"Email delivery is unavailable.");
            url=verificationUrl;
        }
    }
    @BeforeEach void clean() { jdbc.execute("TRUNCATE booking, verification_token, facility_slot, facility, campus_user RESTART IDENTITY CASCADE"); sender.url=null; sender.fail=false; }
    CampusUser student(String email) { var u=new CampusUser("Student",email,passwords.encode(PASSWORD)); u.verify(); return users.saveAndFlush(u); }
    FacilitySlot slot(Facility.Type type,Instant at) {
        var f=facilities.saveAndFlush(new Facility("Test "+type,type,"Test building",6));
        return slots.saveAndFlush(new FacilitySlot(f,at));
    }
    @Test void registrationRequiresVerificationAndTokensAreSingleUse() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json").content("{\"name\":\"Student\",\"email\":\"student@iit.ac.lk\",\"password\":\""+PASSWORD+"\"}")).andExpect(status().isCreated());
        mvc.perform(post("/api/auth/login").with(csrf()).param("username","student@iit.ac.lk").param("password",PASSWORD)).andExpect(status().isUnauthorized());
        String token=sender.url.substring(sender.url.indexOf("?verify=")+8);
        assertThat(jdbc.queryForObject("select token_hash from verification_token",String.class)).isNotEqualTo(token);
        mvc.perform(post("/api/auth/verify").with(csrf()).contentType("application/json").content("{\"token\":\""+token+"\"}")).andExpect(status().isNoContent());
        mvc.perform(post("/api/auth/verify").with(csrf()).contentType("application/json").content("{\"token\":\""+token+"\"}")).andExpect(status().isBadRequest());
        var login=mvc.perform(post("/api/auth/login").with(csrf()).param("username","student@iit.ac.lk").param("password",PASSWORD)).andExpect(status().isNoContent()).andReturn();
        mvc.perform(get("/api/auth/me").session((org.springframework.mock.web.MockHttpSession)login.getRequest().getSession(false)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("STUDENT")).andExpect(jsonPath("$.passwordHash").doesNotExist());
    }
    @Test void deliveryConfigurationIsPublicAndResendingStillRequiresCsrf() throws Exception {
        mvc.perform(get("/api/auth/verification-config")).andExpect(status().isOk()).andExpect(jsonPath("$.delivery").value("console"));
        mvc.perform(post("/api/auth/resend-verification").contentType("application/json")
            .content("{\"email\":\"student@iit.ac.lk\",\"password\":\""+PASSWORD+"\"}"))
            .andExpect(status().isForbidden());
    }
    @Test void resendingRequiresCredentialsHonoursCooldownAndReplacesOldToken() throws Exception {
        auth.register("Student","student@iit.ac.lk",PASSWORD);
        String oldToken=sender.url.substring(sender.url.indexOf("?verify=")+8);
        sender.url=null;
        auth.resend("student@iit.ac.lk",PASSWORD);
        assertThat(sender.url).isNull();
        jdbc.update("update verification_token set expires_at=expires_at-interval '2 minutes'");
        auth.resend("student@iit.ac.lk","wrong password");
        auth.resend("unknown@iit.ac.lk",PASSWORD);
        assertThat(sender.url).isNull();
        mvc.perform(post("/api/auth/resend-verification").with(csrf()).contentType("application/json")
            .content("{\"email\":\"student@iit.ac.lk\",\"password\":\""+PASSWORD+"\"}"))
            .andExpect(status().isAccepted());
        String newToken=sender.url.substring(sender.url.indexOf("?verify=")+8);
        assertThat(newToken).isNotEqualTo(oldToken);
        assertThat(jdbc.queryForObject("select count(*) from verification_token",Integer.class)).isEqualTo(1);
        assertThatThrownBy(() -> auth.verify(oldToken)).isInstanceOf(ApiException.class);
        auth.verify(newToken);
        sender.url=null;
        auth.resend("student@iit.ac.lk",PASSWORD);
        assertThat(sender.url).isNull();
        assertThat(jdbc.queryForObject("select count(*) from verification_token",Integer.class)).isZero();
    }
    @Test void failedEmailDeliveryRollsBackRegistrationAndPreservesPreviousResendToken() {
        sender.fail=true;
        assertThatThrownBy(() -> auth.register("Student","student@iit.ac.lk",PASSWORD)).isInstanceOf(ApiException.class);
        assertThat(users.findByEmail("student@iit.ac.lk")).isEmpty();
        sender.fail=false;
        auth.register("Student","student@iit.ac.lk",PASSWORD);
        String oldToken=sender.url.substring(sender.url.indexOf("?verify=")+8);
        jdbc.update("update verification_token set expires_at=expires_at-interval '2 minutes'");
        sender.fail=true;
        assertThatThrownBy(() -> auth.resend("student@iit.ac.lk",PASSWORD)).isInstanceOf(ApiException.class);
        auth.verify(oldToken);
        assertThat(users.findByEmail("student@iit.ac.lk").orElseThrow().isVerified()).isTrue();
    }
    @Test void outsidersAndForgedRolesCannotRegisterAsAdministrators() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json").content("{\"name\":\"Student\",\"email\":\"s@iit.ac.lk.evil.com\",\"password\":\""+PASSWORD+"\"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json").content("{\"name\":\"Student\",\"email\":\"s@iit.ac.lk\",\"password\":\""+PASSWORD+"\",\"role\":\"ADMIN\"}")).andExpect(status().isCreated());
        assertThat(users.findByEmail("s@iit.ac.lk").orElseThrow().getRole()).isEqualTo(CampusUser.Role.STUDENT);
    }
    @Test void permissionsAndCsrfAreEnforced() throws Exception {
        mvc.perform(get("/api/facilities")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/bookings").with(user("s@iit.ac.lk").roles("STUDENT")).contentType("application/json").content("{\"slotId\":1}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/bookings?date=2026-10-24").with(user("s@iit.ac.lk").roles("STUDENT"))).andExpect(status().isForbidden());
    }
    @Test void sameStudentCannotBookTwoDifferentFacilityTypesInOneDay() {
        student("s@iit.ac.lk");
        var first=slot(Facility.Type.STUDY_ROOM,NOW.plusSeconds(3600));
        var second=slot(Facility.Type.SPORTS_COURT,NOW.plusSeconds(7200));
        service.create("s@iit.ac.lk",first.getId());
        assertThatThrownBy(() -> service.create("s@iit.ac.lk",second.getId())).isInstanceOf(ApiException.class).hasMessageContaining("one-hour");
    }
    @Test void completedSessionStillConsumesDailyQuotaAndAnotherDayWorks() {
        var u=student("s@iit.ac.lk");
        var earlier=slot(Facility.Type.COMPUTER_LAB,NOW.minusSeconds(7200));
        new TransactionTemplate(transactions).executeWithoutResult(s -> bookings.saveAndFlush(new Booking(u,earlier,NOW.minusSeconds(10000))));
        assertThat(service.mine("s@iit.ac.lk").get(0).status()).isEqualTo(Booking.Status.COMPLETED);
        var later=slot(Facility.Type.STUDY_ROOM,NOW.plusSeconds(7200));
        assertThatThrownBy(() -> service.create("s@iit.ac.lk",later.getId())).isInstanceOf(ApiException.class);
        var tomorrow=slot(Facility.Type.SPORTS_COURT,NOW.plusSeconds(86400));
        assertThat(service.create("s@iit.ac.lk",tomorrow.getId()).status()).isEqualTo(Booking.Status.CONFIRMED);
    }
    @Test void cancellingReleasesBothTheSlotAndTheDailyQuota() {
        student("s@iit.ac.lk"); student("other@iit.ac.lk");
        var first=slot(Facility.Type.STUDY_ROOM,NOW.plusSeconds(3600));
        var second=slot(Facility.Type.COMPUTER_LAB,NOW.plusSeconds(7200));
        var booking=service.create("s@iit.ac.lk",first.getId());
        service.cancel("s@iit.ac.lk",booking.id(),false);
        assertThat(service.create("s@iit.ac.lk",second.getId()).status()).isEqualTo(Booking.Status.CONFIRMED);
        assertThat(service.create("other@iit.ac.lk",first.getId()).status()).isEqualTo(Booking.Status.CONFIRMED);
    }
    @Test void studentsCannotCancelSomeoneElsesBooking() throws Exception {
        student("s@iit.ac.lk");
        var b=service.create("s@iit.ac.lk",slot(Facility.Type.STUDY_ROOM,NOW.plusSeconds(3600)).getId());
        mvc.perform(post("/api/bookings/"+b.id()+"/cancel").with(csrf()).with(user("other@iit.ac.lk").roles("STUDENT"))).andExpect(status().isNotFound());
    }
    @Test void databaseRejectsDuplicateSlotsAndDuplicateDailyQuotaWithoutJavaChecks() {
        var u=student("s@iit.ac.lk"); var other=student("other@iit.ac.lk");
        var one=slot(Facility.Type.STUDY_ROOM,NOW.plusSeconds(3600));
        var two=slot(Facility.Type.COMPUTER_LAB,NOW.plusSeconds(7200));
        service.create(u.getEmail(),one.getId());
        assertThatThrownBy(() -> jdbc.update("insert into booking(user_id,slot_id,booking_date,status) values(?,?,?,'CONFIRMED')",other.getId(),one.getId(),LocalDate.of(2026,10,24))).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("insert into booking(user_id,slot_id,booking_date,status) values(?,?,?,'CONFIRMED')",u.getId(),two.getId(),LocalDate.of(2026,10,24))).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
    @Test void simultaneousStudentsCompetingForTheSameSlotHaveOneWinner() throws Exception {
        student("a@iit.ac.lk"); student("b@iit.ac.lk");
        var s=slot(Facility.Type.STUDY_ROOM,NOW.plusSeconds(3600));
        assertOneWinner(() -> service.create("a@iit.ac.lk",s.getId()),() -> service.create("b@iit.ac.lk",s.getId()));
    }
    @Test void simultaneousRequestsByOneStudentForDifferentRoomsHaveOneWinner() throws Exception {
        student("s@iit.ac.lk");
        var a=slot(Facility.Type.STUDY_ROOM,NOW.plusSeconds(3600)); var b=slot(Facility.Type.COMPUTER_LAB,NOW.plusSeconds(7200));
        assertOneWinner(() -> service.create("s@iit.ac.lk",a.getId()),() -> service.create("s@iit.ac.lk",b.getId()));
    }
    void assertOneWinner(Runnable first,Runnable second) throws Exception {
        var ready=new CountDownLatch(2); var start=new CountDownLatch(1); var pool=Executors.newFixedThreadPool(2);
        try {
            var jobs=new ArrayList<Future<Boolean>>();
            for (var action:List.of(first,second)) jobs.add(pool.submit(() -> { ready.countDown(); start.await(10,TimeUnit.SECONDS); try { action.run(); return true; } catch (ApiException ex) { return false; } }));
            assertThat(ready.await(10,TimeUnit.SECONDS)).isTrue(); start.countDown();
            assertThat(jobs.get(0).get(20,TimeUnit.SECONDS) ^ jobs.get(1).get(20,TimeUnit.SECONDS)).isTrue();
            assertThat(bookings.count()).isEqualTo(1);
        } finally { pool.shutdownNow(); }
    }
}
