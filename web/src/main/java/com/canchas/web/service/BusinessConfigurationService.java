package com.canchas.web.service;

import com.canchas.web.dto.EffectiveOperatingHoursDTO;
import com.canchas.web.model.BusinessConfiguration;
import com.canchas.web.model.DailyOperatingHours;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

public interface BusinessConfigurationService {
    BusinessConfiguration getConfiguration();
    EffectiveOperatingHoursDTO getEffectiveOperatingHours(LocalDate date);
    BigDecimal calculatePriceWithLighting(BigDecimal basePrice, LocalTime startTime);
    BigDecimal getLightingSurcharge(LocalTime startTime);
    DailyOperatingHours saveSpecialOperatingHours(LocalDate date, LocalTime openingTime, LocalTime closingTime, boolean closed, String reason);
    Optional<DailyOperatingHours> getDailyOperatingHours(LocalDate date);

    default DailyOperatingHours saveSpecialHours(LocalDate date, LocalTime openingTime, LocalTime closingTime, boolean closed, String reason) {
        return saveSpecialOperatingHours(date, openingTime, closingTime, closed, reason);
    }
}
