package com.canchas.web.controller;

import com.canchas.web.config.SecurityConfig;
import com.canchas.web.controller.admin.AdminReservationController;
import com.canchas.web.model.Court;
import com.canchas.web.model.Reservation;
import com.canchas.web.model.User;
import com.canchas.web.security.CustomUserDetailsService;
import com.canchas.web.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitWebConfig(classes = {SecurityConfig.class, ReservationListRenderingTest.Config.class,
        ClientReservationController.class, AdminReservationController.class})
class ReservationListRenderingTest {
    @Autowired WebApplicationContext context;
    @Autowired ReservationService reservations;
    @Autowired UserService users;
    MockMvc mvc;
    Reservation reservation;

    @BeforeEach
    void setUp() {
        reset(reservations, users);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        User client = new User();
        client.setId(1L);
        client.setFullName("Cliente de prueba");
        when(users.findByUsername("user")).thenReturn(client);
        Court court = new Court();
        court.setName("Cancha de prueba");
        reservation = new Reservation();
        reservation.setId(7L);
        reservation.setClient(client);
        reservation.setCourt(court);
        reservation.setReservationDate(LocalDate.of(2027, 1, 1));
        reservation.setStartTime(LocalTime.of(18, 0));
        reservation.setEndTime(LocalTime.of(19, 0));
        reservation.setTotalAmount(new BigDecimal("100.00"));
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void clientEmptyListRenders() throws Exception {
        when(reservations.findByClientId(1L)).thenReturn(new ArrayList<>());
        mvc.perform(get("/cliente/reservas"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No tienes reservas")))
                .andExpect(content().string(containsString("</html>")));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "CLIENTE"})
    void adminAccountInClientRoleRendersPersonalReservations() throws Exception {
        when(reservations.findByClientId(1L)).thenReturn(new ArrayList<>(List.of(reservation)));
        mvc.perform(get("/cliente/reservas").sessionAttr("ACTIVE_ROLE", "CLIENTE"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cancha de prueba")))
                .andExpect(content().string(containsString("</html>")));
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void clientPaginationPreservesStatusAndSize() throws Exception {
        when(reservations.findByClientId(1L)).thenReturn(new ArrayList<>(List.of(reservation, reservation, reservation)));
        mvc.perform(get("/cliente/reservas").param("estado", "PENDIENTE").param("size", "1").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/cliente/reservas?page=0&amp;size=1&amp;estado=PENDIENTE")))
                .andExpect(content().string(containsString("/cliente/reservas?page=2&amp;size=1&amp;estado=PENDIENTE")))
                .andExpect(content().string(not(containsString("q=PENDIENTE"))))
                .andExpect(content().string(containsString("</html>")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminEmptyListRenders() throws Exception {
        when(reservations.findAllPaginated(0, 15)).thenReturn(new PageImpl<>(List.of()));
        mvc.perform(get("/admin/reservas"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Listado de Reservas")))
                .andExpect(content().string(containsString("</html>")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPaginationRendersFirstMiddleAndLastPage() throws Exception {
        for (int page : new int[]{0, 5, 11}) {
            when(reservations.findAllPaginated(page, 1))
                    .thenReturn(new PageImpl<>(List.of(reservation), PageRequest.of(page, 1), 12));
            mvc.perform(get("/admin/reservas").param("page", String.valueOf(page)).param("size", "1"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Cancha de prueba")))
                    .andExpect(content().string(containsString("fqs-page-num active")))
                    .andExpect(content().string(containsString("</html>")));
        }
    }

    @Configuration
    @EnableWebMvc
    static class Config {
        @Bean CustomUserDetailsService userDetailsService() { return mock(CustomUserDetailsService.class); }
        @Bean UserService userService() { return mock(UserService.class); }
        @Bean ReservationService reservationService() { return mock(ReservationService.class); }
        @Bean CourtService courtService() { return mock(CourtService.class); }
        @Bean AvailabilityService availabilityService() { return mock(AvailabilityService.class); }

        @Bean SpringResourceTemplateResolver templateResolver() {
            SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
            resolver.setPrefix("classpath:/templates/");
            resolver.setSuffix(".html");
            resolver.setCharacterEncoding("UTF-8");
            return resolver;
        }

        @Bean SpringTemplateEngine templateEngine(SpringResourceTemplateResolver resolver) {
            SpringTemplateEngine engine = new SpringTemplateEngine();
            engine.setTemplateResolver(resolver);
            engine.addDialect(new SpringSecurityDialect());
            return engine;
        }

        @Bean ThymeleafViewResolver viewResolver(SpringTemplateEngine engine) {
            ThymeleafViewResolver resolver = new ThymeleafViewResolver();
            resolver.setTemplateEngine(engine);
            resolver.setCharacterEncoding("UTF-8");
            return resolver;
        }
    }
}
