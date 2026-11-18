package com.canchas.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TimeConfig {
    @Bean
    public Clock clock() {
        return Clock.system(com.canchas.web.util.LimaTime.ZONE_ID);
    }
}
