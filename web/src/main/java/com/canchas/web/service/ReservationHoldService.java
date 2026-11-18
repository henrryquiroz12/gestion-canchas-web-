package com.canchas.web.service;

import com.canchas.web.config.ReservationHoldProperties;
import com.canchas.web.dto.*;
import com.canchas.web.exception.*;
import com.canchas.web.model.*;
import com.canchas.web.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class ReservationHoldService {
    private final ReservationHoldRepository holds;
    private final CourtRepository courts;
    private final ReservationSlotRules rules;
    private final ReservationService reservations;
    private final ReservationHoldProperties properties;
    private final Clock clock;

    private LocalDateTime now() { return LocalDateTime.now(clock); }
    private ReservationHold locked(String owner) {
        if (owner == null) throw new OperationNotAllowedException("No hay una retención asociada a esta sesión.");
        return holds.lockOwner(owner).orElseThrow(() -> new OperationNotAllowedException("No hay una retención asociada a esta sesión."));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReservationHoldView start(String owner, Long courtId, LocalDate date, LocalTime time) {
        holds.initializeOwner(owner);
        ReservationHold hold = locked(owner);
        if (hold.isActive(now())) {
            if (hold.getCourt().getId().equals(courtId) && hold.getDate().equals(date) && hold.getStartTime().equals(time)) {
                return view(hold); // Retry/reload never extends the deadline.
            }
            throw new OperationNotAllowedException("Ya tienes un horario retenido. Continúa con él o cancélalo antes de elegir otro.");
        }
        Court court = courts.findByIdForUpdate(courtId).orElseThrow(() -> new ResourceNotFoundException("Cancha no encontrada"));
        rules.validate(court, date, time, null);
        hold.setToken(UUID.randomUUID().toString());
        hold.setCourt(court);
        hold.setDate(date);
        hold.setStartTime(time);
        LocalDateTime deadline = now().plusMinutes(properties.getMinutes());
        LocalDateTime slotStart = LocalDateTime.of(date, time);
        if (!slotStart.isAfter(now())) throw new InvalidDateException("El horario seleccionado ya empezó. Consulta disponibilidad de nuevo.");
        hold.setExpiresAt(deadline.isBefore(slotStart) ? deadline : slotStart);
        hold.setStatus(ReservationHold.Status.ACTIVE);
        hold.setReservation(null);
        return view(holds.saveAndFlush(hold));
    }

    @Transactional(readOnly = true)
    public ReservationHoldView current(String owner) {
        if (owner == null) return null;
        return holds.findById(owner).filter(h -> h.getCourt() != null).map(this::view).orElse(null);
    }

    private ReservationHoldView view(ReservationHold hold) {
        boolean active = hold.isActive(now());
        String state = hold.getStatus() == ReservationHold.Status.ACTIVE && !active ? "EXPIRED" : hold.getStatus().name();
        return new ReservationHoldView(state, hold.getToken(), hold.getCourt().getId(), hold.getCourt().getName(),
                hold.getDate(), hold.getStartTime(), hold.getExpiresAt().atZone(clock.getZone()).toInstant(),
                active ? Math.max(0, Duration.between(now(), hold.getExpiresAt()).getSeconds()) : 0,
                hold.getReservation() == null ? null : hold.getReservation().getId());
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AvailableSlotDTO prepare(String owner) {
        ReservationHold hold = locked(owner);
        if (!hold.isActive(now())) throw new OperationNotAllowedException("El plazo de la retención terminó. Vuelve a consultar disponibilidad.");
        Court court = courts.findByIdForUpdate(hold.getCourt().getId()).orElseThrow();
        if (!hold.isActive(now())) throw new OperationNotAllowedException("El plazo de la retención terminó. Vuelve a consultar disponibilidad.");
        return rules.validate(court, hold.getDate(), hold.getStartTime(), owner);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Reservation confirm(String owner, String token, Long clientId, Reservation.PaymentMethod payment) {
        ReservationHold hold = locked(owner);
        if (token == null || !token.equals(hold.getToken())) throw new OperationNotAllowedException("Esta retención no pertenece al flujo actual de tu sesión.");
        if (hold.getStatus() == ReservationHold.Status.CONFIRMED) {
            if (!hold.getReservation().getClient().getId().equals(clientId)) throw new OperationNotAllowedException("La reserva pertenece a otra cuenta.");
            return hold.getReservation();
        }
        if (!hold.isActive(now())) throw new OperationNotAllowedException("El plazo de la retención terminó. Vuelve a consultar disponibilidad.");
        if (payment == null) throw new IllegalArgumentException("Selecciona el método de pago.");
        courts.findByIdForUpdate(hold.getCourt().getId()).orElseThrow();
        if (!hold.isActive(now())) throw new OperationNotAllowedException("El plazo de la retención terminó. Vuelve a consultar disponibilidad.");
        // Same transaction and court lock: consuming the hold cannot open a gap to competitors.
        hold.setStatus(ReservationHold.Status.CONFIRMED);
        holds.saveAndFlush(hold);
        ReservationRequestDTO request = new ReservationRequestDTO();
        request.setClientId(clientId);
        request.setCourtId(hold.getCourt().getId());
        request.setReservationDate(hold.getDate());
        request.setStartTime(hold.getStartTime());
        request.setPaymentMethod(payment);
        Reservation saved = reservations.create(request, null);
        hold.setReservation(saved);
        holds.saveAndFlush(hold);
        return saved;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void cancel(String owner, String token) {
        ReservationHold hold = locked(owner);
        if (token == null || !token.equals(hold.getToken())) throw new OperationNotAllowedException("La retención ya no corresponde a esta sesión.");
        if (hold.getStatus() == ReservationHold.Status.CONFIRMED) {
            throw new OperationNotAllowedException("El horario ya tiene una reserva. Revisa su detalle para cancelarla.");
        }
        if (hold.getStatus() == ReservationHold.Status.ACTIVE) {
            courts.findByIdForUpdate(hold.getCourt().getId()).orElseThrow();
            hold.setStatus(ReservationHold.Status.CANCELLED);
            holds.saveAndFlush(hold);
        }
    }
}
