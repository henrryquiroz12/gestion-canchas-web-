package com.canchas.web.service;

import com.canchas.web.model.Tournament;
import com.canchas.web.model.TournamentRegistration;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface TournamentService {

    List<Tournament> findAllActive();

    Tournament findBySlug(String slug);

    TournamentRegistration registerTeam(String slug, Long clientId, String teamName);

    Optional<TournamentRegistration> findRegistrationByTournamentAndClient(Tournament tournament, Long clientId);

    List<TournamentRegistration> findConfirmedRegistrations(Tournament tournament);

    List<TournamentRegistration> findAllRegistrations();

    Page<TournamentRegistration> findAllRegistrationsPaginated(int page, int size);

    TournamentRegistration confirmRegistration(Long registrationId);

    TournamentRegistration cancelRegistration(Long registrationId);
}
