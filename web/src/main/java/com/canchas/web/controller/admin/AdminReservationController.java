package com.canchas.web.controller.admin;

import com.canchas.web.dto.ReservationRequestDTO;
import com.canchas.web.model.Court;
import com.canchas.web.model.Reservation;
import com.canchas.web.model.User;
import com.canchas.web.service.CourtService;
import com.canchas.web.service.ReservationService;
import com.canchas.web.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/reservas")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReservationController {

    private final ReservationService reservationService;
    private final UserService userService;
    private final CourtService courtService;

    @GetMapping
    public String listReservations(Model model) {
        model.addAttribute("activeSection", "reservas");
        return "admin/reservas/lista";
    }

    @GetMapping("/buscar")
    @ResponseBody
    public ResponseEntity<?> searchByClient(@RequestParam String criterio, @RequestParam String valor) {
        try {
            return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore())
                    .body(reservationService.searchByClient(criterio, valor));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("valor", ex.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public String reservationDetail(@PathVariable Long id, Model model) {
        Reservation reservation = reservationService.findById(id);
        model.addAttribute("alquiler", reservation);
        model.addAttribute("reservation", reservation);
        model.addAttribute("activeSection", "reservas");
        return "admin/reservas/detalle";
    }

    @PostMapping("/{id}/estado")
    public String changeStatus(@PathVariable Long id, @RequestParam Reservation.ReservationStatus estado, RedirectAttributes ra) {
        try {
            reservationService.changeStatus(id, estado);
            ra.addFlashAttribute("mensajeExito", "Estado de la reserva actualizado a " + estado + ".");
        } catch (Exception e) {
            ra.addFlashAttribute("mensajeError", "Error al actualizar estado: " + e.getMessage());
        }
        return "redirect:/admin/reservas/" + id;
    }

    @GetMapping("/nueva")
    public String newReservation(Model model) {
        List<Court> courts = courtService.findAvailable();
        model.addAttribute("canchas", courts);
        model.addAttribute("courts", courts);
        model.addAttribute("activeSection", "reservas");
        return "admin/reservas/nueva";
    }

    @PostMapping
    @ResponseBody
    public ResponseEntity<Map<String, String>> createReservation(@Valid @ModelAttribute ReservationRequestDTO dto, BindingResult result) {
        if (result.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            result.getFieldErrors().forEach(fe -> errors.put(fe.getField(), fe.getDefaultMessage()));
            return ResponseEntity.badRequest().body(errors);
        }
        try {
            reservationService.create(dto, dto.getClientId());
            return ResponseEntity.ok(Map.of("mensaje", "Reserva creada correctamente"));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("global", ex.getMessage()));
        }
    }
}
