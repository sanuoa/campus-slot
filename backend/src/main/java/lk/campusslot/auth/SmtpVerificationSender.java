package lk.campusslot.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import lk.campusslot.shared.ApiException;
import org.springframework.http.HttpStatus;

@Component @Profile("!local")
public class SmtpVerificationSender implements VerificationSender {
    private final JavaMailSender sender;
    private final String from;
    public SmtpVerificationSender(JavaMailSender sender, @Value("${app.mail-from}") String from) { this.sender = sender; this.from = from; }
    @Override public void send(String email, String url) {
        var message = new SimpleMailMessage();
        message.setFrom(from); message.setTo(email); message.setSubject("Verify your Campus Slot account");
        message.setText("Verify your IIT email for Campus Slot:\n\n" + url + "\n\nThis link expires in 24 hours. If you did not request it, ignore this email.");
        try { sender.send(message); }
        catch (MailException ex) { throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Email delivery is unavailable. Please try registration again later."); }
    }
}
