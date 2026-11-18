package com.canchas.web.repository;

import com.canchas.web.model.InternalReceipt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InternalReceiptRepository extends JpaRepository<InternalReceipt, Long> {

    @Query("SELECT ir FROM InternalReceipt ir WHERE ir.reservation.id = :reservationId")
    Optional<InternalReceipt> findByReservationId(@Param("reservationId") Long reservationId);

    @Query("SELECT ir FROM InternalReceipt ir WHERE ir.issuedAt BETWEEN :start AND :end ORDER BY ir.issuedAt DESC")
    List<InternalReceipt> findByIssuedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = "SELECT ir FROM InternalReceipt ir ORDER BY ir.issuedAt DESC",
           countQuery = "SELECT COUNT(ir) FROM InternalReceipt ir")
    Page<InternalReceipt> findAllPaginated(Pageable pageable);
}
