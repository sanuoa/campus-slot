package lk.campusslot.auth;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.slf4j.*;

@Component @ConditionalOnProperty(name="app.mail.delivery", havingValue="console")
public class LocalVerificationSender implements VerificationSender {
    private static final Logger log = LoggerFactory.getLogger(LocalVerificationSender.class);
    @Override public void send(String email, String url) {
        log.info("LOCAL DEVELOPMENT ONLY — verification for {}: {}", email, url);
    }
}
