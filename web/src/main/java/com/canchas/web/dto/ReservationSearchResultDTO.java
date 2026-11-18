package com.canchas.web.dto;

import com.canchas.web.model.Reservation;
import java.math.BigDecimal;
import java.time.*;

public record ReservationSearchResultDTO(Long id, String fullName, String dni, String courtName,
        LocalDate date, LocalTime startTime, LocalTime endTime, BigDecimal amount, Reservation.ReservationStatus status) {
    public static ReservationSearchResultDTO from(Reservation reservation) {
        return new ReservationSearchResultDTO(reservation.getId(), reservation.getClient().getFullName(),
                reservation.getClient().getDni(), reservation.getCourt().getName(), reservation.getReservationDate(),
                reservation.getStartTime(), reservation.getEndTime(), reservation.getTotalAmount(), reservation.getStatus());
    }
}
