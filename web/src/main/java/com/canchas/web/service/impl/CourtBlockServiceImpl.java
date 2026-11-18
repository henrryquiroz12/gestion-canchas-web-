package com.canchas.web.service.impl;

import com.canchas.web.dto.CourtBlockGroupDTO;
import com.canchas.web.dto.EffectiveOperatingHoursDTO;
import com.canchas.web.exception.InvalidDateException;
import com.canchas.web.exception.OperationNotAllowedException;
import com.canchas.web.model.Court;
import com.canchas.web.model.CourtBlock;
import com.canchas.web.model.Reservation;
import com.canchas.web.repository.CourtBlockRepository;
import com.canchas.web.repository.ReservationRepository;
import com.canchas.web.service.BusinessConfigurationService;
import com.canchas.web.service.CourtBlockService;
import com.canchas.web.service.CourtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service("courtBlockService")
public class CourtBlockServiceImpl implements CourtBlockService {

    private final CourtBlockRepository courtBlockRepository;
    private final ReservationRepository reservationRepository;
    private final CourtService courtService;
    private final BusinessConfigurationService businessConfigurationService;
    private final Clock clock;
    private final com.canchas.web.repository.ReservationHoldRepository holdRepository;

    @Autowired
    public CourtBlockServiceImpl(CourtBlockRepository courtBlockRepository,
                                 ReservationRepository reservationRepository,
                                 CourtService courtService,
                                 BusinessConfigurationService businessConfigurationService,
                                 Clock clock, com.canchas.web.repository.ReservationHoldRepository holdRepository) {
        this.courtBlockRepository = courtBlockRepository;
        this.reservationRepository = reservationRepository;
        this.courtService = courtService;
        this.businessConfigurationService = businessConfigurationService;
        this.clock = clock;
        this.holdRepository = holdRepository;
    }

    private void rejectHeldSlots(Long courtId, LocalDate date, List<LocalTime> slots) {
        if (holdRepository.findActive(courtId, date, LocalDateTime.now(clock)).stream()
                .anyMatch(h -> slots.contains(h.getStartTime()))) {
            throw new OperationNotAllowedException("El horario tiene una retenci\u00f3n activa. Espera su vencimiento o cancelaci\u00f3n.");
        }
    }

    @Override
    public List<CourtBlock> findActiveByCourtAndDate(Long courtId, LocalDate date) {
        return courtBlockRepository.findByCourtIdAndDateAndActiveTrue(courtId, date);
    }

    @Override
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public CourtBlock blockSlot(Long courtId, LocalDate date, LocalTime startTime, String reason) {
        if (reason == null || reason.trim().isBlank()) {
            throw new IllegalArgumentException("El motivo del bloqueo es obligatorio.");
        }
        Court court = courtService.lockById(courtId);

        EffectiveOperatingHoursDTO effectiveHours = businessConfigurationService.getEffectiveOperatingHours(date);
        if (effectiveHours.isClosed()) {
            throw new InvalidDateException("No se puede bloquear un slot en un día cerrado.");
        }

        LocalDate today = LocalDate.now(clock);
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime slotStart = LocalDateTime.of(date, startTime);

        if (date.isBefore(today) || (date.isEqual(today) && slotStart.isBefore(now))) {
            throw new InvalidDateException("No se puede bloquear un slot que ya inició o transcurrió (" + startTime + ").");
        }

        if (slotStart.isBefore(effectiveHours.getOperationStart()) || slotStart.plusHours(1).isAfter(effectiveHours.getOperationEnd())
                || java.time.Duration.between(effectiveHours.getOperationStart(), slotStart).getSeconds() % 3600 != 0
                || startTime.getNano() != 0) {
            throw new InvalidDateException("El slot está fuera del horario operativo.");
        }

        rejectHeldSlots(courtId, date, List.of(startTime));

        boolean existsActiveReservation = reservationRepository.existsByCourtIdAndReservationDateAndStartTimeAndStatusIn(
                courtId, date, startTime, List.of(Reservation.ReservationStatus.PENDIENTE, Reservation.ReservationStatus.CONFIRMADO));
        if (existsActiveReservation) {
            throw new OperationNotAllowedException("No se puede bloquear un slot que tiene una reserva activa.");
        }

        Optional<CourtBlock> existingOpt = courtBlockRepository.findByCourtIdAndDateAndStartTime(courtId, date, startTime);

        if (existingOpt.isPresent()) {
            CourtBlock existing = existingOpt.get();
            if (existing.isActive()) {
                throw new OperationNotAllowedException("El slot ya se encuentra bloqueado.");
            }
            existing.setActive(true);
            existing.setReason(reason);
            return courtBlockRepository.save(existing);
        }

        CourtBlock newBlock = new CourtBlock();
        newBlock.setCourt(court);
        newBlock.setDate(date);
        newBlock.setStartTime(startTime);
        newBlock.setReason(reason);
        newBlock.setActive(true);
        return courtBlockRepository.save(newBlock);
    }

    @Override
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public List<CourtBlock> blockRange(Long courtId, LocalDate date, LocalTime startTime, LocalTime endTime, String reason) {
        if (reason == null || reason.trim().isBlank()) {
            throw new IllegalArgumentException("El motivo del bloqueo es obligatorio.");
        }
        Court court = courtService.lockById(courtId);

        EffectiveOperatingHoursDTO effectiveHours = businessConfigurationService.getEffectiveOperatingHours(date);
        if (effectiveHours.isClosed()) {
            throw new InvalidDateException("No se puede bloquear en un día cerrado.");
        }

        boolean untilMidnight = endTime.equals(LocalTime.MIDNIGHT);
        if (!untilMidnight && !endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("La hora final del rango debe ser posterior a la hora de inicio.");
        }

        LocalDate today = LocalDate.now(clock);
        LocalDateTime now = LocalDateTime.now(clock);

        LocalDateTime rangeStart = LocalDateTime.of(date, startTime);
        LocalDateTime rangeEnd = LocalDateTime.of(untilMidnight ? date.plusDays(1) : date, endTime);
        if (rangeStart.isBefore(effectiveHours.getOperationStart()) || rangeEnd.isAfter(effectiveHours.getOperationEnd())
                || java.time.Duration.between(effectiveHours.getOperationStart(), rangeStart).getSeconds() % 3600 != 0
                || java.time.Duration.between(rangeStart, rangeEnd).getSeconds() % 3600 != 0
                || startTime.getNano() != 0 || endTime.getNano() != 0) {
            throw new InvalidDateException("Selecciona un rango de turnos completos dentro del horario operativo.");
        }

        List<LocalTime> requestedSlots = new ArrayList<>();
        LocalTime current = startTime;
        while (!current.equals(endTime)) {
            requestedSlots.add(current);
            current = current.plusHours(1);
            if (untilMidnight && current.equals(LocalTime.MIDNIGHT)) {
                break;
            }
        }

        if (requestedSlots.isEmpty()) {
            throw new IllegalArgumentException("El rango de horas seleccionado no genera ningún slot.");
        }

        for (LocalTime slot : requestedSlots) {
            LocalDateTime slotDateTime = LocalDateTime.of(date, slot);
            if (date.isBefore(today) || (date.isEqual(today) && slotDateTime.isBefore(now))) {
                throw new InvalidDateException("El rango incluye slots pasados o en curso (" + slot + "). No se puede bloquear.");
            }
            if (slotDateTime.isBefore(effectiveHours.getOperationStart()) || !slotDateTime.isBefore(effectiveHours.getOperationEnd())) {
                throw new InvalidDateException("El slot " + slot + " está fuera del horario operativo efectivo de hoy.");
            }
        }

        rejectHeldSlots(courtId, date, requestedSlots);

        List<Reservation> activeReservations = reservationRepository.findByCourtIdAndReservationDateAndStatusIn(
                courtId, date, List.of(Reservation.ReservationStatus.PENDIENTE, Reservation.ReservationStatus.CONFIRMADO));

        List<Reservation> conflicting = activeReservations.stream()
                .filter(res -> requestedSlots.contains(res.getStartTime()))
                .collect(Collectors.toList());

        if (!conflicting.isEmpty()) {
            String conflictDetails = conflicting.stream()
                    .map(r -> (r.getCourt() != null ? r.getCourt().getName() : "Cancha") + " a las " + r.getStartTime() + " (Cliente: " + (r.getClient() != null ? r.getClient().getFullName() : "N/A") + ")")
                    .collect(Collectors.joining(", "));
            throw new OperationNotAllowedException("No se puede bloquear este periodo porque existen reservas activas: " + conflictDetails);
        }

        for (LocalTime slot : requestedSlots) {
            Optional<CourtBlock> blockOpt = courtBlockRepository.findByCourtIdAndDateAndStartTime(courtId, date, slot);
            if (blockOpt.isPresent() && blockOpt.get().isActive()) {
                throw new OperationNotAllowedException("Parte del rango solicitado ya se encuentra bloqueado (slot: " + slot + "). Operación abortada.");
            }
        }

        List<CourtBlock> createdBlocks = new ArrayList<>();
        for (LocalTime slot : requestedSlots) {
            Optional<CourtBlock> existingOpt = courtBlockRepository.findByCourtIdAndDateAndStartTime(courtId, date, slot);
            CourtBlock block;
            if (existingOpt.isPresent()) {
                block = existingOpt.get();
                block.setActive(true);
                block.setReason(reason);
            } else {
                block = new CourtBlock();
                block.setCourt(court);
                block.setDate(date);
                block.setStartTime(slot);
                block.setReason(reason);
                block.setActive(true);
            }
            createdBlocks.add(courtBlockRepository.save(block));
        }

        return createdBlocks;
    }

    @Override
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public List<CourtBlock> blockFullDay(Long courtId, LocalDate date, String reason) {
        if (reason == null || reason.trim().isBlank()) {
            throw new IllegalArgumentException("El motivo del bloqueo es obligatorio.");
        }
        EffectiveOperatingHoursDTO effectiveHours = businessConfigurationService.getEffectiveOperatingHours(date);
        if (effectiveHours.isClosed()) {
            throw new InvalidDateException("No se puede bloquear una cancha en un día cerrado.");
        }

        LocalTime openTime = effectiveHours.getOperationStart().toLocalTime();
        LocalTime closeTime = effectiveHours.getOperationEnd().toLocalTime();

        return blockRange(courtId, date, openTime, closeTime, reason);
    }

    @Override
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void unblockSlot(Long courtId, LocalDate date, LocalTime startTime) {
        Optional<CourtBlock> existing = courtBlockRepository.findByCourtIdAndDateAndStartTime(courtId, date, startTime);
        existing.ifPresent(block -> {
            block.setActive(false);
            courtBlockRepository.save(block);
        });
    }

    @Override
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void unblockById(Long id) {
        courtBlockRepository.findById(id).ifPresent(block -> {
            block.setActive(false);
            courtBlockRepository.save(block);
        });
    }

    @Override
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void unblockByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        List<CourtBlock> blocks = courtBlockRepository.findAllById(ids);
        for (CourtBlock b : blocks) {
            b.setActive(false);
        }
        courtBlockRepository.saveAll(blocks);
    }

    @Override
    public List<CourtBlock> findAllActive() {
        return courtBlockRepository.findAll().stream()
                .filter(CourtBlock::isActive)
                .collect(Collectors.toList());
    }

    @Override
    public List<CourtBlockGroupDTO> findGroupedBlocks() {
        List<CourtBlock> activeBlocks = findAllActive();
        if (activeBlocks.isEmpty()) return Collections.emptyList();

        Map<String, List<CourtBlock>> groupedByCourtAndDate = activeBlocks.stream()
                .collect(Collectors.groupingBy(b -> b.getCourt().getId() + "_" + b.getDate()));

        List<CourtBlockGroupDTO> result = new ArrayList<>();

        for (List<CourtBlock> blocks : groupedByCourtAndDate.values()) {
            blocks.sort(Comparator.comparing(CourtBlock::getStartTime));
            Court court = blocks.get(0).getCourt();
            LocalDate date = blocks.get(0).getDate();

            EffectiveOperatingHoursDTO effectiveHours = businessConfigurationService.getEffectiveOperatingHours(date);
            int totalEffectiveDaySlots = 17;
            if (!effectiveHours.isClosed()) {
                long minutes = java.time.Duration.between(effectiveHours.getOperationStart(), effectiveHours.getOperationEnd()).toMinutes();
                totalEffectiveDaySlots = (int) (minutes / 60);
            }

            CourtBlock currentStart = null;
            LocalTime currentEnd = null;
            String currentReason = null;
            List<Long> currentIds = new ArrayList<>();

            for (CourtBlock b : blocks) {
                if (currentStart == null) {
                    currentStart = b;
                    currentEnd = b.getStartTime().plusHours(1);
                    currentReason = b.getReason();
                    currentIds.add(b.getId());
                } else if (b.getStartTime().equals(currentEnd) && Objects.equals(b.getReason(), currentReason)) {
                    currentEnd = b.getStartTime().plusHours(1);
                    currentIds.add(b.getId());
                } else {
                    boolean isFullDay = currentIds.size() >= totalEffectiveDaySlots;
                    result.add(CourtBlockGroupDTO.builder()
                            .courtId(court.getId())
                            .courtName(court.getName())
                            .date(date)
                            .startTime(currentStart.getStartTime())
                            .endTime(currentEnd)
                            .reason(currentReason)
                            .totalSlots(currentIds.size())
                            .isFullDay(isFullDay)
                            .ids(new ArrayList<>(currentIds))
                            .build());

                    currentStart = b;
                    currentEnd = b.getStartTime().plusHours(1);
                    currentReason = b.getReason();
                    currentIds = new ArrayList<>(List.of(b.getId()));
                }
            }

            if (currentStart != null) {
                boolean isFullDay = currentIds.size() >= totalEffectiveDaySlots;
                result.add(CourtBlockGroupDTO.builder()
                        .courtId(court.getId())
                        .courtName(court.getName())
                        .date(date)
                        .startTime(currentStart.getStartTime())
                        .endTime(currentEnd)
                        .reason(currentReason)
                        .totalSlots(currentIds.size())
                        .isFullDay(isFullDay)
                        .ids(new ArrayList<>(currentIds))
                        .build());
            }
        }

        result.sort(Comparator.comparing(CourtBlockGroupDTO::getDate).reversed()
                .thenComparing(CourtBlockGroupDTO::getCourtName)
                .thenComparing(CourtBlockGroupDTO::getStartTime));

        return result;
    }
}
