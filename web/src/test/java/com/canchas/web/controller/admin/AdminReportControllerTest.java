package com.canchas.web.controller.admin;

import com.canchas.web.repository.ReservationRepository;
import com.canchas.web.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class AdminReportControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ReportService reportService;

    @InjectMocks
    private AdminReportController controller;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void testReportesRangoValido() throws Exception {
        when(reservationRepository.findByReservationDateBetween(any(), any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/reportes")
                .param("fechaDesde", "2026-10-01")
                .param("fechaHasta", "2026-10-31"))
               .andExpect(status().isOk())
               .andExpect(view().name("admin/reportes/lista"))
               .andExpect(model().attributeExists("totalReservas"));
    }

    @Test
    public void testReportesRangoInvalido() throws Exception {
        when(reservationRepository.findByReservationDateBetween(any(), any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/reportes")
                .param("fechaDesde", "2026-11-01")
                .param("fechaHasta", "2026-10-31"))
               .andExpect(status().isOk())
               .andExpect(view().name("admin/reportes/lista"))
               .andExpect(model().attributeExists("mensajeError"));
    }

    @Test
    public void testDescargarExcel() throws Exception {
        when(reportService.generateExcel(any())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/admin/reportes/excel")
                .param("fechaDesde", "2026-10-01")
                .param("fechaHasta", "2026-10-31"))
               .andExpect(status().isOk())
               .andExpect(header().string("Content-Disposition", "attachment; filename=\"reporte-reservas-2026-10-01_2026-10-31.xlsx\""))
               .andExpect(content().contentType("application/octet-stream"));
    }
}
