package com.canchas.web.service;

import com.canchas.web.dto.ReservationRequestDTO;
import com.canchas.web.exception.InvalidDateException;
import com.canchas.web.model.Court;
import com.canchas.web.model.User;
import com.canchas.web.repository.*;
import com.canchas.web.service.impl.ReservationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceIntegrationTest {

    @Mock private CourtRepository courtRepository;
    @Mock private ReservationHoldRepository holdRepository;
    @Mock private ReservationRepository reservationRepository;
    @Mock private CourtBlockRepository courtBlockRepository;
    @Mock private UserService userService;
    @Mock private BusinessConfigurationService businessConfigurationService;
    @Mock private Clock clock;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    @BeforeEach
    void setUp() {
        reservationService = new ReservationServiceImpl(reservationRepository, courtRepository,
                new ReservationSlotRules(businessConfigurationService, reservationRepository, courtBlockRepository, holdRepository, clock),
                userService, businessConfigurationService, clock);
        Clock fixedClock = Clock.fixed(LocalDateTime.of(2026, 10, 5, 12, 0).atZone(ZoneId.of("America/Lima")).toInstant(), ZoneId.of("America/Lima"));
        lenient().when(clock.instant()).thenReturn(fixedClock.instant());
        lenient().when(clock.getZone()).thenReturn(fixedClock.getZone());
    }

    @Test
    void create_shouldRejectPastDate() {
        ReservationRequestDTO dto = new ReservationRequestDTO();
        dto.setCourtId(1L);
        dto.setReservationDate(LocalDate.now(clock).minusDays(1));
        dto.setStartTime(LocalTime.of(10, 0));

        User u = new User();
        u.setRole(User.Role.CLIENTE);
        lenient().when(userService.findById(1L)).thenReturn(u);

        Court c = new Court();
        c.setAvailable(true);
        lenient().when(courtRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(c));

        assertThrows(InvalidDateException.class, () -> reservationService.create(dto, 1L));
    }
}
