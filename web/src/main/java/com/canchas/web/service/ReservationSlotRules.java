package com.canchas.web.service;

import com.canchas.web.dto.AvailableSlotDTO;
import com.canchas.web.exception.*;
import com.canchas.web.model.*;
import com.canchas.web.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.*;
import java.util.List;

/** Called while holding the court row lock for any new hold or reservation. */
@Component @RequiredArgsConstructor
public class ReservationSlotRules {
    private final BusinessConfigurationService configuration;
    private final ReservationRepository reservations;
    private final CourtBlockRepository blocks;
    private final ReservationHoldRepository holds;
    private final Clock clock;

    public AvailableSlotDTO validate(Court court, LocalDate date, LocalTime time, String ignoredOwner) {
        if (!court.isAvailable()) throw new ResourceNotFoundException("La cancha seleccionada no está disponible");
        if (date == null || time == null) throw new InvalidDateException("Selecciona una fecha y hora válidas.");
        LocalDateTime start = LocalDateTime.of(date, time);
        if (start.isBefore(LocalDateTime.now(clock))) throw new InvalidDateException("No se puede reservar un horario que ya pasó");
        var hours = configuration.getEffectiveOperatingHours(date);
        if (hours.isClosed()) throw new InvalidDateException("El establecimiento está cerrado en la fecha seleccionada.");
        long seconds = Duration.between(hours.getOperationStart(), start).getSeconds();
        if (start.isBefore(hours.getOperationStart()) || start.plusHours(1).isAfter(hours.getOperationEnd())
                || seconds % 3600 != 0 || time.getNano() != 0) {
            throw new InvalidDateException("El horario seleccionado está fuera de los turnos del horario operativo.");
        }
        if (reservations.existsByCourtIdAndReservationDateAndStartTimeAndStatusIn(court.getId(), date, time,
                List.of(Reservation.ReservationStatus.PENDIENTE, Reservation.ReservationStatus.CONFIRMADO))) {
            throw new TimeSlotUnavailableException("Ya existe una reserva para esa cancha en el horario seleccionado");
        }
        if (blocks.findByCourtIdAndDateAndStartTime(court.getId(), date, time).filter(CourtBlock::isActive).isPresent()) {
            throw new TimeSlotUnavailableException("El slot seleccionado se encuentra bloqueado.");
        }
        if (holds.findActive(court.getId(), date, LocalDateTime.now(clock)).stream()
                .anyMatch(h -> h.getStartTime().equals(time) && !h.getOwnerKey().equals(ignoredOwner))) {
            throw new TimeSlotUnavailableException("Otra persona está completando su registro o reserva para este horario.");
        }
        AvailableSlotDTO slot = new AvailableSlotDTO();
        slot.setStartTime(time);
        slot.setEndTime(time.plusHours(1));
        slot.setStatus(AvailableSlotDTO.SlotStatus.DISPONIBLE);
        slot.setBasePrice(court.getHourlyRate());
        slot.setLightingSurcharge(configuration.getLightingSurcharge(time));
        slot.setTotalPrice(slot.getBasePrice().add(slot.getLightingSurcharge()));
        return slot;
    }
}
