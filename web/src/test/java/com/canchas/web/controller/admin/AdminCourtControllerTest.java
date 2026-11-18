package com.canchas.web.controller.admin;

import com.canchas.web.model.Court;
import com.canchas.web.model.Reservation;
import com.canchas.web.repository.CourtRepository;
import com.canchas.web.repository.ReservationRepository;
import com.canchas.web.service.CourtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.*;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class AdminCourtControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CourtService courtService;

    @Mock
    private CourtRepository courtRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private Clock clock;

    @InjectMocks
    private AdminCourtController controller;

    private final LocalDate hoy = LocalDate.of(2026, 10, 18);

    @BeforeEach
    public void setup() {
        Clock fixedClock = Clock.fixed(LocalDateTime.of(hoy, LocalTime.of(12, 0)).atZone(ZoneId.of("America/Lima")).toInstant(), ZoneId.of("America/Lima"));
        lenient().when(clock.instant()).thenReturn(fixedClock.instant());
        lenient().when(clock.getZone()).thenReturn(fixedClock.getZone());
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void testListarCanchas_Retorna200YVistaCorrecta() throws Exception {
        Court c = new Court();
        c.setId(1L);
        c.setName("Cancha 1");
        c.setType(Court.CourtType.NORMAL);
        c.setHourlyRate(new BigDecimal("40.00"));
        c.setAvailable(true);

        when(courtService.findAllPaginated(0, 15)).thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(c)));

        mockMvc.perform(get("/admin/canchas"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/canchas/lista"))
                .andExpect(model().attributeExists("canchas"))
                .andExpect(model().attribute("activeSection", "canchas"));
    }

    @Test
    public void testEditarPrecio_Valido() throws Exception {
        Court c = new Court();
        c.setId(1L);
        c.setName("Cancha 1");
        c.setHourlyRate(new BigDecimal("50.00"));

        when(courtService.update(eq(1L), any(Court.class))).thenReturn(c);

        mockMvc.perform(post("/admin/canchas/1/editar")
                .param("descripcion", "Descripción editada que debe persistirse")
                .param("precioHora", "50.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/canchas"))
                .andExpect(flash().attributeExists("mensajeExito"));
        verify(courtService).update(eq(1L), argThat(court ->
                new BigDecimal("50.00").compareTo(court.getHourlyRate()) == 0
                && "Descripción editada que debe persistirse".equals(court.getDescription())));
    }

    @Test
    public void testEditarPrecio_Invalido() throws Exception {
        mockMvc.perform(post("/admin/canchas/1/editar")
                .param("precioHora", "-10.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/canchas"))
                .andExpect(flash().attributeExists("mensajeError"));
    }

    @Test
    public void testDeshabilitarCancha_ConservaReservasYRedirigeAlPanel() throws Exception {
        Court court = new Court();
        court.setName("Cancha 1");
        court.setAvailable(false);
        when(courtService.changeAvailability(1L, false)).thenReturn(court);
        mockMvc.perform(post("/admin/canchas/1/estado").param("disponible", "false"))
                .andExpect(redirectedUrl("/admin/canchas"))
                .andExpect(flash().attributeExists("mensajeExito"));
        verify(courtService).changeAvailability(1L, false);
        verifyNoInteractions(reservationRepository);
    }
}
