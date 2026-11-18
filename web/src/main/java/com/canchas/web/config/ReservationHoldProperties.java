package com.canchas.web.config;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component @ConfigurationProperties(prefix = "fqs.reservation-hold") @Validated
@Getter @Setter
public class ReservationHoldProperties {
    @Min(1)
    private long minutes = 15;
}
