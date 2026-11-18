package com.canchas.web.controller.admin;

import com.canchas.web.model.TournamentRegistration;
import com.canchas.web.service.TournamentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/torneos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminTournamentController {

    private final TournamentService tournamentService;

    @GetMapping
    public String listRegistrations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            Model model) {
        Page<TournamentRegistration> registrationsPage = tournamentService.findAllRegistrationsPaginated(page, size);
        model.addAttribute("registrationsPage", registrationsPage);
        model.addAttribute("inscripciones", registrationsPage.getContent());
        model.addAttribute("totalInscripciones", registrationsPage.getTotalElements());
        model.addAttribute("activeSection", "gestion");
        return "admin/torneos/lista";
    }

    @PostMapping("/inscripciones/{id}/confirmar")
    public String confirmRegistration(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            TournamentRegistration reg = tournamentService.confirmRegistration(id);
            redirectAttributes.addFlashAttribute("exito", "Inscripción del equipo '" + reg.getTeamName() + "' confirmada exitosamente.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Error al confirmar inscripción: " + ex.getMessage());
        }
        return "redirect:/admin/torneos";
    }

    @PostMapping("/inscripciones/{id}/cancelar")
    public String cancelRegistration(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            TournamentRegistration reg = tournamentService.cancelRegistration(id);
            redirectAttributes.addFlashAttribute("info", "Inscripción del equipo '" + reg.getTeamName() + "' cancelada.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Error al cancelar inscripción: " + ex.getMessage());
        }
        return "redirect:/admin/torneos";
    }
}
