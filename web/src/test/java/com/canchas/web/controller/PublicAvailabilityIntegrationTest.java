package com.canchas.web.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class PublicAvailabilityIntegrationTest {

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
    @DisplayName("Visitante anónimo consulta disponibilidad de Cancha 1 para fecha futura -> HTTP 200 JSON con slots")
    void testAnonymousAvailabilityQueryFutureDate() throws Exception {
        LocalDate fechaFutura = LocalDate.now(ZoneId.of("America/Lima")).plusDays(30);

        mockMvc.perform(get("/api/canchas/1/disponibilidad")
                .param("fecha", fechaFutura.toString())
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[0].startTime", notNullValue()))
                .andExpect(jsonPath("$[0].endTime", notNullValue()))
                .andExpect(jsonPath("$[0].basePrice", notNullValue()))
                .andExpect(jsonPath("$[0].lightingSurcharge", notNullValue()))
                .andExpect(jsonPath("$[0].totalPrice", notNullValue()))
                .andExpect(jsonPath("$[0].status", notNullValue()));
    }

    @Test
    @DisplayName("Disponibilidad en horario habitual 07:00-24:00 contiene slots desde 07:00 hasta 23:00")
    void testAvailabilitySlotsRange() throws Exception {
        LocalDate fechaFutura = LocalDate.now(ZoneId.of("America/Lima")).plusDays(40);

        mockMvc.perform(get("/api/canchas/1/disponibilidad")
                .param("fecha", fechaFutura.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].startTime", is("07:00:00")))
                .andExpect(jsonPath("$[?(@.startTime == '18:00:00')].lightingSurcharge", hasItem(20.0)));
    }
}