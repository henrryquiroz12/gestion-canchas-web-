package com.canchas.web.repository;

import com.canchas.web.model.CourtBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface CourtBlockRepository extends JpaRepository<CourtBlock, Long> {

    @Query("SELECT cb FROM CourtBlock cb WHERE cb.court.id = :courtId AND cb.date = :date AND cb.active = true")
    List<CourtBlock> findByCourtIdAndDateAndActiveTrue(@Param("courtId") Long courtId, @Param("date") LocalDate date);

    @Query("SELECT cb FROM CourtBlock cb WHERE cb.court.id = :courtId AND cb.date = :date AND cb.startTime = :startTime")
    Optional<CourtBlock> findByCourtIdAndDateAndStartTime(@Param("courtId") Long courtId, @Param("date") LocalDate date, @Param("startTime") LocalTime startTime);
}
