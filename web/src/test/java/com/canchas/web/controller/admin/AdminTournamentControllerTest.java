package com.canchas.web.controller.admin;

import com.canchas.web.model.TournamentRegistration;
import com.canchas.web.model.TournamentRegistrationStatus;
import com.canchas.web.service.TournamentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class AdminTournamentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TournamentService tournamentService;

    @InjectMocks
    private AdminTournamentController controller;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void testListRegistrations() throws Exception {
        when(tournamentService.findAllRegistrations()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/torneos"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/torneos/lista"))
                .andExpect(model().attributeExists("inscripciones"))
                .andExpect(model().attribute("totalInscripciones", 0))
                .andExpect(model().attribute("activeSection", "gestion"));
    }

    @Test
    public void testConfirmRegistration() throws Exception {
        TournamentRegistration reg = new TournamentRegistration();
        reg.setId(10L);
        reg.setTeamName("Equipo Alfa");
        reg.setStatus(TournamentRegistrationStatus.CONFIRMADA);

        when(tournamentService.confirmRegistration(10L)).thenReturn(reg);

        mockMvc.perform(post("/admin/torneos/inscripciones/10/confirmar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/torneos"))
                .andExpect(flash().attributeExists("exito"));
    }

    @Test
    public void testCancelRegistration() throws Exception {
        TournamentRegistration reg = new TournamentRegistration();
        reg.setId(10L);
        reg.setTeamName("Equipo Alfa");
        reg.setStatus(TournamentRegistrationStatus.CANCELADA);

        when(tournamentService.cancelRegistration(10L)).thenReturn(reg);

        mockMvc.perform(post("/admin/torneos/inscripciones/10/cancelar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/torneos"))
                .andExpect(flash().attributeExists("info"));
    }
}