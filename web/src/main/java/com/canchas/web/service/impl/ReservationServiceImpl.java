package com.canchas.web.service.impl;

import com.canchas.web.dto.DashboardStatsDTO;
import com.canchas.web.dto.EffectiveOperatingHoursDTO;
import com.canchas.web.dto.ReservationRequestDTO;
import com.canchas.web.exception.*;
import com.canchas.web.model.*;
import com.canchas.web.repository.CourtBlockRepository;
import com.canchas.web.repository.CourtRepository;
import com.canchas.web.repository.ReservationRepository;
import com.canchas.web.service.BusinessConfigurationService;
import com.canchas.web.service.ReservationService;
import com.canchas.web.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service("reservationService")
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final CourtRepository courtRepository;
    private final com.canchas.web.service.ReservationSlotRules slotRules;
    private final UserService userService;
    private final BusinessConfigurationService businessConfigurationService;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<com.canchas.web.dto.ReservationSearchResultDTO> searchByClient(String criterion, String value) {
        var search = com.canchas.web.validation.ClientSearchCriteria.parse(criterion, value);
        if (search.value().isEmpty()) return List.of();
        var limit = PageRequest.of(0, 10);
        List<Reservation> matches;
        if ("NOMBRE".equals(search.criterion())) {
            matches = reservationRepository.searchByClientName(search.value(), limit);
        } else if (search.value().length() == 8) {
            matches = reservationRepository.searchByClientDni(search.value(), limit);
        } else {
            matches = reservationRepository.searchByClientDniPrefix(search.value(), limit);
        }
        return matches.stream().map(com.canchas.web.dto.ReservationSearchResultDTO::from).toList();
    }

    @Override
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public Reservation create(ReservationRequestDTO dto, Long clientIdIfAdmin) {
        Long clientId = clientIdIfAdmin != null ? clientIdIfAdmin : dto.getClientId();
        User client = userService.findById(clientId);
        if (client.getRole() != User.Role.CLIENTE) {
            throw new OperationNotAllowedException("El usuario seleccionado no tiene rol CLIENTE");
        }

        Court court = courtRepository.findByIdForUpdate(dto.getCourtId())
                .orElseThrow(() -> new ResourceNotFoundException("Cancha no encontrada"));

        slotRules.validate(court, dto.getReservationDate(), dto.getStartTime(), null);

        BigDecimal basePrice = court.getHourlyRate();
        BigDecimal lightingSurcharge = businessConfigurationService.getLightingSurcharge(dto.getStartTime());

        Reservation reservation = new Reservation();
        reservation.setClient(client);
        reservation.setCourt(court);
        reservation.setReservationDate(dto.getReservationDate());
        reservation.setStartTime(dto.getStartTime());
        reservation.setEndTime(dto.getStartTime().plusHours(1));
        reservation.setPaymentMethod(dto.getPaymentMethod());
        reservation.setStatus(Reservation.ReservationStatus.PENDIENTE);
        reservation.setAppliedBasePrice(basePrice);
        reservation.setAppliedLightingSurcharge(lightingSurcharge);
        reservation.setTotalAmount(basePrice.add(lightingSurcharge));

        return reservationRepository.save(reservation);
    }

    @Override
    @Transactional
    public Reservation changeStatus(Long id, Reservation.ReservationStatus newStatus) {
        Reservation reservation = findById(id);

        if (reservation.getStatus() == Reservation.ReservationStatus.CANCELADO && newStatus != Reservation.ReservationStatus.CANCELADO) {
            throw new InvalidReservationStatusException("No se puede cambiar el estado de un alquiler cancelado.");
        } else if (reservation.getStatus() == Reservation.ReservationStatus.COMPLETADO && newStatus != Reservation.ReservationStatus.COMPLETADO) {
            throw new InvalidReservationStatusException("No se puede cambiar el estado de un alquiler completado.");
        }

        reservation.setStatus(newStatus);
        return reservationRepository.save(reservation);
    }

    @Override
    public Reservation findById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alquiler no encontrado con id: " + id));
    }

    @Override
    public List<Reservation> findByClientId(Long clientId) {
        User client = userService.findById(clientId);
        return reservationRepository.findByClient(client);
    }

    @Override
    public Page<Reservation> findByClientIdPaginated(Long clientId, int page, int size) {
        int pageNumber = Math.max(0, page);
        int pageSize = size > 0 ? size : 15;
        return reservationRepository.findByClientIdPaginated(clientId, PageRequest.of(pageNumber, pageSize));
    }

    @Override
    public List<Reservation> findByDate(LocalDate date) {
        return reservationRepository.findByReservationDate(date);
    }

    @Override
    public List<Reservation> findAll() {
        return reservationRepository.findAll();
    }

    @Override
    public Page<Reservation> findAllPaginated(int page, int size) {
        int pageNumber = Math.max(0, page);
        int pageSize = size > 0 ? size : 15;
        return reservationRepository.findAllPaginated(PageRequest.of(pageNumber, pageSize));
    }

    @Override
    public DashboardStatsDTO getDashboardStats() {
        DashboardStatsDTO stats = new DashboardStatsDTO();
        List<Reservation> allReservations = reservationRepository.findAll();
        LocalDate today = LocalDate.now(clock);
        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = today.with(DayOfWeek.SUNDAY);

        List<Reservation> weekReservations = allReservations.stream()
                .filter(r -> !r.getReservationDate().isBefore(startOfWeek) && !r.getReservationDate().isAfter(endOfWeek))
                .filter(r -> r.getStatus() != Reservation.ReservationStatus.CANCELADO)
                .collect(Collectors.toList());

        stats.setWeeklyReservations(weekReservations.size());
        stats.setWeeklyRevenue(weekReservations.stream().map(Reservation::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add));

        List<BigDecimal> monthlySales = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            int m = month;
            BigDecimal total = allReservations.stream()
                    .filter(r -> r.getReservationDate().getYear() == today.getYear())
                    .filter(r -> r.getReservationDate().getMonthValue() == m)
                    .filter(r -> r.getStatus() != Reservation.ReservationStatus.CANCELADO)
                    .map(Reservation::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            monthlySales.add(total);
        }
        stats.setMonthlySales(monthlySales);

        Map<String, BigDecimal> salesByCourtType = new LinkedHashMap<>();
        for (Court.CourtType type : Court.CourtType.values()) {
            BigDecimal total = allReservations.stream()
                    .filter(r -> r.getCourt().getType() == type)
                    .filter(r -> r.getStatus() != Reservation.ReservationStatus.CANCELADO)
                    .map(Reservation::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            salesByCourtType.put(type.name(), total);
        }
        stats.setSalesByCourtType(salesByCourtType);
        return stats;
    }
}
