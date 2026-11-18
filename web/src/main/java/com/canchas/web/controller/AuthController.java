package com.canchas.web.controller;

import com.canchas.web.dto.UserRegistrationDTO;
import com.canchas.web.model.Gender;
import com.canchas.web.model.PeruDepartment;
import com.canchas.web.util.PeruLocationCatalog;
import com.canchas.web.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import com.canchas.web.exception.DuplicateDataException;
import com.canchas.web.validation.AccountRules;
import java.util.Map;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String showLogin() {
        return "login";
    }

    @GetMapping("/registro")
    public String showRegistration(Model model) {
        UserRegistrationDTO dto = new UserRegistrationDTO();
        model.addAttribute("registroDto", dto);
        model.addAttribute("registrationDto", dto);
        model.addAttribute("departamentos", PeruDepartment.values());
        model.addAttribute("provinciasMap", PeruLocationCatalog.getAllProvincesByDepartmentCode());
        model.addAttribute("generos", Gender.values());
        return "registro";
    }

    @PostMapping("/registro")
    public String processRegistration(@Valid @ModelAttribute("registroDto") UserRegistrationDTO dto,
                                      BindingResult result,
                                      RedirectAttributes ra,
                                      Model model) {
        model.addAttribute("departamentos", PeruDepartment.values());
        model.addAttribute("provinciasMap", PeruLocationCatalog.getAllProvincesByDepartmentCode());
        model.addAttribute("generos", Gender.values());
        model.addAttribute("registrationDto", dto);
        if (dto.getPassword() != null && !dto.getPassword().equals(dto.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "password.mismatch", "Las contraseñas no coinciden");
        }
        if (dto.getDni() != null && dto.getDni().matches(AccountRules.DNI) && userService.existsByDni(dto.getDni())) {
            result.rejectValue("dni", "dni.duplicate", "El DNI ya se encuentra registrado.");
        }
        if (result.hasErrors()) {
            return "registro";
        }
        try {
            userService.registerClient(dto);
            ra.addFlashAttribute("mensajeExito", "¡Registro exitoso! Ya puedes iniciar sesión con tu cuenta.");
            return "redirect:/login?registroExitoso=true";
        } catch (DuplicateDataException ex) {
            result.rejectValue(ex.getField(), "account.duplicate", ex.getMessage());
            return "registro";
        } catch (DataIntegrityViolationException ex) {
            // The unique constraint is authoritative if another request won the race.
            if (userService.existsByDni(dto.getDni())) {
                result.rejectValue("dni", "dni.duplicate", "El DNI ya se encuentra registrado.");
            } else {
                model.addAttribute("mensajeError", "Los datos de la cuenta ya están registrados. Revisa el correo y el usuario.");
            }
            return "registro";
        } catch (RuntimeException ex) {
            model.addAttribute("mensajeError", ex.getMessage());
            return "registro";
        }
    }

    @GetMapping("/registro/validar-dni")
    @ResponseBody
    public ResponseEntity<?> validateDni(@RequestParam String dni) {
        if (!dni.matches(AccountRules.DNI)) {
            return ResponseEntity.badRequest().body(Map.of("dni", "El DNI debe contener exactamente 8 dígitos."));
        }
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore())
                .body(Map.of("available", !userService.existsByDni(dni)));
    }

    @GetMapping("/error/access-denied")
    public String accessDenied() {
        return "error/403";
    }
}
