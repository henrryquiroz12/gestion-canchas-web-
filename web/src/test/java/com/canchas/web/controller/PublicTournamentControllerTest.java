package com.canchas.web.controller;

import com.canchas.web.model.Tournament;
import com.canchas.web.model.TournamentRegistration;
import com.canchas.web.model.TournamentRegistrationStatus;
import com.canchas.web.model.User;
import com.canchas.web.service.TournamentService;
import com.canchas.web.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class PublicTournamentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TournamentService tournamentService;

    @Mock
    private UserService userService;

    @InjectMocks
    private PublicTournamentController controller;

    private Tournament sampleTournament;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        sampleTournament = new Tournament();
        sampleTournament.setId(1L);
        sampleTournament.setName("Torneo Fredy Queen");
        sampleTournament.setSlug("torneo-fredy-queen");
        sampleTournament.setPricePerTeam(BigDecimal.ZERO);
        sampleTournament.setStatus("ACTIVO");
    }

    @Test
    public void testListTournaments() throws Exception {
        when(tournamentService.findAllActive()).thenReturn(List.of(sampleTournament));

        mockMvc.perform(get("/torneos"))
                .andExpect(status().isOk())
                .andExpect(view().name("torneos/lista"))
                .andExpect(model().attributeExists("torneos"))
                .andExpect(model().attribute("activeSection", "torneos"));
    }

    @Test
    public void testHowItWorks() throws Exception {
        mockMvc.perform(get("/torneos/como-funciona"))
                .andExpect(status().isOk())
                .andExpect(view().name("torneos/como-funciona"))
                .andExpect(model().attribute("activeSection", "torneos"));
    }

    @Test
    public void testTournamentDetail() throws Exception {
        when(tournamentService.findBySlug("torneo-fredy-queen")).thenReturn(sampleTournament);
        when(tournamentService.findConfirmedRegistrations(sampleTournament)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/torneos/torneo-fredy-queen"))
                .andExpect(status().isOk())
                .andExpect(view().name("torneos/detalle"))
                .andExpect(model().attributeExists("torneo"))
                .andExpect(model().attribute("totalConfirmados", 0));
    }

    @Test
    public void testRegisterTeam_Success() throws Exception {
        User client = new User();
        client.setId(15L);
        client.setUsername("cliente1");
        client.setRole(User.Role.CLIENTE);

        TournamentRegistration reg = new TournamentRegistration();
        reg.setTeamName("Los Tigres");
        reg.setStatus(TournamentRegistrationStatus.CONFIRMADA);

        when(userService.findByUsername("cliente1")).thenReturn(client);
        when(tournamentService.registerTeam("torneo-fredy-queen", 15L, "Los Tigres")).thenReturn(reg);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                "cliente1", "pwd", List.of(new SimpleGrantedAuthority("ROLE_CLIENTE")));

        mockMvc.perform(post("/torneos/torneo-fredy-queen/inscribirse")
                .principal(auth)
                .param("nombreEquipo", "Los Tigres"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/torneos/torneo-fredy-queen?inscrito=true"))
                .andExpect(flash().attributeExists("exito"));
    }
}