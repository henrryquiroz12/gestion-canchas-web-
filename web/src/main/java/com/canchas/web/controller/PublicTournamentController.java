package com.canchas.web.controller;

import com.canchas.web.exception.DuplicateDataException;
import com.canchas.web.exception.ResourceNotFoundException;
import com.canchas.web.model.Tournament;
import com.canchas.web.model.TournamentRegistration;
import com.canchas.web.model.TournamentRegistrationStatus;
import com.canchas.web.model.User;
import com.canchas.web.service.TournamentService;
import com.canchas.web.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/torneos")
@RequiredArgsConstructor
public class PublicTournamentController {

    private final TournamentService tournamentService;
    private final UserService userService;

    @GetMapping
    public String listTournaments(Model model) {
        List<Tournament> tournaments = tournamentService.findAllActive();
        model.addAttribute("torneos", tournaments);
        model.addAttribute("activeSection", "torneos");
        return "torneos/lista";
    }

    @GetMapping("/como-funciona")
    public String howItWorks(Model model) {
        model.addAttribute("activeSection", "torneos");
        return "torneos/como-funciona";
    }

    @GetMapping("/{slug}")
    public String tournamentDetail(@PathVariable String slug,
                                   Model model,
                                   Authentication authentication) {
        Tournament tournament = tournamentService.findBySlug(slug);
        List<TournamentRegistration> confirmedRegistrations = tournamentService.findConfirmedRegistrations(tournament);

        TournamentRegistration myRegistration = null;
        boolean isClient = false;
        User currentClient = null;

        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            try {
                User user = userService.findByUsername(authentication.getName());
                if (user.getRole() == User.Role.CLIENTE) {
                    isClient = true;
                    currentClient = user;
                    Optional<TournamentRegistration> myRegOpt = tournamentService.findRegistrationByTournamentAndClient(tournament, user.getId());
                    if (myRegOpt.isPresent()) {
                        myRegistration = myRegOpt.get();
                    }
                }
            } catch (Exception ignored) {
            }
        }

        model.addAttribute("torneo", tournament);
        model.addAttribute("equiposConfirmados", confirmedRegistrations);
        model.addAttribute("totalConfirmados", confirmedRegistrations.size());
        model.addAttribute("miInscripcion", myRegistration);
        model.addAttribute("esCliente", isClient);
        model.addAttribute("currentClient", currentClient);
        model.addAttribute("activeSection", "torneos");
        return "torneos/detalle";
    }

    @GetMapping("/{slug}/inscribirse")
    @PreAuthorize("hasRole('CLIENTE')")
    public String redirectToDetailWithForm(@PathVariable String slug) {
        return "redirect:/torneos/" + slug + "#inscribir-equipo";
    }

    @PostMapping("/{slug}/inscribirse")
    @PreAuthorize("hasRole('CLIENTE')")
    public String registerTeam(@PathVariable String slug,
                               @RequestParam("nombreEquipo") String teamName,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            User client = userService.findByUsername(authentication.getName());
            if (client.getRole() != User.Role.CLIENTE) {
                redirectAttributes.addFlashAttribute("error", "Los administradores no pueden inscribir equipos.");
                return "redirect:/torneos/" + slug;
            }

            TournamentRegistration registration = tournamentService.registerTeam(slug, client.getId(), teamName);
            if (registration.getStatus() == TournamentRegistrationStatus.CONFIRMADA) {
                redirectAttributes.addFlashAttribute("exito", "¡Ynscripción confirmada con éxito! Tu equipo '" + registration.getTeamName() + "' ya está registrado en el torneo.");
            } else {
                redirectAttributes.addFlashAttribute("info", "Inscripción registrada. Pago pendiente de confirmación. Tu equipo '" + registration.getTeamName() + "' quedará confirmado una vez validado el pago de S/ " + registration.getAppliedAmount() + ".");
            }
            return "redirect:/torneos/" + slug + "?inscrito=true";

        } catch (DuplicateDataException | IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/torneos/" + slug;
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Ocurrió un error al procesar la inscripción: " + ex.getMessage());
            return "redirect:/torneos/" + slug;
        }
    }
}
