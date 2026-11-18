package com.canchas.web.service.impl;

import com.canchas.web.exception.DuplicateDataException;
import com.canchas.web.exception.ResourceNotFoundException;
import com.canchas.web.model.Tournament;
import com.canchas.web.model.TournamentRegistration;
import com.canchas.web.model.TournamentRegistrationStatus;
import com.canchas.web.model.User;
import com.canchas.web.repository.TournamentRegistrationRepository;
import com.canchas.web.repository.TournamentRepository;
import com.canchas.web.repository.UserRepository;
import com.canchas.web.service.TournamentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TournamentServiceImpl implements TournamentService {

    private final TournamentRepository tournamentRepository;
    private final TournamentRegistrationRepository registrationRepository;
    private final UserRepository userRepository;

    @Override
    public List<Tournament> findAllActive() {
        return tournamentRepository.findByStatusOrderByCreatedAtAsc("ACTIVO");
    }

    @Override
    public Tournament findBySlug(String slug) {
        return tournamentRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Torneo no encontrado con identificador: " + slug));
    }

    @Override
    @Transactional
    public TournamentRegistration registerTeam(String slug, Long clientId, String teamName) {
        Tournament tournament = findBySlug(slug);

        if (!tournament.isRegistrationsOpen()) {
            throw new IllegalStateException("Las inscripciones para este torneo están cerradas.");
        }

        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));

        if (client.getRole() != User.Role.CLIENTE) {
            throw new IllegalArgumentException("Solo los clientes pueden inscribir equipos.");
        }

        if (teamName == null || teamName.trim().isBlank()) {
            throw new IllegalArgumentException("El nombre del equipo es obligatorio.");
        }
        String cleanTeamName = teamName.trim();
        if (cleanTeamName.length() < 2 || cleanTeamName.length() > 60) {
            throw new IllegalArgumentException("El nombre del equipo debe tener entre 2 y 60 caracteres.");
        }
        if (cleanTeamName.contains("<") || cleanTeamName.contains(">")) {
            throw new IllegalArgumentException("El nombre del equipo no puede contener caracteres HTML.");
        }

        if (registrationRepository.existsByTournamentAndClient(tournament, client)) {
            throw new DuplicateDataException("Ya tienes un equipo registrado en este torneo.");
        }

        if (registrationRepository.existsByTournamentAndTeamNameIgnoreCase(tournament, cleanTeamName)) {
            throw new DuplicateDataException("El nombre de equipo '" + cleanTeamName + "' ya ha sido registrado en este torneo. Por favor elige otro nombre.");
        }

        BigDecimal appliedAmount = tournament.getPricePerTeam() != null ? tournament.getPricePerTeam() : BigDecimal.ZERO;

        TournamentRegistrationStatus initialStatus;
        if (appliedAmount.compareTo(BigDecimal.ZERO) == 0) {
            initialStatus = TournamentRegistrationStatus.CONFIRMADA;
        } else {
            initialStatus = TournamentRegistrationStatus.PENDIENTE_PAGO;
        }

        TournamentRegistration registration = new TournamentRegistration();
        registration.setTournament(tournament);
        registration.setClient(client);
        registration.setTeamName(cleanTeamName);
        registration.setAppliedAmount(appliedAmount);
        registration.setStatus(initialStatus);
        registration.setRegistrationDate(LocalDateTime.now());

        return registrationRepository.save(registration);
    }

    @Override
    public Optional<TournamentRegistration> findRegistrationByTournamentAndClient(Tournament tournament, Long clientId) {
        if (clientId == null) return Optional.empty();
        return userRepository.findById(clientId)
                .flatMap(client -> registrationRepository.findByTournamentAndClient(tournament, client));
    }

    @Override
    public List<TournamentRegistration> findConfirmedRegistrations(Tournament tournament) {
        return registrationRepository.findByTournamentAndStatusOrderByRegistrationDateAsc(tournament, TournamentRegistrationStatus.CONFIRMADA);
    }

    @Override
    public List<TournamentRegistration> findAllRegistrations() {
        return registrationRepository.findAllByOrderByRegistrationDateDesc();
    }

    @Override
    public Page<TournamentRegistration> findAllRegistrationsPaginated(int page, int size) {
        int pageNumber = Math.max(0, page);
        int pageSize = size > 0 ? size : 15;
        return registrationRepository.findAllPaginated(PageRequest.of(pageNumber, pageSize));
    }

    @Override
    @Transactional
    public TournamentRegistration confirmRegistration(Long registrationId) {
        TournamentRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Inscripción no encontrada con id: " + registrationId));
        registration.setStatus(TournamentRegistrationStatus.CONFIRMADA);
        return registrationRepository.save(registration);
    }

    @Override
    @Transactional
    public TournamentRegistration cancelRegistration(Long registrationId) {
        TournamentRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Inscripción no encontrada con id: " + registrationId));
        registration.setStatus(TournamentRegistrationStatus.CANCELADA);
        return registrationRepository.save(registration);
    }
}