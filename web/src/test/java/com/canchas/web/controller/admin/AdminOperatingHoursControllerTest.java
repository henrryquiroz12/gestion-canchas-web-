package com.canchas.web.controller.admin;

import com.canchas.web.dto.EffectiveOperatingHoursDTO;
import com.canchas.web.model.BusinessConfiguration;
import com.canchas.web.model.DailyOperatingHours;
import com.canchas.web.service.BusinessConfigurationService;
import com.canchas.web.service.CourtBlockService;
import com.canchas.web.service.CourtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.*;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
public class AdminOperatingHoursControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BusinessConfigurationService configurationService;

    @Mock
    private CourtBlockService courtBlockService;

    @Mock
    private CourtService courtService;

    @Mock
    private Clock clock;

    @InjectMocks
    private AdminOperatingHoursController controller;

    @BeforeEach
    public void setup() {
        Clock fixedClock = Clock.fixed(LocalDateTime.of(2026, 10, 17, 12, 0).atZone(ZoneId.of("America/Lima")).toInstant(), ZoneId.of("America/Lima"));
        lenient().when(clock.instant()).thenReturn(fixedClock.instant());
        lenient().when(clock.getZone()).thenReturn(fixedClock.getZone());
        lenient().when(courtBlockService.findGroupedBlocks()).thenReturn(Collections.emptyList());
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void testGetHorarios_SinHorarioEspecial() throws Exception {
        BusinessConfiguration config = new BusinessConfiguration();
        config.setDefaultOpeningTime(LocalTime.of(7, 0));
        config.setDefaultClosingTime(LocalTime.of(23, 0));

        when(configurationService.getConfiguration()).thenReturn(config);
        when(configurationService.getEffectiveOperatingHours(any())).thenReturn(new EffectiveOperatingHoursDTO(false, null, null));
        when(configurationService.getDailyOperatingHours(any())).thenReturn(Optional.empty());
        when(courtBlockService.findAllActive()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/horarios"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/horarios/lista"));
    }

    @Test
    public void testGetHorarios_SinBloqueos() throws Exception {
        BusinessConfiguration config = new BusinessConfiguration();
        config.setDefaultOpeningTime(LocalTime.of(7, 0));
        config.setDefaultClosingTime(LocalTime.of(23, 0));

        when(configurationService.getConfiguration()).thenReturn(config);
        when(configurationService.getEffectiveOperatingHours(any())).thenReturn(new EffectiveOperatingHoursDTO(false, null, null));
        when(configurationService.getDailyOperatingHours(any())).thenReturn(Optional.empty());
        when(courtBlockService.findAllActive()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/horarios"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/horarios/lista"));
    }

    @Test
    public void testGetHorarios_ConHorarioEspecial() throws Exception {
        BusinessConfiguration config = new BusinessConfiguration();
        config.setDefaultOpeningTime(LocalTime.of(7, 0));
        config.setDefaultClosingTime(LocalTime.of(23, 0));

        DailyOperatingHours horarioEspecial = new DailyOperatingHours();
        horarioEspecial.setClosed(true);
        horarioEspecial.setReason("Mantenimiento");

        when(configurationService.getConfiguration()).thenReturn(config);
        when(configurationService.getEffectiveOperatingHours(any())).thenReturn(new EffectiveOperatingHoursDTO(true, null, null));
        when(configurationService.getDailyOperatingHours(any())).thenReturn(Optional.of(horarioEspecial));
        when(courtBlockService.findAllActive()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/horarios"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/horarios/lista"));
    }

    @Test
    public void testCerrarHoy23() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/admin/horarios/cerrar-23"))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/admin/horarios"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash().attributeExists("mensajeExito"));
    }

    @Test
    public void testRestaurarCierre00() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/admin/horarios/restaurar-00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/admin/horarios"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash().attributeExists("mensajeExito"));
    }

    @Test
    public void testCrearBloqueo_Slot() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/admin/horarios/bloqueos")
                .param("canchaId", "1")
                .param("tipoBloqueo", "SLOT")
                .param("horaInicio", "20:00")
                .param("motivoGeneral", "Mantenimiento")
                .param("motivoDetalle", "Luz"))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/admin/horarios"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash().attributeExists("mensajeExito"));
    }

    @Test
    public void testCrearBloqueo_RangoUnico() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/admin/horarios/bloqueos")
                .param("canchaId", "1")
                .param("horaInicio", "17:00")
                .param("horaFin", "18:00")
                .param("motivoGeneral", "Mantenimiento")
                .param("motivoDetalle", "Pintado"))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/admin/horarios"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash().attributeExists("mensajeExito"));
    }

    @Test
    public void testCrearBloqueo_Rango() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/admin/horarios/bloqueos")
                .param("canchaId", "1")
                .param("tipoBloqueo", "RANGO")
                .param("horaInicio", "10:00")
                .param("horaFin", "15:00")
                .param("motivoGeneral", "Limpieza"))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/admin/horarios"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash().attributeExists("mensajeExito"));
    }

    @Test
    public void testCrearBloqueo_TodoElDia() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/admin/horarios/bloqueos")
                .param("canchaId", "1")
                .param("tipoBloqueo", "TODO_EL_DIA")
                .param("motivoGeneral", "Mantenimiento"))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/admin/horarios"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash().attributeExists("mensajeExito"));
    }

    @Test
    public void testDesbloquearGrupo() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/admin/horarios/bloqueos/desbloquear-grupo")
                .param("ids", "1", "2", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/admin/horarios"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash().attributeExists("mensajeExito"));
    }
}
