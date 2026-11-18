package com.canchas.web.controller;

import com.canchas.web.dto.AvailableSlotDTO;
import com.canchas.web.dto.ReservationRequestDTO;
import com.canchas.web.exception.OperationNotAllowedException;
import com.canchas.web.model.Court;
import com.canchas.web.model.Reservation;
import com.canchas.web.model.User;
import com.canchas.web.service.AvailabilityService;
import com.canchas.web.service.CourtService;
import com.canchas.web.service.ReservationService;
import com.canchas.web.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ClientReservationControllerUnitTest {

    @Mock
    private CourtService courtService;
    @Mock
    private AvailabilityService availabilityService;
    @Mock
    private UserService userService;
    @Mock
    private ReservationService reservationService;
    @Mock
    private Authentication authentication;
    @Mock
    private RedirectAttributes redirectAttributes;
    @Mock
    private BindingResult bindingResult;

    @InjectMocks
    private ClientReservationController controller;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void GET_Confirmation_AvailableSlot_ShouldReturnView() {
        LocalDate date = LocalDate.of(2026, 10, 5);
        LocalTime time = LocalTime.of(19, 0);

        Court c = new Court();
        c.setId(1L);
        c.setAvailable(true);
        when(courtService.findById(1L)).thenReturn(c);

        AvailableSlotDTO slot = new AvailableSlotDTO();
        slot.setStartTime(time);
        slot.setStatus(AvailableSlotDTO.SlotStatus.DISPONIBLE);
        when(availabilityService.getAvailability(1L, date)).thenReturn(Collections.singletonList(slot));

        Model model = new ConcurrentModel();
        String view = controller.showConfirmationForm(1L, date, time, model, redirectAttributes);

        assertEquals("cliente/reservas/confirmar", view);
        assertEquals(slot, model.getAttribute("slot"));
    }

    @Test
    public void GET_Confirmation_OccupiedSlot_ShouldRedirectWithError() {
        LocalDate date = LocalDate.of(2026, 10, 5);
        LocalTime time = LocalTime.of(19, 0);

        Court c = new Court();
        c.setId(1L);
        c.setAvailable(true);
        when(courtService.findById(1L)).thenReturn(c);

        AvailableSlotDTO slot = new AvailableSlotDTO();
        slot.setStartTime(time);
        slot.setStatus(AvailableSlotDTO.SlotStatus.OCUPADO);
        when(availabilityService.getAvailability(1L, date)).thenReturn(Collections.singletonList(slot));

        Model model = new ConcurrentModel();
        String view = controller.showConfirmationForm(1L, date, time, model, redirectAttributes);

        assertEquals("redirect:/canchas/1", view);
        verify(redirectAttributes).addFlashAttribute(eq("error"), anyString());
    }

    @Test
    public void POST_CreateReservation_ShouldCreateAndRedirect() {
        ReservationRequestDTO req = new ReservationRequestDTO();
        req.setCourtId(1L);

        User u = new User();
        u.setId(5L);
        when(authentication.getName()).thenReturn("cliente");
        when(userService.findByUsername("cliente")).thenReturn(u);
        when(bindingResult.hasErrors()).thenReturn(false);

        Reservation res = new Reservation();
        res.setId(99L);
        when(reservationService.create(any(), isNull())).thenReturn(res);

        String view = controller.createReservation(req, bindingResult, authentication, redirectAttributes);

        assertEquals("redirect:/cliente/reservas/99?creada=true", view);
        assertEquals(5L, req.getClientId());
    }

    @Test
    public void GET_ViewReservation_AuthorizedClient_ShouldReturnView() {
        User clientAuth = new User();
        clientAuth.setId(5L);
        when(authentication.getName()).thenReturn("cliente");
        when(userService.findByUsername("cliente")).thenReturn(clientAuth);

        Reservation res = new Reservation();
        res.setId(99L);
        res.setClient(clientAuth);
        res.setReservationDate(LocalDate.of(2026, 12, 1));
        res.setStartTime(LocalTime.of(15, 0));
        res.setStatus(Reservation.ReservationStatus.PENDIENTE);
        when(reservationService.findById(99L)).thenReturn(res);

        Model model = new ConcurrentModel();
        String view = controller.viewReservation(99L, authentication, model, redirectAttributes);

        assertEquals("cliente/reservas/detalle", view);
    }

    @Test
    public void GET_ViewReservation_UnauthorizedClient_ShouldThrowException() {
        User clientA = new User();
        clientA.setId(10L);

        User clientB = new User();
        clientB.setId(20L);

        when(authentication.getName()).thenReturn("clienteB");
        when(userService.findByUsername("clienteB")).thenReturn(clientB);

        Reservation res = new Reservation();
        res.setId(99L);
        res.setClient(clientA);
        when(reservationService.findById(99L)).thenReturn(res);

        assertThrows(OperationNotAllowedException.class, () -> {
            controller.viewReservation(99L, authentication, new ConcurrentModel(), redirectAttributes);
        });
    }

    @Test
    public void POST_CancelReservation_UnauthorizedClient_ShouldThrowException() {
        User clientA = new User();
        clientA.setId(10L);

        User clientB = new User();
        clientB.setId(20L);

        when(authentication.getName()).thenReturn("clienteB");
        when(userService.findByUsername("clienteB")).thenReturn(clientB);

        Reservation res = new Reservation();
        res.setId(99L);
        res.setClient(clientA);
        when(reservationService.findById(99L)).thenReturn(res);

        assertThrows(OperationNotAllowedException.class, () -> {
            controller.cancelReservation(99L, authentication, redirectAttributes);
        });
    }
}
