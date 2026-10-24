package lk.campusslot.auth;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.slf4j.*;

@Component @Profile("local")
public class LocalVerificationSender implements VerificationSender {
    private static final Logger log = LoggerFactory.getLogger(LocalVerificationSender.class);
    @Override public void send(String email, String url) {
        log.info("LOCAL DEVELOPMENT ONLY — verification for {}: {}", email, url);
    }
}
