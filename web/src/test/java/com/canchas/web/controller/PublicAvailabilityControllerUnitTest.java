package com.canchas.web.controller;

import com.canchas.web.dto.AvailableSlotDTO;
import com.canchas.web.service.AvailabilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

public class PublicAvailabilityControllerUnitTest {

    @Mock
    private AvailabilityService availabilityService;

    @InjectMocks
    private PublicAvailabilityController controller;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void getAvailability_ShouldReturnSlotsForDate() {
        LocalDate date = LocalDate.of(2026, 10, 5);

        AvailableSlotDTO slot1 = new AvailableSlotDTO();
        slot1.setStartTime(LocalTime.of(17, 0));
        slot1.setEndTime(LocalTime.of(18, 0));
        slot1.setBasePrice(new BigDecimal("40.00"));
        slot1.setLightingSurcharge(BigDecimal.ZERO);
        slot1.setTotalPrice(new BigDecimal("40.00"));
        slot1.setStatus(AvailableSlotDTO.SlotStatus.DISPONIBLE);

        AvailableSlotDTO slot2 = new AvailableSlotDTO();
        slot2.setStartTime(LocalTime.of(18, 0));
        slot2.setEndTime(LocalTime.of(19, 0));
        slot2.setBasePrice(new BigDecimal("40.00"));
        slot2.setLightingSurcharge(new BigDecimal("20.00"));
        slot2.setTotalPrice(new BigDecimal("60.00"));
        slot2.setStatus(AvailableSlotDTO.SlotStatus.DISPONIBLE);

        when(availabilityService.getAvailability(1L, date)).thenReturn(Arrays.asList(slot1, slot2));

        ResponseEntity<List<AvailableSlotDTO>> response = controller.getAvailability(1L, date);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals(new BigDecimal("60.00"), response.getBody().get(1).getTotalPrice());
    }
}
