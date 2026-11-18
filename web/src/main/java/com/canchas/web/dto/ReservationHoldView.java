package com.canchas.web.dto;

import java.time.*;

public record ReservationHoldView(String status, String token, Long courtId, String courtName,
        LocalDate date, LocalTime startTime, Instant expiresAt, long remainingSeconds, Long reservationId) {
    public boolean isActive() { return "ACTIVE".equals(status); }
}
