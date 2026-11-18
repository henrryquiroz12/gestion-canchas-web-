package com.canchas.web.service;

import com.canchas.web.dto.ReservationRequestDTO;
import com.canchas.web.exception.ResourceNotFoundException;
import com.canchas.web.model.*;
import com.canchas.web.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AdminPanelPersistenceTest {
    @Autowired CourtService courts;
    @Autowired CourtRepository courtRepository;
    @Autowired UserService users;
    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired ReservationRepository reservations;
    @Autowired ReservationService reservationService;
    @Autowired EntityManager em;
    @Autowired org.springframework.web.context.WebApplicationContext context;

    @Test
    @org.springframework.security.test.context.support.WithMockUser(roles = "ADMIN")
    void clientPageRendersEmptyAndCourtPageShowsSaveAndCancel() throws Exception {
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity()).build();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/admin/clientes"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(
                        org.hamcrest.Matchers.containsString("<tbody id=\"clientSearchResults\"></tbody>")));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/admin/canchas"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(
                        org.hamcrest.Matchers.containsString("id=\"btnGuardarCancha\">Guardar</button>")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(
                        org.hamcrest.Matchers.containsString(">Cancelar</button>")));
    }

    private User client(String names, String surname, String dni) {
        User user = new User();
        user.setUsername("test" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));
        user.setPassword("encoded-test-password");
        user.setFullName(names + " Peña " + surname);
        user.setDni(dni);
        user.setEmail(user.getUsername() + "@gmail.com");
        user.setPhoneNumber("912345678");
        user.setRegisteredAt(LocalDate.now());
        user.setRoles(new HashSet<>(Set.of(roleRepository.findByName("ROLE_CLIENTE").orElseThrow())));
        user.setActive(true);
        return userRepository.saveAndFlush(user);
    }

    @Test
    void searchesAreLimitedInDatabaseAndCanBeNarrowedByDniOrSurname() {
        String prefix;
        do {
            prefix = String.valueOf(100000 + new Random().nextInt(900000));
        } while (!userRepository.searchClientsByDniPrefix(prefix, PageRequest.of(0, 1)).isEmpty());
        String surname = "Ñ" + UUID.randomUUID().toString().replaceAll("[0-9-]", "a");
        Long selected = null;
        for (int i = 0; i < 12; i++) {
            User user = client(i == 0 ? "Álvaro" : "María", surname, prefix + String.format("%02d", i));
            if (i == 0) selected = user.getId();
        }
        em.clear();
        assertEquals(10, users.searchClients("DNI", prefix).size());
        var exact = users.searchClients("DNI", prefix + "00");
        assertEquals(1, exact.size());
        assertEquals(selected, exact.getFirst().id());
        assertEquals(10, users.searchClients("NOMBRE", surname).size());
        var narrowed = users.searchClients("NOMBRE", "Álvaro Peña " + surname);
        assertEquals(1, narrowed.size());
        assertEquals(selected, narrowed.getFirst().id());
    }

    @Test
    void savingEditableFieldsAndDisablingSurviveReloadWithoutChangingReservations() {
        Court court = new Court();
        court.setName("Cancha prueba administración");
        court.setType(Court.CourtType.NORMAL);
        court.setHourlyRate(new BigDecimal("40.00"));
        court.setDescription("Descripción inicial de la cancha de prueba");
        court = courts.create(court);
        Long id = court.getId();
        User client = client("María", "Prueba", null);
        List<Long> reservationIds = new ArrayList<>();
        for (var status : List.of(Reservation.ReservationStatus.COMPLETADO, Reservation.ReservationStatus.CONFIRMADO)) {
            Reservation reservation = new Reservation();
            reservation.setCourt(court);
            reservation.setClient(client);
            reservation.setReservationDate(LocalDate.now().plusDays(status == Reservation.ReservationStatus.COMPLETADO ? -1 : 2));
            reservation.setStartTime(LocalTime.of(12, 0));
            reservation.setEndTime(LocalTime.of(13, 0));
            reservation.setPaymentMethod(Reservation.PaymentMethod.EFECTIVO);
            reservation.setAppliedBasePrice(new BigDecimal("40.00"));
            reservation.setTotalAmount(new BigDecimal("40.00"));
            reservation.setStatus(status);
            reservationIds.add(reservations.saveAndFlush(reservation).getId());
        }
        Court edit = new Court();
        edit.setHourlyRate(new BigDecimal("55.00"));
        edit.setDescription("Descripción editada que debe persistirse");
        courts.update(id, edit);
        courts.changeAvailability(id, false);
        em.flush();
        em.clear(); // Simulate a fresh load from the database.
        Court reloaded = courts.findById(id);
        assertFalse(reloaded.isAvailable());
        assertEquals(0, new BigDecimal("55.00").compareTo(reloaded.getHourlyRate()));
        assertEquals(edit.getDescription(), reloaded.getDescription());
        assertTrue(courts.findAvailable().stream().noneMatch(c -> c.getId().equals(id)));
        assertEquals(Reservation.ReservationStatus.COMPLETADO, reservations.findById(reservationIds.get(0)).orElseThrow().getStatus());
        assertEquals(Reservation.ReservationStatus.CONFIRMADO, reservations.findById(reservationIds.get(1)).orElseThrow().getStatus());
        for (Long reservationId : reservationIds) {
            assertEquals(0, new BigDecimal("40.00").compareTo(reservations.findById(reservationId).orElseThrow().getAppliedBasePrice()));
        }
        ReservationRequestDTO request = new ReservationRequestDTO();
        request.setCourtId(id);
        assertThrows(ResourceNotFoundException.class, () -> reservationService.create(request, client.getId()));
    }
}
