package com.canchas.web.controller.admin;

import com.canchas.web.model.Court;
import com.canchas.web.service.CourtService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/admin/canchas")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCourtController {

    private final CourtService courtService;

    @GetMapping
    public String listCourts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            Model model) {
        Page<Court> courtsPage = courtService.findAllPaginated(page, size);
        model.addAttribute("courtsPage", courtsPage);
        model.addAttribute("canchas", courtsPage.getContent());
        model.addAttribute("courts", courtsPage.getContent());
        model.addAttribute("activeSection", "canchas");
        return "admin/canchas/lista";
    }

    @PostMapping("/{id}/editar")
    public Object editCourt(@PathVariable Long id,
                            @ModelAttribute Court court,
                            @RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
                            RedirectAttributes ra) {
        boolean isAjax = "XMLHttpRequest".equalsIgnoreCase(requestedWith);
        if (court.getHourlyRate() == null || court.getHourlyRate().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            if (isAjax) {
                return ResponseEntity.badRequest().body(Map.of("precioHora", "El precio por hora debe ser mayor a 0."));
            }
            ra.addFlashAttribute("mensajeError", "El precio por hora debe ser mayor a 0.");
            return "redirect:/admin/canchas";
        }
        try {
            Court updated = courtService.update(id, court);
            if (isAjax) {
                return ResponseEntity.ok(Map.of("mensaje", "Cancha " + updated.getName() + " actualizada correctamente."));
            }
            ra.addFlashAttribute("mensajeExito", "Cancha " + updated.getName() + " actualizada correctamente.");
        } catch (RuntimeException ex) {
            if (isAjax) {
                return ResponseEntity.badRequest().body(Map.of("global", ex.getMessage()));
            }
            ra.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/admin/canchas";
    }

    @PostMapping("/{id}/estado")
    public String changeCourtStatus(@PathVariable Long id, @RequestParam boolean disponible, RedirectAttributes ra) {
        try {
            Court court = courtService.changeAvailability(id, disponible);
            String action = disponible ? "habilitada" : "deshabilitada";
            ra.addFlashAttribute("mensajeExito", "La cancha '" + court.getName() + "' fue " + action + " exitosamente.");
        } catch (Exception e) {
            ra.addFlashAttribute("mensajeError", "Error al actualizar estado: " + e.getMessage());
        }
        return "redirect:/admin/canchas";
    }

}
