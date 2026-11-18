package com.canchas.web.security;

import com.canchas.web.config.SecurityConfig;
import com.canchas.web.model.PeruDepartment;
import com.canchas.web.model.User;
import com.canchas.web.repository.UserRepository;
import com.canchas.web.util.LimaTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Random;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class RoleSecurityAndSavedRequestTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        if (userRepository.findByUsername("cliente_test").isEmpty()) {
            User client = new User();
            client.setUsername("cliente_test");
            client.setPassword(passwordEncoder.encode("Password123!"));
            client.setFullName("Cliente Test");
            client.setEmail("cliente_test@canchas.com");
            client.setDni("87654321");
            client.setPhoneNumber("912345678");
            client.setDepartment(PeruDepartment.LA_LIBERTAD);
            client.setRole(User.Role.CLIENTE);
            client.setActive(true);
            client.setRegisteredAt(LocalDate.now());
            userRepository.save(client);
        }
    }

    @Test
    void registrationRendersSharedRulesAndPublicDniCheck() throws Exception {
        mockMvc.perform(get("/registro"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("window.ACCOUNT_RULES")))
                .andExpect(content().string(containsString("/js/core/account-validation.js")));
        mockMvc.perform(get("/registro/validar-dni").param("dni", "87654321"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    @DisplayName("A) GET /img/hero-futbol.jpg y recursos estaticos son accesibles sin autenticacion")
    void testStaticResourcesAccessibleWithoutLogin() throws Exception {
        mockMvc.perform(get("/img/hero-futbol.jpg"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/css/global.css"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("B) Validador de URLs estaticas rechaza imagenes, CSS, JS, favicons y APIs")
    void testStaticUrlSanitizer() {
        assertTrue(SecurityConfig.isStaticOrInvalidUrl("/img/hero-futbol.jpg"));
        assertTrue(SecurityConfig.isStaticOrInvalidUrl("/img/hero-futbol.jpg?continue"));
        assertTrue(SecurityConfig.isStaticOrInvalidUrl("/images/brand/logo.png"));
        assertTrue(SecurityConfig.isStaticOrInvalidUrl("/css/global.css"));
        assertTrue(SecurityConfig.isStaticOrInvalidUrl("/js/app.js"));
        assertTrue(SecurityConfig.isStaticOrInvalidUrl("/favicon.ico"));
        assertTrue(SecurityConfig.isStaticOrInvalidUrl("/api/canchas/1/disponibilidad"));

        // Rutas funcionales de navegacion legitimas NUNCA son estaticas
        assertFalse(SecurityConfig.isStaticOrInvalidUrl("/canchas"));
        assertFalse(SecurityConfig.isStaticOrInvalidUrl("/canchas/1"));
        assertFalse(SecurityConfig.isStaticOrInvalidUrl("/cliente/reservas"));
        assertFalse(SecurityConfig.isStaticOrInvalidUrl("/cliente/reservas/nueva?canchaId=1&fecha=2026-10-25&horaInicio=18:00"));
        assertFalse(SecurityConfig.isStaticOrInvalidUrl("/admin/dashboard"));
        assertFalse(SecurityConfig.isStaticOrInvalidUrl("/admin/torneos"));
    }

    @Test
    @DisplayName("C) Login ADMIN redirige a /seleccionar-rol para elegir entre Administrador y Cliente")
    void testAdminLoginRedirectsToRoleSelection() throws Exception {
        mockMvc.perform(post("/login")
                .param("username", "Mark2026")
                .param("password", "Proyectoweb2026")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/seleccionar-rol"));
    }

    @Test
    @DisplayName("D) Registro exitoso NUNCA redirige a una imagen, redirige a /login?registroExitoso=true")
    void testRegistrationRedirectsToLogin() throws Exception {
        int randomSuffix = new Random().nextInt(90000000) + 10000000;
        String uniqueDni = String.valueOf(randomSuffix);
        String uniqueUser = "usr" + uniqueDni.substring(0, 6);

        var mvcResult = mockMvc.perform(post("/registro")
                .param("username", uniqueUser)
                .param("password", "Password123!")
                .param("confirmPassword", "Password123!")
                .param("fullName", "Usuario Registro Valido")
                .param("email", uniqueUser + "@gmail.com")
                .param("dni", uniqueDni)
                .param("phoneNumber", "987654321")
                .param("department", "LA_LIBERTAD")
                .param("province", "Trujillo")
                .param("gender", "MALE")
                .param("address", "Av. Principal 123")
                .with(csrf()))
                .andReturn();

        if (mvcResult.getModelAndView() != null) {
            System.out.println("REGISTRO MODEL: " + mvcResult.getModelAndView().getModel());
        }
        assertEquals(302, mvcResult.getResponse().getStatus());
        assertEquals("/login?registroExitoso=true", mvcResult.getResponse().getRedirectedUrl());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Seguridad: ADMIN accede a /admin/dashboard (200)")
    void testAdminCanAccessAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "cliente_test", roles = {"CLIENTE"})
    @DisplayName("Seguridad: CLIENTE intentando acceder a /admin/dashboard recibe 403 Forbidden")
    void testClientCannotAccessAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Seguridad: ADMIN intentando acceder a /cliente/reservas recibe 403 Forbidden")
    void testAdminCannotAccessClientReservas() throws Exception {
        mockMvc.perform(get("/cliente/reservas"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "cliente_test", roles = {"CLIENTE"})
    @DisplayName("Seguridad: CLIENTE accede a /cliente/reservas (200)")
    void testClientCanAccessClientReservas() throws Exception {
        mockMvc.perform(get("/cliente/reservas"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Seguridad: ANONIMO accediendo a /cliente/reservas es redirigido a /login")
    void testAnonymousAccessToClientReservasRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/cliente/reservas"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("Paginas publicas accesibles para ANONIMO")
    void testPublicPagesAccessibleToAnonymous() throws Exception {
        mockMvc.perform(get("/inicio")).andExpect(status().isOk());
        mockMvc.perform(get("/canchas")).andExpect(status().isOk());
        mockMvc.perform(get("/torneos")).andExpect(status().isOk());
        mockMvc.perform(get("/nosotros")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Paginas publicas accesibles para ADMIN")
    void testPublicPagesAccessibleToAdmin() throws Exception {
        mockMvc.perform(get("/inicio")).andExpect(status().isOk());
        mockMvc.perform(get("/canchas")).andExpect(status().isOk());
        mockMvc.perform(get("/torneos")).andExpect(status().isOk());
        mockMvc.perform(get("/nosotros")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "cliente_test", roles = {"CLIENTE"})
    @DisplayName("Dropdown CLIENTE contiene Mi Perfil, Mis Reservas y NO contiene Mi Panel")
    void testClientDropdownOptions() throws Exception {
        mockMvc.perform(get("/inicio"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Mi Perfil")))
                .andExpect(content().string(containsString("Mis Reservas")))
                .andExpect(content().string(containsString("Cerrar Sesión")))
                .andExpect(content().string(not(containsString("Mi Panel"))));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Dropdown ADMIN contiene Cambiar Rol y Cerrar Sesion sin opciones de Cliente")
    void testAdminDropdownOptions() throws Exception {
        mockMvc.perform(get("/inicio"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cambiar de rol")))
                .andExpect(content().string(not(containsString("Mi Panel"))))
                .andExpect(content().string(not(containsString("Mis Reservas"))))
                .andExpect(content().string(containsString("Cerrar Sesión")))
                .andExpect(content().string(not(containsString("Mi Perfil"))));
    }

    @Test
    @DisplayName("Horarios: conversion visual de 00:00 (medianoche) a 24:00")
    void testSlotPresentationHelper2400() {
        LocalTime start = LocalTime.of(23, 0);
        LocalTime end = LocalTime.of(0, 0);

        String visualRange = LimaTime.formatSlotRange(start, end);
        assertEquals("23:00 - 24:00", visualRange, "El ultimo slot debe presentarse como 23:00 - 24:00");

        String normalRange = LimaTime.formatSlotRange(LocalTime.of(17, 0), LocalTime.of(18, 0));
        assertEquals("17:00 - 18:00", normalRange);
    }
}
