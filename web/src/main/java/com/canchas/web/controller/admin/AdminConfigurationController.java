package com.canchas.web.controller.admin;

import com.canchas.web.model.BusinessConfiguration;
import com.canchas.web.repository.BusinessConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/configuracion")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminConfigurationController {

    private final BusinessConfigurationRepository configurationRepository;
    private final com.canchas.web.repository.CourtRepository courtRepository;

    @GetMapping
    public String viewConfiguration(Model model) {
        BusinessConfiguration config = configurationRepository.findById((short) 1)
                .orElse(new BusinessConfiguration());
        model.addAttribute("configuracion", config);
        model.addAttribute("configuration", config);
        model.addAttribute("activeSection", "configuracion");
        return "admin/configuracion/editar";
    }

    @PostMapping
    @org.springframework.transaction.annotation.Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public String saveConfiguration(@ModelAttribute BusinessConfiguration form, RedirectAttributes ra) {
        try {
            courtRepository.lockAllForScheduleUpdate();
            BusinessConfiguration config = configurationRepository.findById((short) 1)
                .orElse(new BusinessConfiguration());
            if (form.getContactPhone() == null || !form.getContactPhone().matches("^9[0-9]{8}$")) {
                throw new IllegalArgumentException("El teléfono debe contener 9 dígitos y comenzar con 9.");
            }
            if (form.getContactEmail() == null || !form.getContactEmail().matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
                throw new IllegalArgumentException("El correo de contacto no es válido.");
            }
            config.setCompanyName(form.getCompanyName());
            config.setCity(form.getCity());
            config.setAddress(form.getAddress());
            config.setContactPhone(form.getContactPhone());
            config.setContactEmail(form.getContactEmail());
            config.setDefaultOpeningTime(form.getDefaultOpeningTime());
            config.setDefaultClosingTime(form.getDefaultClosingTime());
            config.setLightingStartTime(form.getLightingStartTime());
            config.setLightingSurcharge(form.getLightingSurcharge());

            configurationRepository.save(config);
            ra.addFlashAttribute("mensajeExito", "Configuración guardada correctamente.");
        } catch (Exception e) {
            ra.addFlashAttribute("mensajeError", "Error al guardar configuración: " + e.getMessage());
        }
        return "redirect:/admin/configuracion";
    }
}
