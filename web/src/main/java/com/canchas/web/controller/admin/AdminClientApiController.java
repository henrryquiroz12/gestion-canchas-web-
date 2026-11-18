package com.canchas.web.controller.admin;

import com.canchas.web.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/api/clientes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminClientApiController {

    private final UserService userService;

    @GetMapping(value = "/buscar", params = {"criterio", "valor", "!dni"})
    public ResponseEntity<?> searchClients(@RequestParam String criterio, @RequestParam String valor) {
        try {
            return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore())
                    .body(userService.searchClients(criterio, valor));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("valor", ex.getMessage()));
        }
    }

    @GetMapping(value = "/buscar", params = {"dni", "!criterio", "!valor"})
    public ResponseEntity<?> findByDni(@RequestParam String dni) {
        if (dni == null || !dni.matches("^[0-9]{8}$")) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "El DNI debe contener exactamente 8 dígitos."));
        }
        return userService.findActiveClientByDni(dni)
                .map(u -> ResponseEntity.ok(Map.of(
                        "id", u.getId(),
                        "nombreCompleto", u.getFullName(),
                        "fullName", u.getFullName(),
                        "dni", u.getDni() != null ? u.getDni() : "",
                        "email", u.getEmail()
                )))
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("mensaje", "No se encontró un cliente registrado con ese DNI.")));
    }
}
