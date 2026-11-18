package com.canchas.web.repository;

import com.canchas.web.model.Tournament;
import com.canchas.web.model.TournamentRegistration;
import com.canchas.web.model.TournamentRegistrationStatus;
import com.canchas.web.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TournamentRegistrationRepository extends JpaRepository<TournamentRegistration, Long> {

    @Query("SELECT tr FROM TournamentRegistration tr WHERE tr.tournament = :tournament ORDER BY tr.registrationDate ASC")
    List<TournamentRegistration> findByTournamentOrderByRegistrationDateAsc(@Param("tournament") Tournament tournament);

    @Query("SELECT tr FROM TournamentRegistration tr WHERE tr.tournament = :tournament AND tr.status = :status ORDER BY tr.registrationDate ASC")
    List<TournamentRegistration> findByTournamentAndStatusOrderByRegistrationDateAsc(@Param("tournament") Tournament tournament, @Param("status") TournamentRegistrationStatus status);

    @Query("SELECT tr FROM TournamentRegistration tr WHERE tr.tournament = :tournament AND tr.client = :client")
    Optional<TournamentRegistration> findByTournamentAndClient(@Param("tournament") Tournament tournament, @Param("client") User client);

    @Query("SELECT COUNT(tr) > 0 FROM TournamentRegistration tr WHERE tr.tournament = :tournament AND tr.client = :client")
    boolean existsByTournamentAndClient(@Param("tournament") Tournament tournament, @Param("client") User client);

    @Query("SELECT COUNT(tr) > 0 FROM TournamentRegistration tr WHERE tr.tournament = :tournament AND LOWER(tr.teamName) = LOWER(:teamName)")
    boolean existsByTournamentAndTeamNameIgnoreCase(@Param("tournament") Tournament tournament, @Param("teamName") String teamName);

    @Query("SELECT tr FROM TournamentRegistration tr ORDER BY tr.registrationDate DESC")
    List<TournamentRegistration> findAllByOrderByRegistrationDateDesc();

    @Query(value = "SELECT tr FROM TournamentRegistration tr ORDER BY tr.registrationDate DESC",
           countQuery = "SELECT COUNT(tr) FROM TournamentRegistration tr")
    Page<TournamentRegistration> findAllPaginated(Pageable pageable);
}
