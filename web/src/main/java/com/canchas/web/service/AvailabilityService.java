package com.canchas.web.service;

import com.canchas.web.dto.AvailableSlotDTO;
import java.time.LocalDate;
import java.util.List;

public interface AvailabilityService {
    List<AvailableSlotDTO> getAvailability(Long courtId, LocalDate date);
}
