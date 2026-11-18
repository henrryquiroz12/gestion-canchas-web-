package com.canchas.web.service.impl;

import com.canchas.web.dto.EffectiveOperatingHoursDTO;
import com.canchas.web.exception.BusinessConfigurationNotFoundException;
import com.canchas.web.exception.OperationNotAllowedException;
import com.canchas.web.model.BusinessConfiguration;
import com.canchas.web.model.DailyOperatingHours;
import com.canchas.web.model.Reservation;
import com.canchas.web.repository.BusinessConfigurationRepository;
import com.canchas.web.repository.DailyOperatingHoursRepository;
import com.canchas.web.repository.ReservationRepository;
import com.canchas.web.service.BusinessConfigurationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service("businessConfigurationService")
@RequiredArgsConstructor
public class BusinessConfigurationServiceImpl implements BusinessConfigurationService {

    private final BusinessConfigurationRepository businessConfigurationRepository;
    private final DailyOperatingHoursRepository dailyOperatingHoursRepository;
    private final ReservationRepository reservationRepository;
    private final com.canchas.web.repository.CourtRepository courtRepository;
    private final com.canchas.web.repository.ReservationHoldRepository holdRepository;
    private final java.time.Clock clock;

    @Override
    public BusinessConfiguration getConfiguration() {
        return businessConfigurationRepository.findById((short) 1)
                .orElseThrow(() -> new BusinessConfigurationNotFoundException("La configuración inicial del negocio no existe en la base de datos."));
    }

    @Override
    public EffectiveOperatingHoursDTO getEffectiveOperatingHours(LocalDate date) {
        DailyOperatingHours override = dailyOperatingHoursRepository.findByDate(date).orElse(null);

        if (override != null && override.isClosed()) {
            return new EffectiveOperatingHoursDTO(true, null, null);
        }

        LocalTime opening;
        LocalTime closing;

        if (override != null) {
            opening = override.getOpeningTime();
            closing = override.getClosingTime();
        } else {
            BusinessConfiguration config = getConfiguration();
            opening = config.getDefaultOpeningTime();
            closing = config.getDefaultClosingTime();
        }

        LocalDateTime operationStart = LocalDateTime.of(date, opening);
        LocalDateTime operationEnd = LocalDateTime.of(date, closing);

        if (closing.equals(LocalTime.MIDNIGHT)) {
            operationEnd = operationEnd.plusDays(1);
        } else if (closing.isBefore(opening)) {
            throw new OperationNotAllowedException("El horario de cierre no puede ser anterior a la apertura salvo que sea 00:00.");
        }

        return new EffectiveOperatingHoursDTO(false, operationStart, operationEnd);
    }

    @Override
    public BigDecimal calculatePriceWithLighting(BigDecimal basePrice, LocalTime startTime) {
        return basePrice.add(getLightingSurcharge(startTime));
    }

    @Override
    public BigDecimal getLightingSurcharge(LocalTime startTime) {
        BusinessConfiguration config = getConfiguration();
        if (!startTime.isBefore(config.getLightingStartTime())) {
            return config.getLightingSurcharge();
        }
        return BigDecimal.ZERO;
    }

    @Override
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public DailyOperatingHours saveSpecialOperatingHours(LocalDate date, LocalTime openingTime, LocalTime closingTime, boolean closed, String reason) {
        courtRepository.lockAllForScheduleUpdate();
        if (!closed && closingTime.isBefore(openingTime) && !closingTime.equals(LocalTime.MIDNIGHT)) {
            throw new OperationNotAllowedException("Horario especial de cierre inválido.");
        }

        var activeHolds = holdRepository.findActiveOnDate(date, LocalDateTime.now(clock));
        for (var hold : activeHolds) {
            if (closed) throw new OperationNotAllowedException("No se puede cerrar el día mientras existen retenciones activas.");
            var from = LocalDateTime.of(date, openingTime);
            var until = LocalDateTime.of(date, closingTime);
            if (closingTime.equals(LocalTime.MIDNIGHT)) until = until.plusDays(1);
            var start = LocalDateTime.of(date, hold.getStartTime());
            if (start.isBefore(from) || start.plusHours(1).isAfter(until)) {
                throw new OperationNotAllowedException("El nuevo horario deja fuera una retención activa.");
            }
        }
        List<Reservation> activeReservations = reservationRepository.findByReservationDateAndStatusIn(
                date, List.of(Reservation.ReservationStatus.PENDIENTE, Reservation.ReservationStatus.CONFIRMADO));

        if (!activeReservations.isEmpty()) {
            if (closed) {
                throw new OperationNotAllowedException("No se puede cerrar el día porque existen reservas activas.");
            }

            LocalDateTime operationStart = LocalDateTime.of(date, openingTime);
            LocalDateTime operationEnd = LocalDateTime.of(date, closingTime);
            if (closingTime.equals(LocalTime.MIDNIGHT)) {
                operationEnd = operationEnd.plusDays(1);
            }

            for (Reservation r : activeReservations) {
                LocalDateTime rStart = LocalDateTime.of(date, r.getStartTime());
                LocalDateTime rEnd = LocalDateTime.of(date, r.getEndTime());
                if (r.getEndTime().equals(LocalTime.MIDNIGHT) || r.getEndTime().isBefore(r.getStartTime())) {
                    rEnd = rEnd.plusDays(1);
                }

                if (rStart.isBefore(operationStart) || rEnd.isAfter(operationEnd)) {
                    throw new OperationNotAllowedException("No se puede reducir el horario porque deja fuera reservas activas.");
                }
            }
        }

        DailyOperatingHours operatingHours = dailyOperatingHoursRepository.findByDate(date).orElse(new DailyOperatingHours());
        operatingHours.setDate(date);
        operatingHours.setOpeningTime(openingTime);
        operatingHours.setClosingTime(closingTime);
        operatingHours.setClosed(closed);
        operatingHours.setReason(reason);
        return dailyOperatingHoursRepository.save(operatingHours);
    }

    @Override
    public Optional<DailyOperatingHours> getDailyOperatingHours(LocalDate date) {
        return dailyOperatingHoursRepository.findByDate(date);
    }
}
