package com.canchas.web.repository;

import com.canchas.web.model.Reservation;
import com.canchas.web.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("SELECT r FROM Reservation r JOIN FETCH r.client u JOIN FETCH u.persona p JOIN FETCH r.court WHERE p.dni = :dni ORDER BY r.reservationDate DESC, r.startTime DESC, r.id DESC")
    List<Reservation> searchByClientDni(@Param("dni") String dni, Pageable limit);

    @Query("SELECT r FROM Reservation r JOIN FETCH r.client u JOIN FETCH u.persona p JOIN FETCH r.court WHERE p.dni LIKE CONCAT(:prefix, '%') ORDER BY r.reservationDate DESC, r.startTime DESC, r.id DESC")
    List<Reservation> searchByClientDniPrefix(@Param("prefix") String prefix, Pageable limit);

    @Query("SELECT r FROM Reservation r JOIN FETCH r.client u JOIN FETCH u.persona p JOIN FETCH r.court WHERE LOWER(CONCAT(p.nombres, ' ', p.apellidoPaterno, ' ', p.apellidoMaterno)) LIKE LOWER(CONCAT('%', :name, '%')) ORDER BY r.reservationDate DESC, r.startTime DESC, r.id DESC")
    List<Reservation> searchByClientName(@Param("name") String name, Pageable limit);

    @Query("SELECT r FROM Reservation r WHERE r.client = :client ORDER BY r.reservationDate DESC, r.startTime DESC")
    List<Reservation> findByClient(@Param("client") User client);

    @Query(value = "SELECT r FROM Reservation r WHERE r.client.id = :clientId ORDER BY r.reservationDate DESC, r.startTime DESC",
           countQuery = "SELECT COUNT(r) FROM Reservation r WHERE r.client.id = :clientId")
    Page<Reservation> findByClientIdPaginated(@Param("clientId") Long clientId, Pageable pageable);

    @Query(value = "SELECT r FROM Reservation r ORDER BY r.reservationDate DESC, r.startTime DESC",
           countQuery = "SELECT COUNT(r) FROM Reservation r")
    Page<Reservation> findAllPaginated(Pageable pageable);

    @Query("SELECT r FROM Reservation r WHERE r.reservationDate = :reservationDate ORDER BY r.startTime ASC")
    List<Reservation> findByReservationDate(@Param("reservationDate") LocalDate reservationDate);

    @Query("SELECT r FROM Reservation r WHERE r.reservationDate BETWEEN :startDate AND :endDate ORDER BY r.reservationDate ASC, r.startTime ASC")
    List<Reservation> findByReservationDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT r FROM Reservation r WHERE r.status = :status ORDER BY r.reservationDate DESC, r.startTime DESC")
    List<Reservation> findByStatus(@Param("status") Reservation.ReservationStatus status);

    @Query("""
            SELECT r FROM Reservation r 
            WHERE r.court.id = :courtId 
              AND r.reservationDate = :date 
              AND r.status <> 'CANCELADO' 
              AND r.startTime < :endTime 
              AND r.endTime > :startTime
            """)
    List<Reservation> findOverlappingReservations(
            @Param("courtId") Long courtId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);

    @Query("SELECT r FROM Reservation r WHERE r.court.id = :courtId AND r.reservationDate = :reservationDate AND r.status IN :statuses")
    List<Reservation> findByCourtIdAndReservationDateAndStatusIn(@Param("courtId") Long courtId, @Param("reservationDate") LocalDate reservationDate, @Param("statuses") Collection<Reservation.ReservationStatus> statuses);

    @Query("SELECT r FROM Reservation r WHERE r.reservationDate = :reservationDate AND r.status IN :statuses")
    List<Reservation> findByReservationDateAndStatusIn(@Param("reservationDate") LocalDate reservationDate, @Param("statuses") Collection<Reservation.ReservationStatus> statuses);

    @Query("SELECT COUNT(r) > 0 FROM Reservation r WHERE r.court.id = :courtId AND r.reservationDate = :reservationDate AND r.startTime = :startTime AND r.status IN :statuses")
    boolean existsByCourtIdAndReservationDateAndStartTimeAndStatusIn(@Param("courtId") Long courtId, @Param("reservationDate") LocalDate reservationDate, @Param("startTime") LocalTime startTime, @Param("statuses") Collection<Reservation.ReservationStatus> statuses);

    @Query("SELECT r FROM Reservation r WHERE r.court.id = :courtId AND r.reservationDate >= :reservationDate AND r.status IN :statuses")
    List<Reservation> findByCourtIdAndReservationDateGreaterThanEqualAndStatusIn(@Param("courtId") Long courtId, @Param("reservationDate") LocalDate reservationDate, @Param("statuses") Collection<Reservation.ReservationStatus> statuses);
}
