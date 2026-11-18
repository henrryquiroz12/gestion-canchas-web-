package com.canchas.web.repository;

import com.canchas.web.model.DailyOperatingHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyOperatingHoursRepository extends JpaRepository<DailyOperatingHours, Long> {

    @Query("SELECT doh FROM DailyOperatingHours doh WHERE doh.date = :date")
    Optional<DailyOperatingHours> findByDate(@Param("date") LocalDate date);
}
