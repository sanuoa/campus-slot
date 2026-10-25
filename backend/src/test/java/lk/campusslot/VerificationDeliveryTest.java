package lk.campusslot;

import lk.campusslot.auth.*;
import lk.campusslot.shared.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class VerificationDeliveryTest {
    private final ApplicationContextRunner context = new ApplicationContextRunner()
        .withUserConfiguration(LocalVerificationSender.class, SmtpVerificationSender.class)
        .withBean(JavaMailSender.class, () -> mock(JavaMailSender.class))
        .withPropertyValues("app.mail-from=sender@example.com", "spring.mail.host=smtp.gmail.com");

    @Test void smtpCanBeEnabledWithTheLocalProfile() {
        context.withPropertyValues("spring.profiles.active=local", "app.mail.delivery=smtp").run(c -> {
            assertThat(c).hasSingleBean(VerificationSender.class).hasSingleBean(SmtpVerificationSender.class);
            assertThat(c).doesNotHaveBean(LocalVerificationSender.class);
        });
    }
    @Test void consoleDeliveryIsExplicit() {
        context.withPropertyValues("app.mail.delivery=console").run(c -> {
            assertThat(c).hasSingleBean(LocalVerificationSender.class).doesNotHaveBean(SmtpVerificationSender.class);
        });
    }
    @Test void missingSmtpSettingsFailAtStartup() {
        context.withPropertyValues("app.mail.delivery=smtp", "spring.mail.host=").run(c -> assertThat(c).hasFailed());
        context.withPropertyValues("app.mail.delivery=smtp", "app.mail-from=").run(c -> assertThat(c).hasFailed());
    }
    @Test void consoleDeliveryCannotBeUsedOutsideLocalEvenWithUppercaseConfiguration() {
        var env=new org.springframework.mock.env.MockEnvironment().withProperty("app.mail.delivery","CONSOLE");
        var bootstrap=new lk.campusslot.config.Bootstrap(env,
            mock(UserRepository.class), mock(lk.campusslot.facility.FacilityRepository.class),
            mock(lk.campusslot.facility.SlotRepository.class),
            mock(org.springframework.security.crypto.password.PasswordEncoder.class), java.time.Clock.systemUTC());
        assertThatThrownBy(() -> bootstrap.run()).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("only allowed with the local profile");
    }
    @Test void smtpAddressesAndVerificationLinkAreSentToTheMailClient() {
        var client=mock(JavaMailSender.class);
        var sender=new SmtpVerificationSender(client,"sender@example.com","smtp.gmail.com");
        sender.send("student@iit.ac.lk","http://localhost:5173/?verify=test-token");
        var captured=org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(client).send(captured.capture());
        assertThat(captured.getValue().getFrom()).isEqualTo("sender@example.com");
        assertThat(captured.getValue().getTo()).containsExactly("student@iit.ac.lk");
        assertThat(captured.getValue().getText()).contains("http://localhost:5173/?verify=test-token", "24 hours");
    }
    @Test void smtpFailuresReturnAnActionableErrorWithoutLeakingProviderDetails() {
        var client=mock(JavaMailSender.class);
        doThrow(new MailSendException("private provider diagnostic")).when(client).send(any(SimpleMailMessage.class));
        var sender=new SmtpVerificationSender(client,"sender@example.com","smtp.gmail.com");
        assertThatThrownBy(() -> sender.send("student@iit.ac.lk","verification-link"))
            .isInstanceOf(ApiException.class).hasMessageContaining("Email delivery is unavailable")
            .hasMessageNotContaining("private provider diagnostic");
    }
}
