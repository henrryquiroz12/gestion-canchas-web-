package com.canchas.web.controller;

import com.canchas.web.dto.AvailableSlotDTO;
import com.canchas.web.dto.ReservationRequestDTO;
import com.canchas.web.exception.OperationNotAllowedException;
import com.canchas.web.model.Court;
import com.canchas.web.model.Reservation;
import com.canchas.web.model.User;
import com.canchas.web.service.AvailabilityService;
import com.canchas.web.service.CourtService;
import com.canchas.web.service.ReservationService;
import com.canchas.web.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/cliente/reservas")
@RequiredArgsConstructor
public class ClientReservationController {

    private final CourtService courtService;
    private final AvailabilityService availabilityService;
    private final UserService userService;
    private final ReservationService reservationService;

    @GetMapping("/nueva")
    public String showConfirmationForm(
            @RequestParam Long canchaId,
            @RequestParam LocalDate fecha,
            @RequestParam LocalTime horaInicio,
            Model model,
            RedirectAttributes redirectAttributes) {

        Court court = courtService.findById(canchaId);
        if (court == null || !court.isAvailable()) {
            redirectAttributes.addFlashAttribute("error", "La cancha no existe o no está disponible.");
            return "redirect:/canchas";
        }

        // Consultar disponibilidad y validar slot
        List<AvailableSlotDTO> slots = availabilityService.getAvailability(canchaId, fecha);
        AvailableSlotDTO validatedSlot = slots.stream()
                .filter(s -> s.getStartTime().equals(horaInicio))
                .findFirst()
                .orElse(null);

        if (validatedSlot == null || validatedSlot.getStatus() != AvailableSlotDTO.SlotStatus.DISPONIBLE) {
            redirectAttributes.addFlashAttribute("error", "Este horario acaba de dejar de estar disponible. Selecciona otro horario.");
            return "redirect:/canchas/" + canchaId;
        }

        ReservationRequestDTO requestDTO = new ReservationRequestDTO();
        requestDTO.setCourtId(canchaId);
        requestDTO.setReservationDate(fecha);
        requestDTO.setStartTime(horaInicio);

        model.addAttribute("cancha", court);
        model.addAttribute("court", court);
        model.addAttribute("slot", validatedSlot);
        model.addAttribute("alquilerRequest", requestDTO);
        model.addAttribute("reservationRequest", requestDTO);
        model.addAttribute("metodosPago", Reservation.PaymentMethod.values());
        model.addAttribute("paymentMethods", Reservation.PaymentMethod.values());
        model.addAttribute("activeSection", "nuevaReserva");

        return "cliente/reservas/confirmar";
    }

    @PostMapping
    public String createReservation(
            @Valid @ModelAttribute("alquilerRequest") ReservationRequestDTO requestDTO,
            BindingResult result,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Verifica los datos enviados.");
            return "redirect:/canchas/" + requestDTO.getCourtId();
        }

        User client = userService.findByUsername(authentication.getName());
        requestDTO.setClientId(client.getId());

        try {
            Reservation saved = reservationService.create(requestDTO, null);
            return "redirect:/cliente/reservas/" + saved.getId() + "?creada=true";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/cliente/reservas/nueva?canchaId=" + requestDTO.getCourtId() +
                    "&fecha=" + requestDTO.getReservationDate() + "&horaInicio=" + requestDTO.getStartTime();
        }
    }

    @GetMapping("/{id}")
    public String viewReservation(@PathVariable Long id, Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
        Reservation reservation;
        try {
            reservation = reservationService.findById(id);
        } catch (Exception e) {
            return "redirect:/cliente/reservas";
        }

        User client = userService.findByUsername(authentication.getName());
        if (!reservation.getClient().getId().equals(client.getId())) {
            throw new OperationNotAllowedException("No tienes permiso para ver esta reserva.");
        }

        model.addAttribute("alquiler", reservation);
        model.addAttribute("reservation", reservation);

        LocalDateTime reservationStart = LocalDateTime.of(reservation.getReservationDate(), reservation.getStartTime());
        LocalDateTime now = LocalDateTime.now(ZoneId.of("America/Lima"));
        boolean cancellable = reservationStart.isAfter(now) &&
                (reservation.getStatus() == Reservation.ReservationStatus.PENDIENTE ||
                        reservation.getStatus() == Reservation.ReservationStatus.CONFIRMADO);
        model.addAttribute("esCancelable", cancellable);
        model.addAttribute("cancellable", cancellable);
        model.addAttribute("activeSection", "reservas");

        return "cliente/reservas/detalle";
    }

    @GetMapping
    public String listReservations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String estado,
            Model model,
            Authentication authentication) {
        User client = userService.findByUsername(authentication.getName());
        List<Reservation> myReservations = reservationService.findByClientId(client.getId());

        // Filtro básico
        if (estado != null && !estado.isBlank()) {
            try {
                Reservation.ReservationStatus statusEnum = Reservation.ReservationStatus.valueOf(estado.toUpperCase());
                myReservations = myReservations.stream()
                        .filter(a -> a.getStatus() == statusEnum)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException ignored) {
            }
        }

        // Orden: futuras primero, próximas antes. Luego historial.
        LocalDateTime now = LocalDateTime.now(ZoneId.of("America/Lima"));

        myReservations.sort((a, b) -> {
            LocalDateTime dateA = LocalDateTime.of(a.getReservationDate(), a.getStartTime());
            LocalDateTime dateB = LocalDateTime.of(b.getReservationDate(), b.getStartTime());

            boolean isFutureA = dateA.isAfter(now) && (a.getStatus() == Reservation.ReservationStatus.PENDIENTE || a.getStatus() == Reservation.ReservationStatus.CONFIRMADO);
            boolean isFutureB = dateB.isAfter(now) && (b.getStatus() == Reservation.ReservationStatus.PENDIENTE || b.getStatus() == Reservation.ReservationStatus.CONFIRMADO);

            if (isFutureA && !isFutureB) return -1;
            if (!isFutureA && isFutureB) return 1;

            if (isFutureA && isFutureB) {
                return dateA.compareTo(dateB); // Las más próximas primero (ascendente)
            } else {
                return dateB.compareTo(dateA); // Historial más reciente primero (descendente)
            }
        });

        int pageNumber = Math.max(0, page);
        int pageSize = size > 0 ? size : 15;
        int fromIndex = Math.min(pageNumber * pageSize, myReservations.size());
        int toIndex = Math.min(fromIndex + pageSize, myReservations.size());
        List<Reservation> pagedContent = myReservations.subList(fromIndex, toIndex);
        org.springframework.data.domain.Page<Reservation> clientReservationsPage = new org.springframework.data.domain.PageImpl<>(
                pagedContent,
                org.springframework.data.domain.PageRequest.of(pageNumber, pageSize),
                myReservations.size()
        );

        model.addAttribute("clientReservationsPage", clientReservationsPage);
        model.addAttribute("estadoFiltro", estado);
        model.addAttribute("misAlquileres", clientReservationsPage.getContent());
        model.addAttribute("myReservations", clientReservationsPage.getContent());
        model.addAttribute("activeSection", "reservas");
        return "cliente/reservas/lista";
    }

    @PostMapping("/{id}/cancelar")
    public String cancelReservation(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        Reservation reservation;
        try {
            reservation = reservationService.findById(id);
        } catch (Exception e) {
            return "redirect:/cliente/reservas";
        }

        User client = userService.findByUsername(authentication.getName());
        if (!reservation.getClient().getId().equals(client.getId())) {
            throw new OperationNotAllowedException("No tienes permiso para cancelar esta reserva.");
        }

        LocalDateTime reservationStart = LocalDateTime.of(reservation.getReservationDate(), reservation.getStartTime());
        LocalDateTime now = LocalDateTime.now(ZoneId.of("America/Lima"));

        if (!reservationStart.isAfter(now)) {
            redirectAttributes.addFlashAttribute("error", "No puedes cancelar una reserva cuyo horario ya pasó.");
            return "redirect:/cliente/reservas/" + id;
        }

        if (reservation.getStatus() == Reservation.ReservationStatus.CANCELADO || reservation.getStatus() == Reservation.ReservationStatus.COMPLETADO) {
            redirectAttributes.addFlashAttribute("error", "Esta reserva no puede ser cancelada en su estado actual.");
            return "redirect:/cliente/reservas/" + id;
        }

        try {
            reservationService.changeStatus(id, Reservation.ReservationStatus.CANCELADO);
            redirectAttributes.addFlashAttribute("exito", "Reserva cancelada correctamente.");
            return "redirect:/cliente/reservas/" + id + "?cancelada=true";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/cliente/reservas/" + id;
        }
    }
}
