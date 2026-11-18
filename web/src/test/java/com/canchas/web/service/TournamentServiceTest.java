package com.canchas.web.service;

import com.canchas.web.exception.DuplicateDataException;
import com.canchas.web.exception.ResourceNotFoundException;
import com.canchas.web.model.Tournament;
import com.canchas.web.model.TournamentRegistration;
import com.canchas.web.model.TournamentRegistrationStatus;
import com.canchas.web.model.User;
import com.canchas.web.repository.TournamentRegistrationRepository;
import com.canchas.web.repository.TournamentRepository;
import com.canchas.web.repository.UserRepository;
import com.canchas.web.service.impl.TournamentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TournamentServiceTest {

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private TournamentRegistrationRepository registrationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TournamentServiceImpl tournamentService;

    private Tournament freeTournament;
    private Tournament paidTournament;
    private User clientUser;
    private User adminUser;

    @BeforeEach
    public void setup() {
        freeTournament = new Tournament();
        freeTournament.setId(1L);
        freeTournament.setName("Torneo Relámpago Gratuito");
        freeTournament.setSlug("torneo-relampago-gratis");
        freeTournament.setPricePerTeam(BigDecimal.ZERO);
        freeTournament.setRegistrationsOpen(true);
        freeTournament.setStatus("ACTIVO");

        paidTournament = new Tournament();
        paidTournament.setId(2L);
        paidTournament.setName("Copa Fredy Queen Pro");
        paidTournament.setSlug("copa-fredy-queen-pro");
        paidTournament.setPricePerTeam(new BigDecimal("250.00"));
        paidTournament.setRegistrationsOpen(true);
        paidTournament.setStatus("ACTIVO");

        clientUser = new User();
        clientUser.setId(10L);
        clientUser.setUsername("juanperez");
        clientUser.setRole(User.Role.CLIENTE);

        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("admin");
        adminUser.setRole(User.Role.ADMIN);
    }

    @Test
    public void testRegisterTeam_FreeTournament_DirectlyConfirmed() {
        when(tournamentRepository.findBySlug("torneo-relampago-gratis")).thenReturn(Optional.of(freeTournament));
        when(userRepository.findById(10L)).thenReturn(Optional.of(clientUser));
        when(registrationRepository.existsByTournamentAndClient(freeTournament, clientUser)).thenReturn(false);
        when(registrationRepository.existsByTournamentAndTeamNameIgnoreCase(freeTournament, "Los Galácticos")).thenReturn(false);
        when(registrationRepository.save(any(TournamentRegistration.class))).thenAnswer(i -> i.getArgument(0));

        TournamentRegistration reg = tournamentService.registerTeam("torneo-relampago-gratis", 10L, "Los Galácticos");

        assertNotNull(reg);
        assertEquals("Los Galácticos", reg.getTeamName());
        assertEquals(TournamentRegistrationStatus.CONFIRMADA, reg.getStatus());
        assertEquals(BigDecimal.ZERO, reg.getAppliedAmount());
    }

    @Test
    public void testRegisterTeam_PaidTournament_PendingPayment() {
        when(tournamentRepository.findBySlug("copa-fredy-queen-pro")).thenReturn(Optional.of(paidTournament));
        when(userRepository.findById(10L)).thenReturn(Optional.of(clientUser));
        when(registrationRepository.existsByTournamentAndClient(paidTournament, clientUser)).thenReturn(false);
        when(registrationRepository.existsByTournamentAndTeamNameIgnoreCase(paidTournament, "Rayo F.C.")).thenReturn(false);
        when(registrationRepository.save(any(TournamentRegistration.class))).thenAnswer(i -> i.getArgument(0));

        TournamentRegistration reg = tournamentService.registerTeam("copa-fredy-queen-pro", 10L, "Rayo F.C.");

        assertNotNull(reg);
        assertEquals("Rayo F.C.", reg.getTeamName());
        assertEquals(TournamentRegistrationStatus.PENDIENTE_PAGO, reg.getStatus());
        assertEquals(new BigDecimal("250.00"), reg.getAppliedAmount());
    }

    @Test
    public void testRegisterTeam_DuplicateTeamName_Rejected() {
        when(tournamentRepository.findBySlug("copa-fredy-queen-pro")).thenReturn(Optional.of(paidTournament));
        when(userRepository.findById(10L)).thenReturn(Optional.of(clientUser));
        when(registrationRepository.existsByTournamentAndClient(paidTournament, clientUser)).thenReturn(false);
        when(registrationRepository.existsByTournamentAndTeamNameIgnoreCase(paidTournament, "Rayo F.C.")).thenReturn(true);

        DuplicateDataException ex = assertThrows(DuplicateDataException.class, () ->
            tournamentService.registerTeam("copa-fredy-queen-pro", 10L, "Rayo F.C.")
        );
        assertTrue(ex.getMessage().contains("ya ha sido registrado"));
    }

    @Test
    public void testRegisterTeam_DuplicateClient_Rejected() {
        when(tournamentRepository.findBySlug("copa-fredy-queen-pro")).thenReturn(Optional.of(paidTournament));
        when(userRepository.findById(10L)).thenReturn(Optional.of(clientUser));
        when(registrationRepository.existsByTournamentAndClient(paidTournament, clientUser)).thenReturn(true);

        DuplicateDataException ex = assertThrows(DuplicateDataException.class, () ->
            tournamentService.registerTeam("copa-fredy-queen-pro", 10L, "Otro Equipo")
        );
        assertEquals("Ya tienes un equipo registrado en este torneo.", ex.getMessage());
    }

    @Test
    public void testRegisterTeam_ClosedTournament_Rejected() {
        paidTournament.setRegistrationsOpen(false);
        when(tournamentRepository.findBySlug("copa-fredy-queen-pro")).thenReturn(Optional.of(paidTournament));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
            tournamentService.registerTeam("copa-fredy-queen-pro", 10L, "Los Tigres")
        );
        assertTrue(ex.getMessage().contains("cerradas"));
    }

    @Test
    public void testRegisterTeam_AdminUser_Rejected() {
        when(tournamentRepository.findBySlug("copa-fredy-queen-pro")).thenReturn(Optional.of(paidTournament));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            tournamentService.registerTeam("copa-fredy-queen-pro", 1L, "Admin FC")
        );
        assertEquals("Solo los clientes pueden inscribir equipos.", ex.getMessage());
    }

    @Test
    public void testConfirmRegistration_AdminAction() {
        TournamentRegistration reg = new TournamentRegistration();
        reg.setId(50L);
        reg.setTeamName("Deportivo Sur");
        reg.setStatus(TournamentRegistrationStatus.PENDIENTE_PAGO);

        when(registrationRepository.findById(50L)).thenReturn(Optional.of(reg));
        when(registrationRepository.save(any(TournamentRegistration.class))).thenAnswer(i -> i.getArgument(0));

        TournamentRegistration confirmed = tournamentService.confirmRegistration(50L);
        assertEquals(TournamentRegistrationStatus.CONFIRMADA, confirmed.getStatus());
    }

    @Test
    public void testCancelRegistration_AdminAction() {
        TournamentRegistration reg = new TournamentRegistration();
        reg.setId(50L);
        reg.setTeamName("Deportivo Sur");
        reg.setStatus(TournamentRegistrationStatus.PENDIENTE_PAGO);

        when(registrationRepository.findById(50L)).thenReturn(Optional.of(reg));
        when(registrationRepository.save(any(TournamentRegistration.class))).thenAnswer(i -> i.getArgument(0));

        TournamentRegistration cancelled = tournamentService.cancelRegistration(50L);
        assertEquals(TournamentRegistrationStatus.CANCELADA, cancelled.getStatus());
    }

    @Test
    public void testFindAllActive() {
        when(tournamentRepository.findByStatusOrderByCreatedAtAsc("ACTIVO"))
                .thenReturn(List.of(freeTournament, paidTournament));

        List<Tournament> result = tournamentService.findAllActive();
        assertEquals(2, result.size());
    }
}