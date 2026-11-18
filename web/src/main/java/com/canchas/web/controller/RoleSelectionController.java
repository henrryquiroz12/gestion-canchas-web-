package com.canchas.web.controller;

import com.canchas.web.model.User;
import com.canchas.web.service.UserService;
import com.canchas.web.security.ActiveRoleAccess;
import org.springframework.security.access.AccessDeniedException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class RoleSelectionController {

    private final UserService userService;

    @GetMapping("/seleccionar-rol")
    public String showRoleSelection(Authentication authentication, Model model, HttpSession session) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return "redirect:/login";
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN") || a.equals("ADMIN"));

        boolean isCliente = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_CLIENTE") || a.equals("CLIENTE"));

        User user = userService.findByUsername(authentication.getName());

        model.addAttribute("usuario", user);
        model.addAttribute("tieneRolAdmin", isAdmin);
        model.addAttribute("tieneRolCliente", isCliente);
        model.addAttribute("activeRole", session.getAttribute("ACTIVE_ROLE"));

        return "seleccionar-rol";
    }

    @GetMapping("/seleccionar-rol/admin")
    public String selectAdminRole(Authentication authentication, HttpSession session) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN") || a.equals("ADMIN"));

        if (!isAdmin) {
            return "redirect:/error/access-denied";
        }

        session.setAttribute(ActiveRoleAccess.SESSION_ATTRIBUTE, "ADMIN");
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/seleccionar-rol/cliente")
    public String selectClienteRole(Authentication authentication, HttpSession session) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        if (!ActiveRoleAccess.hasRole(authentication, "CLIENTE")) {
            throw new AccessDeniedException("El usuario no tiene el rol Cliente");
        }

        session.setAttribute(ActiveRoleAccess.SESSION_ATTRIBUTE, "CLIENTE");
        return "redirect:/canchas";
    }

    @GetMapping("/cambiar-rol")
    public String switchRole() {
        return "redirect:/seleccionar-rol";
    }
}

