package lk.campusslot.config;

import org.springframework.context.annotation.*;
import java.time.*;

@Configuration
public class TimeConfig {
    public static final ZoneId CAMPUS_ZONE = ZoneId.of("Asia/Colombo");
    @Bean public Clock clock() { return Clock.systemUTC(); }
}
