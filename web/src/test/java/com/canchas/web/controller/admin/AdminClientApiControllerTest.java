package com.canchas.web.controller.admin;

import com.canchas.web.model.User;
import com.canchas.web.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class AdminClientApiControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private AdminClientApiController controller;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void testFindByDni_Existing_Returns200() throws Exception {
        User u = new User();
        u.setId(5L);
        u.setFullName("Juan Perez");
        u.setDni("12345678");
        u.setEmail("juan@correo.com");
        u.setRole(User.Role.CLIENTE);
        u.setActive(true);

        when(userService.findActiveClientByDni("12345678")).thenReturn(Optional.of(u));

        mockMvc.perform(get("/admin/api/clientes/buscar").param("dni", "12345678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.nombreCompleto").value("Juan Perez"))
                .andExpect(jsonPath("$.dni").value("12345678"))
                .andExpect(jsonPath("$.email").value("juan@correo.com"));
    }

    @Test
    public void testFindByDni_NotFound_Returns404() throws Exception {
        when(userService.findActiveClientByDni("87654321")).thenReturn(Optional.empty());

        mockMvc.perform(get("/admin/api/clientes/buscar").param("dni", "87654321"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No se encontró un cliente registrado con ese DNI."));
    }

    @Test
    public void testFindByDni_InvalidFormat_Returns400() throws Exception {
        mockMvc.perform(get("/admin/api/clientes/buscar").param("dni", "1234"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El DNI debe contener exactamente 8 dígitos."));
    }
}
