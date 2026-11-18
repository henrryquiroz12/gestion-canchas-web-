package com.canchas.web.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class PublicViewsSmokeTest {

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
    @DisplayName("GET /inicio -> 200 y renderiza portada sin sección historia")
    void testInicioView() throws Exception {
        mockMvc.perform(get("/inicio"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Busca.")))
                .andExpect(content().string(containsString("Reserva.")))
                .andExpect(content().string(containsString("Juega.")))
                .andExpect(content().string(containsString("07:00 - 24:00")));
    }

    @Test
    @DisplayName("GET /canchas -> 200 y renderiza catálogo con Grass Sintético")
    void testCanchasView() throws Exception {
        mockMvc.perform(get("/canchas"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Encuentra tu cancha")))
                .andExpect(content().string(containsString("Grass Sintético")));
    }

    @Test
    @DisplayName("GET /canchas/1 -> 200 y renderiza detalle de cancha sin error 500")
    void testCanchaDetalleView() throws Exception {
        mockMvc.perform(get("/canchas/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cancha 1")))
                .andExpect(content().string(containsString("Grass Sintético")))
                .andExpect(content().string(containsString("Reservar horario")));
    }

    @Test
    @DisplayName("GET /login -> 200 y renderiza login accesible sin error 500")
    void testLoginView() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Iniciar Sesión")))
                .andExpect(content().string(containsString("username")))
                .andExpect(content().string(containsString("password")));
    }

    @Test
    @DisplayName("GET /registro -> 200 y renderiza wizard de 3 pasos")
    void testRegistroView() throws Exception {
        mockMvc.perform(get("/registro"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Crea tu cuenta")))
                .andExpect(content().string(containsString("Registro de Cliente")))
                .andExpect(content().string(containsString("Paso 1: Datos personales")));
    }

    @Test
    @DisplayName("GET /torneos -> 200 y renderiza lista de torneos con títulos legibles")
    void testTorneosView() throws Exception {
        mockMvc.perform(get("/torneos"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Torneos y Copas Fredy Queen")))
                .andExpect(content().string(containsString("Torneo de Bienvenida")))
                .andExpect(content().string(containsString("Copa Fredy Queen")));
    }

    @Test
    @DisplayName("GET /torneos/torneo-bienvenida -> 200 y renderiza detalle gratuito")
    void testTorneoBienvenidaDetalle() throws Exception {
        mockMvc.perform(get("/torneos/torneo-bienvenida"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Torneo de Bienvenida")))
                .andExpect(content().string(containsString("Gratuito")))
                .andExpect(content().string(containsString("Volver a torneos")))
                .andExpect(content().string(not(containsString("fqs-breadcrumb"))));
    }

    @Test
    @DisplayName("GET /torneos/copa-fredy-queen -> 200 y renderiza detalle oficial")
    void testCopaFredyQueenDetalle() throws Exception {
        mockMvc.perform(get("/torneos/copa-fredy-queen"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Copa Fredy Queen")))
                .andExpect(content().string(containsString("280.00")))
                .andExpect(content().string(containsString("Volver a torneos")))
                .andExpect(content().string(not(containsString("fqs-breadcrumb"))));
    }

    @Test
    @DisplayName("GET /torneos/como-funciona -> 200 y renderiza guía de participación")
    void testTorneosComoFuncionaView() throws Exception {
        mockMvc.perform(get("/torneos/como-funciona"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("¿Cómo Funcionan los Torneos?")));
    }

    @Test
    @DisplayName("GET /nosotros -> 200 y renderiza historia institucional y galería")
    void testNosotrosView() throws Exception {
        mockMvc.perform(get("/nosotros"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nuestra Historia")))
                .andExpect(content().string(containsString("Misión, Visión y Valores")));
    }
}
