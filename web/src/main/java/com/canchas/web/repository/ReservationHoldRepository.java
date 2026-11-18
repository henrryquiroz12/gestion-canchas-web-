package com.canchas.web.repository;

import com.canchas.web.model.ReservationHold;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.*;
import java.util.*;

public interface ReservationHoldRepository extends JpaRepository<ReservationHold, String> {
    // Creates a durable per-session mutex, including on the first concurrent request.
    @Modifying
    @Query(value = "INSERT INTO retenciones_horario (owner_key, status) VALUES (:owner, 'CANCELLED') ON DUPLICATE KEY UPDATE owner_key = owner_key", nativeQuery = true)
    void initializeOwner(@Param("owner") String owner);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM ReservationHold h WHERE h.ownerKey = :owner")
    Optional<ReservationHold> lockOwner(@Param("owner") String owner);

    @Query("SELECT h FROM ReservationHold h WHERE h.court.id = :court AND h.date = :date AND h.status = com.canchas.web.model.ReservationHold.Status.ACTIVE AND h.expiresAt > :now")
    List<ReservationHold> findActive(@Param("court") Long court, @Param("date") LocalDate date, @Param("now") LocalDateTime now);

    @Query("SELECT h FROM ReservationHold h WHERE h.status = com.canchas.web.model.ReservationHold.Status.ACTIVE AND h.expiresAt > :now AND h.date = :date")
    List<ReservationHold> findActiveOnDate(@Param("date") LocalDate date, @Param("now") LocalDateTime now);
}
