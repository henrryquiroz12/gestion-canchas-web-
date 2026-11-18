package com.canchas.web.service;

import com.canchas.web.dto.DashboardStatsDTO;
import com.canchas.web.dto.ReservationRequestDTO;
import com.canchas.web.model.Reservation;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;

public interface ReservationService {
    List<com.canchas.web.dto.ReservationSearchResultDTO> searchByClient(String criterion, String value);

    Reservation create(ReservationRequestDTO dto, Long clientIdIfAdmin);
    Reservation changeStatus(Long id, Reservation.ReservationStatus newStatus);
    Reservation findById(Long id);
    List<Reservation> findByClientId(Long clientId);
    Page<Reservation> findByClientIdPaginated(Long clientId, int page, int size);
    List<Reservation> findByDate(LocalDate date);
    List<Reservation> findAll();
    Page<Reservation> findAllPaginated(int page, int size);
    DashboardStatsDTO getDashboardStats();
}
