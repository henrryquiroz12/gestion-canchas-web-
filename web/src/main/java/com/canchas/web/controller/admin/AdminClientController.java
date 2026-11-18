package com.canchas.web.controller.admin;

import com.canchas.web.dto.UserRequestDTO;
import com.canchas.web.model.User;
import com.canchas.web.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin/clientes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminClientController {

    private final UserService userService;

    @GetMapping
    public String listClients(Model model) {
        model.addAttribute("activeSection", "clientes");
        return "admin/clientes/lista";
    }

    @PostMapping("/{id}/editar")
    @ResponseBody
    public ResponseEntity<Map<String, String>> editClient(@PathVariable Long id, @Valid @ModelAttribute UserRequestDTO dto, BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(extractErrors(result));
        }
        try {
            dto.setRole(User.Role.CLIENTE);
            userService.update(id, dto);
            return ResponseEntity.ok(Map.of("mensaje", "Cliente actualizado correctamente"));
        } catch (com.canchas.web.exception.DuplicateDataException ex) {
            return ResponseEntity.badRequest().body(Map.of(ex.getField(), ex.getMessage()));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("global", ex.getMessage()));
        }
    }

    @PostMapping("/{id}/estado")
    public String changeClientStatus(@PathVariable Long id, @RequestParam boolean activo, RedirectAttributes ra) {
        try {
            User user = userService.findById(id);
            if (user.hasRole("ROLE_CLIENTE") || user.hasRole("CLIENTE")) {
                user.setActive(activo);
                UserRequestDTO dto = new UserRequestDTO();
                dto.setFullName(user.getFullName());
                dto.setEmail(user.getEmail());
                dto.setPhoneNumber(user.getPhoneNumber());
                dto.setAddress(user.getAddress());
                dto.setUsername(user.getUsername());
                dto.setRole(User.Role.CLIENTE);
                dto.setActive(activo);
                userService.update(id, dto);
                ra.addFlashAttribute("mensajeExito", activo ? "Cliente activado correctamente." : "Cliente inhabilitado correctamente.");
            } else {
                ra.addFlashAttribute("mensajeError", "Solo se puede cambiar el estado a usuarios con rol CLIENTE.");
            }
        } catch (Exception e) {
            ra.addFlashAttribute("mensajeError", "Error al actualizar estado: " + e.getMessage());
        }
        return "redirect:/admin/clientes";
    }

    private Map<String, String> extractErrors(BindingResult result) {
        Map<String, String> errors = new HashMap<>();
        result.getFieldErrors().forEach(fe -> errors.put(fe.getField(), fe.getDefaultMessage()));
        return errors;
    }
}
