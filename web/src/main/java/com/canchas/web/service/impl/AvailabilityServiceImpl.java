package com.canchas.web.service.impl;

import com.canchas.web.dto.AvailableSlotDTO;
import com.canchas.web.dto.EffectiveOperatingHoursDTO;
import com.canchas.web.model.Court;
import com.canchas.web.model.CourtBlock;
import com.canchas.web.model.Reservation;
import com.canchas.web.repository.ReservationRepository;
import com.canchas.web.service.AvailabilityService;
import com.canchas.web.service.BusinessConfigurationService;
import com.canchas.web.service.CourtBlockService;
import com.canchas.web.service.CourtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service("availabilityService")
@RequiredArgsConstructor
public class AvailabilityServiceImpl implements AvailabilityService {

    private final CourtService courtService;
    private final BusinessConfigurationService businessConfigurationService;
    private final CourtBlockService courtBlockService;
    private final ReservationRepository reservationRepository;
    private final Clock clock;
    private final com.canchas.web.repository.ReservationHoldRepository holdRepository;

    @Override
    public List<AvailableSlotDTO> getAvailability(Long courtId, LocalDate date) {
        Court court = courtService.findById(courtId);

        List<AvailableSlotDTO> slots = new ArrayList<>();

        if (!court.isAvailable()) {
            return slots;
        }

        EffectiveOperatingHoursDTO hours = businessConfigurationService.getEffectiveOperatingHours(date);
        if (hours.isClosed()) {
            return slots;
        }

        List<CourtBlock> blocks = courtBlockService.findActiveByCourtAndDate(courtId, date);
        List<Reservation> reservations = reservationRepository.findByCourtIdAndReservationDateAndStatusIn(
                courtId, date, List.of(Reservation.ReservationStatus.PENDIENTE, Reservation.ReservationStatus.CONFIRMADO));

        var holds = holdRepository.findActive(courtId, date, LocalDateTime.now(clock));

        LocalDateTime current = hours.getStartOperation();
        LocalDateTime endOperation = hours.getEndOperation();
        LocalDateTime now = LocalDateTime.now(clock);

        while (current.plusMinutes(60).compareTo(endOperation) <= 0) {
            LocalTime startTime = current.toLocalTime();
            LocalTime endTime = current.plusMinutes(60).toLocalTime();

            AvailableSlotDTO slot = new AvailableSlotDTO();
            slot.setStartTime(startTime);
            slot.setEndTime(endTime);

            BigDecimal basePrice = court.getHourlyRate();
            BigDecimal surcharge = businessConfigurationService.getLightingSurcharge(startTime);
            slot.setBasePrice(basePrice);
            slot.setLightingSurcharge(surcharge);
            slot.setTotalPrice(basePrice.add(surcharge));

            if (date.isBefore(LocalDate.now(clock)) || (date.isEqual(LocalDate.now(clock)) && current.isBefore(now))) {
                slot.setStatus(AvailableSlotDTO.SlotStatus.BLOQUEADO);
            } else if (isBlocked(startTime, blocks)) {
                slot.setStatus(AvailableSlotDTO.SlotStatus.BLOQUEADO);
            } else if (isOccupied(startTime, reservations) || holds.stream().anyMatch(h -> h.getStartTime().equals(startTime))) {
                slot.setStatus(AvailableSlotDTO.SlotStatus.OCUPADO);
            } else {
                slot.setStatus(AvailableSlotDTO.SlotStatus.DISPONIBLE);
            }

            slots.add(slot);
            current = current.plusMinutes(60);
        }

        return slots;
    }

    private boolean isBlocked(LocalTime startTime, List<CourtBlock> blocks) {
        return blocks.stream().anyMatch(b -> b.getStartTime().equals(startTime));
    }

    private boolean isOccupied(LocalTime startTime, List<Reservation> reservations) {
        return reservations.stream().anyMatch(r -> r.getStartTime().equals(startTime));
    }
}
