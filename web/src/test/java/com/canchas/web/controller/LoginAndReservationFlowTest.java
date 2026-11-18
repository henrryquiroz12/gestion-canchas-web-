package com.canchas.web.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.hamcrest.Matchers.endsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class LoginAndReservationFlowTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    @DisplayName("Credenciales incorrectas en login -> redirección controlada a /login?error=true sin HTTP 500")
    void testInvalidLogin() throws Exception {
        mockMvc.perform(post("/login")
                .param("username", "usuario_no_existente")
                .param("password", "clave_invalida")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    @DisplayName("Credenciales válidas ADMIN (Mark2026 / Proyectoweb2026) -> redirección a /seleccionar-rol")
    void testAdminLogin() throws Exception {
        mockMvc.perform(post("/login")
                .param("username", "Mark2026")
                .param("password", "Proyectoweb2026")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/seleccionar-rol"));
    }

    @Test
    @DisplayName("Visitante anónimo accede a /cliente/reservas/nueva -> redirigido a login sin error 500")
    void testAnonymousAccessToReservationRedirectsToLogin() throws Exception {
        String hoyLima = LocalDate.now(ZoneId.of("America/Lima")).toString();
        mockMvc.perform(get("/cliente/reservas/nueva")
                .param("canchaId", "1")
                .param("fecha", hoyLima)
                .param("horaInicio", "09:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", endsWith("/login")));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"CLIENTE"})
    @DisplayName("Cliente autenticado accede a /cliente/reservas/nueva -> renderiza confirmación")
    void testAuthenticatedClientCanAccessReservation() throws Exception {
        String hoyLima = LocalDate.now(ZoneId.of("America/Lima")).plusDays(1).toString();
        mockMvc.perform(get("/cliente/reservas/nueva")
                .param("canchaId", "1")
                .param("fecha", hoyLima)
                .param("horaInicio", "10:00"))
                .andExpect(status().isOk())
                .andExpect(view().name("cliente/reservas/confirmar"));
    }
}