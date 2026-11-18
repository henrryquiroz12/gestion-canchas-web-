package com.canchas.web.repository;

import com.canchas.web.model.Court;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourtRepository extends JpaRepository<Court, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Court c ORDER BY c.id")
    List<Court> lockAllForScheduleUpdate();

    @Query("SELECT c FROM Court c WHERE c.available = true ORDER BY c.name ASC")
    List<Court> findByAvailableTrue();

    @Query("SELECT c FROM Court c WHERE c.type = :type ORDER BY c.name ASC")
    List<Court> findByType(@Param("type") Court.CourtType type);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Court c WHERE c.id = :id")
    Optional<Court> findByIdForUpdate(@Param("id") Long id);

    @Query(value = "SELECT c FROM Court c ORDER BY c.id ASC",
           countQuery = "SELECT COUNT(c) FROM Court c")
    Page<Court> findAllPaginated(Pageable pageable);
}
