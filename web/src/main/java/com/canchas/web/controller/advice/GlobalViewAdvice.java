package com.canchas.web.controller.advice;

import com.canchas.web.model.BusinessConfiguration;
import com.canchas.web.model.User;
import com.canchas.web.repository.UserRepository;
import com.canchas.web.security.ActiveRoleAccess;
import com.canchas.web.service.BusinessConfigurationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalViewAdvice {

    private final UserRepository userRepository;
    private final BusinessConfigurationService configurationService;

    @ModelAttribute
    public void addGlobalAttributes(Model model, Authentication authentication, HttpServletRequest request) {
        try {
            BusinessConfiguration config = configurationService.getConfiguration();
            model.addAttribute("companyConfig", config);
        } catch (Exception ignored) {
        }

        boolean isAdminSection = false;
        if (request != null) {
            String uri = request.getRequestURI();
            if (uri != null && (uri.startsWith("/admin/") || uri.equals("/admin"))) {
                isAdminSection = true;
            }
        }
        model.addAttribute("isAdminSection", isAdminSection);
        model.addAttribute("activeRole", ActiveRoleAccess.resolve(authentication, request));
        model.addAttribute("canSwitchRole", ActiveRoleAccess.hasRole(authentication, "ADMIN"));

        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            userRepository.findByUsername(authentication.getName()).ifPresent(user -> {
                model.addAttribute("currentUser", user);
                String displayName = formatDisplayName(user.getFullName(), user.getUsername());
                model.addAttribute("displayName", displayName);
            });
        }
    }

    public void addGlobalAttributes(Model model, Authentication authentication) {
        addGlobalAttributes(model, authentication, null);
    }

    /**
     * Regla de Display Name para Navbar (Primer Nombre + Primer Apellido):
     * - Si nombreCompleto tiene 4 o más palabras: primera palabra + penúltima palabra.
     *   Ej: "Juan José Pérez García" -> "Juan Pérez"
     *       "Marlon Robinson Quipuzco Vargas" -> "Marlon Quipuzco"
     * - Si tiene 3 palabras: primera palabra + segunda palabra.
     *   Ej: "Pepe Pérez González" -> "Pepe Pérez"
     * - Si tiene 2 palabras: primera palabra + segunda palabra.
     *   Ej: "Juan Pérez" -> "Juan Pérez"
     * - Si tiene 1 palabra: primera palabra.
     *   Ej: "Juan" -> "Juan"
     * - Si es nulo o vacío: fallback al username.
     */
    public static String formatDisplayName(String fullName, String fallbackUsername) {
        if (fullName == null || fullName.isBlank()) {
            return fallbackUsername != null ? fallbackUsername : "";
        }
        String[] words = fullName.trim().split("\\s+");
        if (words.length == 0 || words[0].isBlank()) {
            return fallbackUsername != null ? fallbackUsername : "";
        }
        if (words.length == 1) {
            return words[0];
        }
        if (words.length == 2 || words.length == 3) {
            return words[0] + " " + words[1];
        }
        // 4 o más palabras: primera palabra + penúltima palabra
        return words[0] + " " + words[words.length - 2];
    }
}
