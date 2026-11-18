package com.canchas.web.repository;

import com.canchas.web.model.Tournament;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TournamentRepository extends JpaRepository<Tournament, Long> {

    @Query("SELECT t FROM Tournament t WHERE t.slug = :slug")
    Optional<Tournament> findBySlug(@Param("slug") String slug);

    @Query("SELECT t FROM Tournament t WHERE t.status = :status ORDER BY t.createdAt ASC")
    List<Tournament> findByStatusOrderByCreatedAtAsc(@Param("status") String status);

    @Query(value = "SELECT t FROM Tournament t ORDER BY t.createdAt DESC",
           countQuery = "SELECT COUNT(t) FROM Tournament t")
    Page<Tournament> findAllPaginated(Pageable pageable);
}
