package com.canchas.web.controller;

import com.canchas.web.dto.ReservationRequestDTO;
import com.canchas.web.model.Reservation;
import com.canchas.web.security.VisitorHoldSession;
import com.canchas.web.service.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.*;
import java.util.Map;

@Controller @RequestMapping("/retenciones") @RequiredArgsConstructor
public class ReservationHoldController {
    private final ReservationHoldService holds;
    private final CourtService courts;
    private final UserService users;

    private boolean loggedIn(Authentication auth) {
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }

    @PostMapping
    public String start(@RequestParam Long courtId, @RequestParam LocalDate date, @RequestParam LocalTime startTime,
                        HttpSession session, Authentication auth, RedirectAttributes flash) {
        try {
            holds.start(VisitorHoldSession.owner(session), courtId, date, startTime);
            session.setAttribute(VisitorHoldSession.FLOW, true);
            return loggedIn(auth) ? "redirect:/retenciones/continuar" : "redirect:/registro";
        } catch (RuntimeException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
            return "redirect:/canchas/" + courtId;
        }
    }

    @GetMapping @ResponseBody
    public ResponseEntity<?> current(HttpServletRequest request) {
        var hold = holds.current(VisitorHoldSession.existingOwner(request.getSession(false)));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(hold == null ? Map.of("status", "NONE") : hold);
    }

    @PostMapping("/cancelar") @ResponseBody
    public ResponseEntity<?> cancel(@RequestParam String token, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        try {
            holds.cancel(VisitorHoldSession.existingOwner(session), token);
            if (session != null) session.removeAttribute(VisitorHoldSession.FLOW);
            return ResponseEntity.ok(Map.of("status", "CANCELLED"));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/continuar")
    public String resume(HttpServletRequest request, Authentication auth, Model model) {
        if (!loggedIn(auth)) return "redirect:/login";
        String owner = VisitorHoldSession.existingOwner(request.getSession(false));
        var hold = holds.current(owner);
        model.addAttribute("visitorHold", hold);
        if (hold != null && "CONFIRMED".equals(hold.status())) {
            // Confirm endpoint verifies ownership before returning a completed reservation.
            return "redirect:/cliente/reservas/" + hold.reservationId();
        }
        if (hold == null || !hold.isActive()) return "retencion-vencida";
        try {
            var slot = holds.prepare(owner);
            ReservationRequestDTO dto = new ReservationRequestDTO();
            dto.setCourtId(hold.courtId());
            dto.setReservationDate(hold.date());
            dto.setStartTime(hold.startTime());
            var court = courts.findById(hold.courtId());
            model.addAttribute("cancha", court);
            model.addAttribute("court", court);
            model.addAttribute("slot", slot);
            model.addAttribute("alquilerRequest", dto);
            model.addAttribute("reservationRequest", dto);
            model.addAttribute("metodosPago", Reservation.PaymentMethod.values());
            model.addAttribute("paymentMethods", Reservation.PaymentMethod.values());
            model.addAttribute("holdConfirmation", true);
            return "cliente/reservas/confirmar";
        } catch (RuntimeException ex) {
            model.addAttribute("holdError", ex.getMessage());
            return "retencion-vencida";
        }
    }

    @PostMapping("/confirmar") @PreAuthorize("hasRole('CLIENTE')")
    public String confirm(@RequestParam String token, @RequestParam Reservation.PaymentMethod metodoPago,
                          Authentication auth, HttpServletRequest request, RedirectAttributes flash) {
        HttpSession session = request.getSession(false);
        try {
            var client = users.findByUsername(auth.getName());
            var saved = holds.confirm(VisitorHoldSession.existingOwner(session), token, client.getId(), metodoPago);
            session.removeAttribute(VisitorHoldSession.FLOW);
            return "redirect:/cliente/reservas/" + saved.getId() + "?creada=true";
        } catch (RuntimeException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
            return "redirect:/retenciones/continuar";
        }
    }
}
