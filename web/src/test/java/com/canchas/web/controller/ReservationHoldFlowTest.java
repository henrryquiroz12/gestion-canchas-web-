package com.canchas.web.controller;

import com.canchas.web.dto.*;
import com.canchas.web.model.*;
import com.canchas.web.repository.*;
import com.canchas.web.security.VisitorHoldSession;
import com.canchas.web.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest
@Import(ReservationHoldFlowTest.TestTime.class)
class ReservationHoldFlowTest {
    static class MutableClock extends Clock {
        final AtomicReference<Instant> instant = new AtomicReference<>();
        public ZoneId getZone() { return ZoneId.of("America/Lima"); }
        public Clock withZone(ZoneId zone) { return Clock.fixed(instant(), zone); }
        public Instant instant() { return instant.get(); }
        void advance(Duration duration) { instant.updateAndGet(i -> i.plus(duration)); }
    }
    @TestConfiguration
    static class TestTime {
        @Bean @Primary MutableClock testClock() {
            MutableClock clock = new MutableClock();
            clock.instant.set(LocalDate.now().plusDays(30).atTime(8, 0).atZone(clock.getZone()).toInstant());
            return clock;
        }
    }
    @Autowired WebApplicationContext context;
    @Autowired MutableClock clock;
    @Autowired ReservationHoldService holds;
    @Autowired AvailabilityService availability;
    @Autowired ReservationService reservations;
    @Autowired CourtBlockService blocks;
    @Autowired BusinessConfigurationService configuration;
    @Autowired CourtRepository courts;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    MockMvc mvc;
    MockHttpSession first, second;
    Long courtId;
    User client;
    LocalDate day;
    LocalTime time = LocalTime.of(10, 0);
    final List<Long> createdUsers = new ArrayList<>();

    @BeforeEach
    void setup() {
        clock.instant.set(LocalDate.now().plusDays(30).atTime(8, 0).atZone(clock.getZone()).toInstant());
        day = LocalDate.now(clock).plusDays(1);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        first = new MockHttpSession(); second = new MockHttpSession();
        Court court = new Court();
        court.setName("Cancha prueba retenciones " + UUID.randomUUID().toString().substring(0, 8));
        court.setDescription("Cancha temporal para verificar retenciones concurrentes");
        court.setType(Court.CourtType.NORMAL);
        court.setHourlyRate(new BigDecimal("40.00")); court.setAvailable(true);
        courtId = courts.saveAndFlush(court).getId();
        client = createClient();
    }
    User createClient() {
        User user = new User();
        user.setUsername("hold" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));
        user.setFullName("María Peña Prueba"); user.setEmail(user.getUsername() + "@gmail.com");
        user.setPassword(encoder.encode("Password123!"));
        user.setPhoneNumber("912345678"); user.setRegisteredAt(LocalDate.now()); user.setActive(true);
        user.setRoles(new HashSet<>(Set.of(roles.findByName("ROLE_CLIENTE").orElseThrow())));
        user = users.saveAndFlush(user); createdUsers.add(user.getId()); return user;
    }
    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM retenciones_horario WHERE court_id = ?", courtId);
        jdbc.update("DELETE FROM alquileres WHERE cancha_id = ?", courtId);
        jdbc.update("DELETE FROM bloqueo_cancha WHERE cancha_id = ?", courtId);
        courts.deleteById(courtId);
        for (Long id : createdUsers) users.deleteById(id);
    }
    ReservationHoldView start(MockHttpSession session) throws Exception {
        mvc.perform(post("/retenciones").session(session).with(csrf()).param("courtId", courtId.toString())
                .param("date", day.toString()).param("startTime", time.toString()))
                .andExpect(redirectedUrl("/registro"));
        return holds.current(VisitorHoldSession.existingOwner(session));
    }
    AvailableSlotDTO.SlotStatus slot() {
        return availability.getAvailability(courtId, day).stream().filter(s -> s.getStartTime().equals(time))
                .findFirst().orElseThrow().getStatus();
    }
    ReservationRequestDTO request() {
        ReservationRequestDTO dto = new ReservationRequestDTO();
        dto.setCourtId(courtId); dto.setClientId(client.getId()); dto.setReservationDate(day);
        dto.setStartTime(time); dto.setPaymentMethod(Reservation.PaymentMethod.EFECTIVO); return dto;
    }

    @Test
    void twoSessionsRespectCancellationExpiryAndNoRenewalOnReload() throws Exception {
        mvc.perform(get("/canchas/" + courtId).session(first)).andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"holdStartForm\"")));
        var held = start(first);
        assertEquals(900, held.remainingSeconds());
        assertEquals(AvailableSlotDTO.SlotStatus.OCUPADO, slot());
        mvc.perform(get("/api/canchas/" + courtId + "/disponibilidad").session(second).param("fecha", day.toString()))
                .andExpect(status().isOk()).andExpect(content().string(containsString("OCUPADO")));
        mvc.perform(post("/retenciones").session(second).with(csrf()).param("courtId", courtId.toString())
                .param("date", day.toString()).param("startTime", time.toString()))
                .andExpect(redirectedUrl("/canchas/" + courtId)).andExpect(flash().attributeExists("error"));
        clock.advance(Duration.ofMinutes(1));
        mvc.perform(get("/registro").session(first)).andExpect(status().isOk())
                .andExpect(content().string(containsString(held.token())));
        var retry = start(first);
        assertEquals(held.expiresAt(), retry.expiresAt());
        assertEquals(840, retry.remainingSeconds());
        assertThrows(RuntimeException.class, () -> reservations.create(request(), null));
        mvc.perform(post("/retenciones/cancelar").session(second).with(csrf()).param("token", held.token()))
                .andExpect(status().isConflict());
        mvc.perform(post("/retenciones/cancelar").session(first).with(csrf()).param("token", held.token()))
                .andExpect(status().isOk());
        assertEquals(AvailableSlotDTO.SlotStatus.DISPONIBLE, slot());
        var secondHold = start(second);
        clock.advance(Duration.ofMinutes(15));
        mvc.perform(get("/retenciones").session(second)).andExpect(jsonPath("$.status").value("EXPIRED"));
        mvc.perform(get("/retenciones/continuar").session(second).with(user(client.getUsername()).roles("CLIENTE")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Volver a consultar horarios")));
        assertEquals(AvailableSlotDTO.SlotStatus.DISPONIBLE, slot());
        assertThrows(RuntimeException.class, () -> holds.confirm(VisitorHoldSession.existingOwner(second), secondHold.token(), client.getId(), Reservation.PaymentMethod.EFECTIVO));
        var newHold = start(first);
        assertNotEquals(held.token(), newHold.token());
    }

    @Test
    void registrationLoginAndConcurrentConfirmationCreateExactlyOnePendingReservation() throws Exception {
        var hold = start(first);
        String username = "reg" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String dni;
        do { dni = String.format("%08d", new Random().nextInt(100000000)); } while (users.existsByDni(dni));
        mvc.perform(post("/registro").session(first).with(csrf()).param("dni", dni).param("username", username)
                .param("fullName", "María Peña Prueba").param("email", username + "@gmail.com")
                .param("phoneNumber", "912345678").param("department", "LIMA").param("province", "Lima")
                .param("gender", "FEMALE").param("password", "Password123!").param("confirmPassword", "Password123!"))
                .andExpect(redirectedUrl("/login?registroExitoso=true"));
        User registered = users.findByUsername(username).orElseThrow(); createdUsers.add(registered.getId());
        mvc.perform(get("/login").session(first)).andExpect(content().string(containsString(hold.token())));
        String originalSession = first.getId();
        mvc.perform(post("/login").session(first).with(csrf()).param("username", username).param("password", "Password123!"))
                .andExpect(redirectedUrl("/retenciones/continuar"));
        assertNotEquals(originalSession, first.getId());
        assertEquals(hold.expiresAt(), holds.current(VisitorHoldSession.existingOwner(first)).expiresAt());
        mvc.perform(get("/retenciones/continuar").session(first)).andExpect(status().isOk())
                .andExpect(content().string(containsString("/retenciones/confirmar")));
        try (var executor = Executors.newFixedThreadPool(2)) {
            CountDownLatch ready = new CountDownLatch(2), go = new CountDownLatch(1);
            Callable<String> confirm = () -> {
                ready.countDown(); go.await();
                return mvc.perform(post("/retenciones/confirmar").session(first).with(csrf())
                        .param("token", hold.token()).param("metodoPago", "EFECTIVO"))
                        .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
            };
            Future<String> one = executor.submit(confirm), two = executor.submit(confirm);
            assertTrue(ready.await(5, TimeUnit.SECONDS)); go.countDown();
            assertEquals(one.get(15, TimeUnit.SECONDS), two.get(15, TimeUnit.SECONDS));
        }
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM alquileres WHERE cancha_id = ?", Integer.class, courtId));
        assertEquals("PENDIENTE", jdbc.queryForObject("SELECT estado FROM alquileres WHERE cancha_id = ?", String.class, courtId));
        assertEquals(AvailableSlotDTO.SlotStatus.OCUPADO, slot());
    }

    @Test
    void simultaneousVisitorsCannotBothHoldTheSlot() throws Exception {
        String ownerA = VisitorHoldSession.owner(first), ownerB = VisitorHoldSession.owner(second);
        try (var executor = Executors.newFixedThreadPool(2)) {
            CountDownLatch go = new CountDownLatch(1);
            List<Future<Boolean>> results = new ArrayList<>();
            for (String owner : List.of(ownerA, ownerB)) results.add(executor.submit(() -> {
                go.await();
                try { holds.start(owner, courtId, day, time); return true; }
                catch (RuntimeException ex) { return false; }
            }));
            go.countDown();
            int winners = 0;
            for (var result : results) if (result.get(15, TimeUnit.SECONDS)) winners++;
            assertEquals(1, winners);
        }
    }

    @Test
    void simultaneousHoldAndDirectReservationHaveOnlyOneWinner() throws Exception {
        String owner = VisitorHoldSession.owner(first);
        try (var executor = Executors.newFixedThreadPool(2)) {
            CountDownLatch ready = new CountDownLatch(2), go = new CountDownLatch(1);
            Callable<Boolean> hold = () -> {
                ready.countDown(); go.await();
                try { holds.start(owner, courtId, day, time); return true; }
                catch (RuntimeException ex) { return false; }
            };
            Callable<Boolean> book = () -> {
                ready.countDown(); go.await();
                try { reservations.create(request(), null); return true; }
                catch (RuntimeException ex) { return false; }
            };
            var held = executor.submit(hold); var booked = executor.submit(book);
            assertTrue(ready.await(5, TimeUnit.SECONDS)); go.countDown();
            assertNotEquals(held.get(15, TimeUnit.SECONDS), booked.get(15, TimeUnit.SECONDS));
            int bookings = jdbc.queryForObject("SELECT COUNT(*) FROM alquileres WHERE cancha_id = ?", Integer.class, courtId);
            int activeHolds = jdbc.queryForObject("SELECT COUNT(*) FROM retenciones_horario WHERE court_id = ? AND status = 'ACTIVE'", Integer.class, courtId);
            assertEquals(1, bookings + activeHolds);
        }
    }

    @Test
    void simultaneousRequestsInOneSessionCannotHoldTwoSlots() throws Exception {
        String owner = VisitorHoldSession.owner(first);
        try (var executor = Executors.newFixedThreadPool(2)) {
            CountDownLatch go = new CountDownLatch(1);
            List<Future<Boolean>> results = new ArrayList<>();
            for (LocalTime slot : List.of(time, time.plusHours(1))) results.add(executor.submit(() -> {
                go.await();
                try { holds.start(owner, courtId, day, slot); return true; }
                catch (RuntimeException ex) { return false; }
            }));
            go.countDown();
            int winners = 0;
            for (var result : results) if (result.get(15, TimeUnit.SECONDS)) winners++;
            assertEquals(1, winners);
        }
    }

    @Test
    void holdsRespectAdministrativeBlocksOperatingHoursAndOneActivePerSession() throws Exception {
        blocks.blockSlot(courtId, day, time, "Mantenimiento");
        assertThrows(RuntimeException.class, () -> holds.start(VisitorHoldSession.owner(first), courtId, day, time));
        blocks.unblockSlot(courtId, day, time);
        var hold = start(first);
        assertThrows(RuntimeException.class, () -> holds.start(VisitorHoldSession.existingOwner(first), courtId, day, time.plusHours(1)));
        assertThrows(RuntimeException.class, () -> blocks.blockSlot(courtId, day, time, "Mantenimiento"));
        assertThrows(RuntimeException.class, () -> configuration.saveSpecialOperatingHours(day, LocalTime.of(7, 0), LocalTime.MIDNIGHT, true, "Cerrado"));
        holds.cancel(VisitorHoldSession.existingOwner(first), hold.token());
        assertThrows(RuntimeException.class, () -> holds.start(VisitorHoldSession.existingOwner(first), courtId, day, time.plusMinutes(30)));
    }
}
