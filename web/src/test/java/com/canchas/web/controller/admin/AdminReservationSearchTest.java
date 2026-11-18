package com.canchas.web.controller.admin;

import com.canchas.web.model.*;
import com.canchas.web.repository.*;
import com.canchas.web.service.ReservationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class AdminReservationSearchTest {
    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired CourtRepository courts;
    @Autowired ReservationRepository reservations;
    @Autowired ReservationService service;

    @Test
    void initialPageDoesNotQueryReservations() {
        ReservationService mocked = mock(ReservationService.class);
        var controller = new AdminReservationController(mocked, null, null);
        assertEquals("admin/reservas/lista", controller.listReservations(new org.springframework.ui.ConcurrentModel()));
        verifyNoInteractions(mocked);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void searchReturnsTenNewestReservationsByDniAndNames() throws Exception {
        User client = new User();
        String suffix = UUID.randomUUID().toString().replaceAll("[0-9-]", "a");
        String surname = "Peña" + suffix;
        client.setUsername("search" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));
        client.setFullName("María Álvarez " + surname);
        String dni;
        do { dni = String.format("%08d", new Random().nextInt(100000000)); } while (users.existsByDni(dni));
        client.setDni(dni);
        client.setEmail(client.getUsername() + "@gmail.com");
        client.setPassword("encoded-test-password");
        client.setRegisteredAt(LocalDate.now());
        client.setRoles(new HashSet<>(Set.of(roles.findByName("ROLE_CLIENTE").orElseThrow())));
        client = users.saveAndFlush(client);
        Court court = new Court();
        court.setName("Cancha prueba búsqueda"); court.setType(Court.CourtType.NORMAL);
        court.setDescription("Cancha para probar búsqueda de reservas"); court.setHourlyRate(new BigDecimal("40.00"));
        court = courts.saveAndFlush(court);
        Long newest = null;
        for (int i = 0; i < 12; i++) {
            Reservation reservation = new Reservation();
            reservation.setClient(client); reservation.setCourt(court);
            reservation.setReservationDate(LocalDate.now().minusDays(i));
            reservation.setStartTime(LocalTime.of(10, 0)); reservation.setEndTime(LocalTime.of(11, 0));
            reservation.setStatus(Reservation.ReservationStatus.values()[i % 4]);
            reservation.setPaymentMethod(Reservation.PaymentMethod.EFECTIVO);
            reservation.setAppliedBasePrice(new BigDecimal("40.00")); reservation.setTotalAmount(new BigDecimal("40.00"));
            reservation = reservations.saveAndFlush(reservation);
            if (i == 0) newest = reservation.getId();
        }
        var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        mvc.perform(get("/admin/reservas")).andExpect(status().isOk())
                .andExpect(content().string(containsString("<tbody id=\"clientSearchResults\"></tbody>")));
        for (var criterion : Map.of("DNI", dni, "NOMBRE", surname).entrySet()) {
            mvc.perform(get("/admin/reservas/buscar").param("criterio", criterion.getKey()).param("valor", criterion.getValue()))
                    .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(10)))
                    .andExpect(jsonPath("$[0].id").value(newest))
                    .andExpect(jsonPath("$[0].dni").value(dni))
                    .andExpect(jsonPath("$[0].courtName").value(court.getName()));
        }
        String dniPrefix = dni.substring(0, 7);
        var prefix = service.searchByClient("DNI", dniPrefix);
        assertTrue(prefix.size() <= 10);
        assertTrue(prefix.stream().allMatch(r -> r.dni().startsWith(dniPrefix)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void invalidAndEmptySearchValuesAreHandledByServer() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        for (var invalid : List.of(new String[]{"EMAIL", "a"}, new String[]{"DNI", "123456789"},
                new String[]{"DNI", "12 3"}, new String[]{"NOMBRE", "Ana9"}, new String[]{"NOMBRE", "%"})) {
            mvc.perform(get("/admin/reservas/buscar").param("criterio", invalid[0]).param("valor", invalid[1]))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.valor").exists());
        }
        mvc.perform(get("/admin/reservas/buscar").param("criterio", "DNI").param("valor", ""))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }
}
