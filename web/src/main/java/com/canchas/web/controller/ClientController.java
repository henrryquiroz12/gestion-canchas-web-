package com.canchas.web.controller;

import com.canchas.web.dto.ClientProfileDTO;
import com.canchas.web.model.Gender;
import com.canchas.web.model.PeruDepartment;
import com.canchas.web.util.PeruLocationCatalog;
import com.canchas.web.model.User;
import com.canchas.web.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/cliente")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENTE')")
public class ClientController {

    private final UserService userService;

    @GetMapping("/dashboard")
    public String dashboard() {
        return "redirect:/canchas";
    }

    @GetMapping("/perfil")
    public String profile(Model model, Authentication authentication) {
        User client = userService.findByUsername(authentication.getName());
        model.addAttribute("cliente", client);
        model.addAttribute("client", client);
        model.addAttribute("departamentos", PeruDepartment.values());
        model.addAttribute("provinciasMap", PeruLocationCatalog.getAllProvincesByDepartmentCode());
        model.addAttribute("generos", Gender.values());
        model.addAttribute("activeSection", "perfil");
        return "cliente/perfil";
    }

    @PostMapping("/perfil/editar")
    @ResponseBody
    public ResponseEntity<Map<String, String>> editProfile(@Valid @ModelAttribute ClientProfileDTO dto,
                                                           BindingResult result,
                                                           Authentication authentication) {
        User client = userService.findByUsername(authentication.getName());
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(extractErrors(result));
        }
        try {
            userService.updateProfile(client.getId(), dto);
            return ResponseEntity.ok(Map.of("mensaje", "Cambios guardados correctamente."));
        } catch (com.canchas.web.exception.DuplicateDataException ex) {
            return ResponseEntity.badRequest().body(Map.of(ex.getField(), ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("global", ex.getMessage()));
        }
    }

    private Map<String, String> extractErrors(BindingResult result) {
        Map<String, String> errors = new HashMap<>();
        result.getFieldErrors().forEach(fe -> errors.put(fe.getField(), fe.getDefaultMessage()));
        return errors;
    }
}