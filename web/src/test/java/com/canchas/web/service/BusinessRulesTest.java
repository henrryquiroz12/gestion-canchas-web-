package com.canchas.web.service;

import com.canchas.web.dto.AvailableSlotDTO;
import com.canchas.web.dto.EffectiveOperatingHoursDTO;
import com.canchas.web.dto.ReservationRequestDTO;
import com.canchas.web.exception.*;
import com.canchas.web.model.*;
import com.canchas.web.repository.*;
import com.canchas.web.service.impl.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.*;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BusinessRulesTest {

    @Mock private CourtRepository courtRepository;
    @Mock private ReservationHoldRepository holdRepository;
    @Mock private ReservationRepository reservationRepository;
    @Mock private CourtBlockRepository courtBlockRepository;
    @Mock private UserService userService;
    @Mock private BusinessConfigurationRepository configurationRepository;
    @Mock private DailyOperatingHoursRepository dailyOperatingHoursRepository;
    @Mock private InternalReceiptRepository receiptRepository;
    @Mock private CourtService courtService;

    @Mock private Clock clock;

    private ReservationServiceImpl reservationService;
    private BusinessConfigurationServiceImpl configurationService;
    private AvailabilityServiceImpl availabilityService;
    private InternalReceiptServiceImpl receiptService;
    private CourtBlockServiceImpl courtBlockService;

    private final LocalDate hoy = LocalDate.of(2026, 10, 5);
    private final LocalTime ahora = LocalTime.of(12, 0);

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(LocalDateTime.of(hoy, ahora).atZone(ZoneId.of("America/Lima")).toInstant(), ZoneId.of("America/Lima"));
        lenient().when(clock.instant()).thenReturn(fixedClock.instant());
        lenient().when(clock.getZone()).thenReturn(fixedClock.getZone());

        lenient().when(courtService.lockById(anyLong())).thenAnswer(inv -> courtService.findById(inv.getArgument(0)));
        configurationService = new BusinessConfigurationServiceImpl(configurationRepository, dailyOperatingHoursRepository, reservationRepository, courtRepository, holdRepository, clock);
        courtBlockService = new CourtBlockServiceImpl(courtBlockRepository, reservationRepository, courtService, configurationService, clock, holdRepository);
        availabilityService = new AvailabilityServiceImpl(courtService, configurationService, courtBlockService, reservationRepository, clock, holdRepository);
        reservationService = new ReservationServiceImpl(reservationRepository, courtRepository, 
                new ReservationSlotRules(configurationService, reservationRepository, courtBlockRepository, holdRepository, clock), userService, configurationService, clock);
        receiptService = new InternalReceiptServiceImpl(receiptRepository, reservationService);
    }

    private BusinessConfiguration mockConfig() {
        BusinessConfiguration config = new BusinessConfiguration();
        config.setDefaultOpeningTime(LocalTime.of(7, 0));
        config.setDefaultClosingTime(LocalTime.MIDNIGHT);
        config.setLightingStartTime(LocalTime.of(18, 0));
        config.setLightingSurcharge(new BigDecimal("20.00"));
        lenient().when(configurationRepository.findById((short) 1)).thenReturn(Optional.of(config));
        return config;
    }

    private Court mockCourt(Long id, BigDecimal price, Court.CourtType type, boolean available) {
        Court c = new Court();
        c.setId(id);
        c.setHourlyRate(price);
        c.setType(type);
        c.setAvailable(available);
        lenient().when(courtRepository.findByIdForUpdate(id)).thenReturn(Optional.of(c));
        lenient().when(courtService.findById(id)).thenReturn(c);
        return c;
    }

    private User mockClient() {
        User u = new User();
        u.setId(1L);
        u.setRole(User.Role.CLIENTE);
        lenient().when(userService.findById(1L)).thenReturn(u);
        return u;
    }

    // 1. Cancha normal 17:00 -> base 40, ilum 0
    @Test
    void test1_Normal17BaseLighting0() {
        mockConfig();
        BigDecimal surcharge = configurationService.getLightingSurcharge(LocalTime.of(17, 0));
        assertEquals(BigDecimal.ZERO, surcharge);
    }

    // 2. Cancha normal 18:00 -> base 40, ilum 20
    @Test
    void test2_Normal18BaseLighting20() {
        mockConfig();
        BigDecimal surcharge = configurationService.getLightingSurcharge(LocalTime.of(18, 0));
        assertEquals(new BigDecimal("20.00"), surcharge);
    }

    // 3. Grande 18:00 -> base 60, ilum 20
    @Test
    void test3_Grande18Base60Lighting20() {
        mockConfig();
        Court c = mockCourt(1L, new BigDecimal("60.00"), Court.CourtType.GRANDE, true);
        BigDecimal surcharge = configurationService.getLightingSurcharge(LocalTime.of(18, 0));
        assertEquals(new BigDecimal("20.00"), surcharge);
        assertEquals(new BigDecimal("60.00"), c.getHourlyRate());
    }

    // 4. 22:00-23:00 con cierre 23:00 (excepción hoy) -> valido
    @Test
    void test4_Slot22Closing23Valid() {
        BusinessConfiguration config = mockConfig();
        config.setDefaultClosingTime(LocalTime.of(23, 0));
        mockCourt(1L, new BigDecimal("40.00"), Court.CourtType.NORMAL, true);
        List<AvailableSlotDTO> slots = availabilityService.getAvailability(1L, hoy);
        assertTrue(slots.stream().anyMatch(s -> s.getStartTime().equals(LocalTime.of(22, 0))));
    }

    // 5. 23:00-00:00 con cierre habitual 00:00 -> valido
    @Test
    void test5_Slot23Closing00Valid() {
        mockConfig();
        mockCourt(1L, new BigDecimal("40.00"), Court.CourtType.NORMAL, true);
        List<AvailableSlotDTO> slots = availabilityService.getAvailability(1L, hoy);
        assertTrue(slots.stream().anyMatch(s -> s.getStartTime().equals(LocalTime.of(23, 0))));
    }

    // 6. 23:00 con excepción cierre 23:00 -> invalido (desaparece)
    @Test
    void test6_Slot23Closing23Invalid() {
        BusinessConfiguration config = mockConfig();
        config.setDefaultClosingTime(LocalTime.of(23, 0));
        mockCourt(1L, new BigDecimal("40.00"), Court.CourtType.NORMAL, true);
        List<AvailableSlotDTO> slots = availabilityService.getAvailability(1L, hoy);
        assertFalse(slots.stream().anyMatch(s -> s.getStartTime().equals(LocalTime.of(23, 0))));
    }

    // 7. 07:00 -> 02:00 horario invalido
    @Test
    void test7_InvalidOperatingHours02AM() {
        assertThrows(OperationNotAllowedException.class, () -> {
            configurationService.saveSpecialOperatingHours(hoy, LocalTime.of(7, 0), LocalTime.of(2, 0), false, "test");
        });
    }

    // 8. Dia cerrado: sin slots
    @Test
    void test8_ClosedDayNoSlots() {
        mockConfig();
        mockCourt(1L, new BigDecimal("40.00"), Court.CourtType.NORMAL, true);
        DailyOperatingHours override = new DailyOperatingHours();
        override.setClosed(true);
        when(dailyOperatingHoursRepository.findByDate(hoy)).thenReturn(Optional.of(override));
        List<AvailableSlotDTO> slots = availabilityService.getAvailability(1L, hoy);
        assertTrue(slots.isEmpty());
    }

    // 9, 10, 11. Estados y Slots
    @Test
    void test9_10_11_StatesAndSlots() {
        mockConfig();
        mockCourt(1L, new BigDecimal("40.00"), Court.CourtType.NORMAL, true);
        
        Reservation p = new Reservation(); p.setStartTime(LocalTime.of(14,0)); p.setStatus(Reservation.ReservationStatus.PENDIENTE);
        Reservation c = new Reservation(); c.setStartTime(LocalTime.of(15,0)); c.setStatus(Reservation.ReservationStatus.CONFIRMADO);
        
        when(reservationRepository.findByCourtIdAndReservationDateAndStatusIn(eq(1L), eq(hoy.plusDays(1)), any())).thenReturn(List.of(p, c));
        
        List<AvailableSlotDTO> slots = availabilityService.getAvailability(1L, hoy.plusDays(1));
        
        assertEquals(AvailableSlotDTO.SlotStatus.OCUPADO, slots.stream().filter(s -> s.getStartTime().equals(LocalTime.of(14,0))).findFirst().get().getStatus());
        assertEquals(AvailableSlotDTO.SlotStatus.OCUPADO, slots.stream().filter(s -> s.getStartTime().equals(LocalTime.of(15,0))).findFirst().get().getStatus());
        assertEquals(AvailableSlotDTO.SlotStatus.DISPONIBLE, slots.stream().filter(s -> s.getStartTime().equals(LocalTime.of(16,0))).findFirst().get().getStatus());
    }

    // 12. Bloqueo slot bloqueado
    @Test
    void test12_BlockHidesOrChangesState() {
        mockConfig();
        mockCourt(1L, new BigDecimal("40.00"), Court.CourtType.NORMAL, true);
        CourtBlock b = new CourtBlock();
        b.setStartTime(LocalTime.of(17,0));
        b.setActive(true);
        when(courtBlockRepository.findByCourtIdAndDateAndActiveTrue(1L, hoy.plusDays(1))).thenReturn(List.of(b));
        
        List<AvailableSlotDTO> slots = availabilityService.getAvailability(1L, hoy.plusDays(1));
        assertEquals(AvailableSlotDTO.SlotStatus.BLOQUEADO, slots.stream().filter(s -> s.getStartTime().equals(LocalTime.of(17,0))).findFirst().get().getStatus());
    }

    // 13. Cancha no disponible
    @Test
    void test13_UnavailableCourtNotReservable() {
        mockClient();
        mockConfig();
        mockCourt(1L, new BigDecimal("40.00"), Court.CourtType.NORMAL, false);
        
        ReservationRequestDTO req = new ReservationRequestDTO();
        req.setCourtId(1L); req.setReservationDate(hoy.plusDays(1)); req.setStartTime(LocalTime.of(15,0));
        
        assertThrows(ResourceNotFoundException.class, () -> reservationService.create(req, 1L));
    }

    // 14. Hora pasada no reservable
    @Test
    void test14_PastTimeRejected() {
        mockClient();
        mockConfig();
        mockCourt(1L, new BigDecimal("40.00"), Court.CourtType.NORMAL, true);
        
        ReservationRequestDTO req = new ReservationRequestDTO();
        req.setCourtId(1L); req.setReservationDate(hoy); req.setStartTime(LocalTime.of(10,0));
        
        assertThrows(InvalidDateException.class, () -> reservationService.create(req, 1L));
    }

    // 15. Fecha pasada
    @Test
    void test15_PastDateRejected() {
        mockClient();
        mockCourt(1L, new BigDecimal("40.00"), Court.CourtType.NORMAL, true);
        
        ReservationRequestDTO req = new ReservationRequestDTO();
        req.setCourtId(1L); req.setReservationDate(hoy.minusDays(1)); req.setStartTime(LocalTime.of(15,0));
        
        assertThrows(InvalidDateException.class, () -> reservationService.create(req, 1L));
    }

    // 16. Doble reserva rechazada
    @Test
    void test16_DoubleBookingRejected() {
        mockClient();
        mockConfig();
        mockCourt(1L, new BigDecimal("40.00"), Court.CourtType.NORMAL, true);
        when(reservationRepository.existsByCourtIdAndReservationDateAndStartTimeAndStatusIn(
                eq(1L), eq(hoy), eq(LocalTime.of(15,0)), any())).thenReturn(true);
                
        ReservationRequestDTO req = new ReservationRequestDTO();
        req.setCourtId(1L); req.setReservationDate(hoy); req.setStartTime(LocalTime.of(15,0));
        assertThrows(TimeSlotUnavailableException.class, () -> reservationService.create(req, 1L));
    }

    // 17, 18. Bloquear slot con reserva activa
    @Test
    void test17_18_BlockWithActiveReservation() {
        mockConfig();
        when(reservationRepository.existsByCourtIdAndReservationDateAndStartTimeAndStatusIn(
                eq(1L), eq(hoy), eq(LocalTime.of(15,0)), any())).thenReturn(true);
        assertThrows(OperationNotAllowedException.class, () -> courtBlockService.blockSlot(1L, hoy, LocalTime.of(15,0), "test"));
    }

    // 19, 20. Reducir/Cerrar horario con reserva
    @Test
    void test19_20_ModifyOperatingHoursWithReservation() {
        Reservation a = new Reservation();
        a.setStartTime(LocalTime.of(15,0)); a.setEndTime(LocalTime.of(16,0));
        when(reservationRepository.findByReservationDateAndStatusIn(eq(hoy), any())).thenReturn(List.of(a));
        
        assertThrows(OperationNotAllowedException.class, () -> 
            configurationService.saveSpecialOperatingHours(hoy, LocalTime.of(7,0), LocalTime.of(23,0), true, "test")
        );
        
        assertThrows(OperationNotAllowedException.class, () -> 
            configurationService.saveSpecialOperatingHours(hoy, LocalTime.of(7,0), LocalTime.of(14,0), false, "test")
        );
    }

    // 21. Snapshot precio
    @Test
    void test21_PriceSnapshot() {
        mockClient();
        mockConfig();
        mockCourt(1L, new BigDecimal("40.00"), Court.CourtType.NORMAL, true);
        ReservationRequestDTO req = new ReservationRequestDTO();
        req.setCourtId(1L); req.setReservationDate(hoy); req.setStartTime(LocalTime.of(15,0));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));
        
        Reservation result = reservationService.create(req, 1L);
        assertEquals(new BigDecimal("40.00"), result.getAppliedBasePrice());
    }

    // 22. Cálculo de total exclusivo: precio base + recargo iluminación (sin implementos)
    @Test
    void test22_CalculateTotalWithoutEquipment() {
        mockClient();
        mockConfig();
        mockCourt(1L, new BigDecimal("40.00"), Court.CourtType.NORMAL, true);
        when(reservationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ReservationRequestDTO req = new ReservationRequestDTO();
        req.setCourtId(1L);
        req.setReservationDate(hoy);
        req.setStartTime(LocalTime.of(19, 0)); // Turno noche: +S/20

        Reservation creado = reservationService.create(req, 1L);

        assertEquals(new BigDecimal("40.00"), creado.getAppliedBasePrice());
        assertEquals(new BigDecimal("20.00"), creado.getAppliedLightingSurcharge());
        assertEquals(new BigDecimal("60.00"), creado.getTotalAmount());
    }

    // 24, 25, 26, 27. Cancelacion y transiciones
    @Test
    void test24_27_Transitions() {
        Reservation a = new Reservation(); a.setId(1L); a.setStatus(Reservation.ReservationStatus.PENDIENTE);
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(a));
        when(reservationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        
        reservationService.changeStatus(1L, Reservation.ReservationStatus.CANCELADO);
        assertEquals(Reservation.ReservationStatus.CANCELADO, a.getStatus());
        
        reservationService.changeStatus(1L, Reservation.ReservationStatus.CANCELADO);
        assertEquals(Reservation.ReservationStatus.CANCELADO, a.getStatus());
        
        assertThrows(InvalidReservationStatusException.class, () -> reservationService.changeStatus(1L, Reservation.ReservationStatus.CONFIRMADO));
        
        a.setStatus(Reservation.ReservationStatus.COMPLETADO);
        assertThrows(InvalidReservationStatusException.class, () -> reservationService.changeStatus(1L, Reservation.ReservationStatus.PENDIENTE));
        assertThrows(InvalidReservationStatusException.class, () -> reservationService.changeStatus(1L, Reservation.ReservationStatus.CANCELADO));
    }

    // 28. Factura numero seguro
    @Test
    void test28_ReceiptNumber() {
        Reservation a = new Reservation(); a.setId(10L); a.setTotalAmount(new BigDecimal("100.00"));
        lenient().when(reservationRepository.findById(10L)).thenReturn(Optional.of(a));
        
        when(receiptRepository.saveAndFlush(any(InternalReceipt.class))).thenAnswer(i -> {
            InternalReceipt f = i.getArgument(0); f.setId(137L); return f;
        });
        when(receiptRepository.save(any(InternalReceipt.class))).thenAnswer(i -> i.getArgument(0));
        
        InternalReceipt f = receiptService.generate(10L);
        assertEquals("FQ-000137", f.getReceiptNumber());
    }

    // 29. Segunda factura
    @Test
    void test29_DuplicateReceipt() {
        Reservation a = new Reservation(); a.setId(10L);
        when(reservationRepository.findById(10L)).thenReturn(Optional.of(a));
        when(receiptRepository.findByReservationId(10L)).thenReturn(Optional.of(new InternalReceipt()));
        
        assertThrows(OperationNotAllowedException.class, () -> receiptService.generate(10L));
    }

    // 30. Configuracion inexistente
    @Test
    void test30_ConfigurationNotFound() {
        when(configurationRepository.findById((short)1)).thenReturn(Optional.empty());
        assertThrows(BusinessConfigurationNotFoundException.class, () -> configurationService.getConfiguration());
    }

    // 31. TEST BUG PRECIO LEGACY (NORMAL 18:00 -> 60)
    @Test
    void test31_LegacyPriceBug() {
        mockClient();
        mockConfig();
        mockCourt(1L, new BigDecimal("40.00"), Court.CourtType.NORMAL, true);
        
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));
        
        ReservationRequestDTO req = new ReservationRequestDTO();
        req.setCourtId(1L);
        req.setReservationDate(hoy.plusDays(1));
        req.setStartTime(LocalTime.of(18, 0));
        req.setPaymentMethod(Reservation.PaymentMethod.EFECTIVO);
        
        Reservation resultado = reservationService.create(req, 1L);
        
        assertEquals(new BigDecimal("40.00"), resultado.getAppliedBasePrice());
        assertEquals(new BigDecimal("20.00"), resultado.getAppliedLightingSurcharge());
        assertEquals(new BigDecimal("60.00"), resultado.getTotalAmount());
    }

    // 32. Bloqueo slot individual
    @Test
    void test32_SingleSlotBlock() {
        mockCourt(1L, new BigDecimal("40.00"), Court.CourtType.NORMAL, true);
        mockConfig();
        LocalDate manana = hoy.plusDays(1);
        when(courtBlockRepository.findByCourtIdAndDateAndStartTime(1L, manana, LocalTime.of(20, 0))).thenReturn(Optional.empty());
        when(courtBlockRepository.save(any(CourtBlock.class))).thenAnswer(i -> i.getArgument(0));

        CourtBlock b = courtBlockService.blockSlot(1L, manana, LocalTime.of(20, 0), "Mantenimiento");
        assertNotNull(b);
        assertEquals(LocalTime.of(20, 0), b.getStartTime());
        assertEquals("Mantenimiento", b.getReason());
        assertTrue(b.isActive());
    }

    // FASE 15.1: Verificación de rango 1 slot: 17:00 a 18:00
    @Test
    void test_RangeBlockOneSlot_17to18() {
        mockCourt(1L, new BigDecimal("40.00"), Court.CourtType.NORMAL, true);
        mockConfig();
        LocalDate manana = hoy.plusDays(1);
        when(courtBlockRepository.save(any(CourtBlock.class))).thenAnswer(i -> i.getArgument(0));
        when(reservationRepository.findByCourtIdAndReservationDateAndStatusIn(eq(1L), eq(manana), any())).thenReturn(Collections.emptyList());

        List<CourtBlock> slots = courtBlockService.blockRange(1L, manana, LocalTime.of(17, 0), LocalTime.of(18, 0), "Pintado");
        assertEquals(1, slots.size());
        assertEquals(LocalTime.of(17, 0), slots.get(0).getStartTime());
    }

    // FASE 15.1: Verificación de rango todo el horario: 07:00 a 00:00 (17 slots)
    @Test
    void test_RangeBlockAllHours_07to00() {
        mockCourt(1L, new BigDecimal("40.00"), Court.CourtType.NORMAL, true);
        mockConfig();
        LocalDate manana = hoy.plusDays(1);
        when(courtBlockRepository.save(any(CourtBlock.class))).thenAnswer(i -> i.getArgument(0));
        when(reservationRepository.findByCourtIdAndReservationDateAndStatusIn(eq(1L), eq(manana), any())).thenReturn(Collections.emptyList());

        List<CourtBlock> slots = courtBlockService.blockRange(1L, manana, LocalTime.of(7, 0), LocalTime.MIDNIGHT, "Mantenimiento general");
        assertEquals(17, slots.size());
        assertEquals(LocalTime.of(7, 0), slots.get(0).getStartTime());
        assertEquals(LocalTime.of(23, 0), slots.get(16).getStartTime());
    }

    // FASE 15.1: Verificación de rango inválido (hasta <= desde) -> rechazado
    @Test
    void test_RangeBlockInvalidRangeRejected() {
        mockCourt(1L, new BigDecimal("40.00"), Court.CourtType.NORMAL, true);
        mockConfig();
        LocalDate manana = hoy.plusDays(1);

        assertThrows(IllegalArgumentException.class, () ->
            courtBlockService.blockRange(1L, manana, LocalTime.of(18, 0), LocalTime.of(17, 0), "Rango invertido")
        );
        assertThrows(IllegalArgumentException.class, () ->
            courtBlockService.blockRange(1L, manana, LocalTime.of(15, 0), LocalTime.of(15, 0), "Misma hora")
        );
    }

    @Test
    void test33_RangeBlockFiveSlots() {
        mockCourt(1L, new BigDecimal("40.00"), Court.CourtType.NORMAL, true);
        mockConfig();
        LocalDate manana = hoy.plusDays(1);
        when(courtBlockRepository.save(any(CourtBlock.class))).thenAnswer(i -> i.getArgument(0));
        when(reservationRepository.findByCourtIdAndReservationDateAndStatusIn(eq(1L), eq(manana), any())).thenReturn(Collections.emptyList());

        List<CourtBlock> slots = courtBlockService.blockRange(1L, manana, LocalTime.of(10, 0), LocalTime.of(15, 0), "Limpieza general");
        assertEquals(5, slots.size());
        assertEquals(LocalTime.of(10, 0), slots.get(0).getStartTime());
        assertEquals(LocalTime.of(14, 0), slots.get(4).getStartTime());
    }

    // 34. Bloqueo todo el dia horario normal -> 17 slots
    @Test
    void test34_FullDayBlockNormal() {
        mockCourt(1L, new BigDecimal("40.00"), Court.CourtType.NORMAL, true);
        mockConfig();
        LocalDate manana = hoy.plusDays(1);
        when(courtBlockRepository.save(any(CourtBlock.class))).thenAnswer(i -> i.getArgument(0));
        when(reservationRepository.findByCourtIdAndReservationDateAndStatusIn(eq(1L), eq(manana), any())).thenReturn(Collections.emptyList());

        List<CourtBlock> slots = courtBlockService.blockFullDay(1L, manana, "Mantenimiento total");
        assertEquals(17, slots.size());
        assertEquals(LocalTime.of(7, 0), slots.get(0).getStartTime());
        assertEquals(LocalTime.of(23, 0), slots.get(16).getStartTime());
    }

    // 35. Bloqueo todo el dia con cierre 23:00 -> 16 slots, no incluye 23:00-00:00
    @Test
    void test35_FullDayBlockClosing23() {
        mockCourt(1L, new BigDecimal("40.00"), Court.CourtType.NORMAL, true);
        mockConfig();
        LocalDate manana = hoy.plusDays(1);
        DailyOperatingHours especial = new DailyOperatingHours();
        especial.setOpeningTime(LocalTime.of(7, 0));
        especial.setClosingTime(LocalTime.of(23, 0));
        especial.setClosed(false);
        when(dailyOperatingHoursRepository.findByDate(manana)).thenReturn(Optional.of(especial));

        when(courtBlockRepository.save(any(CourtBlock.class))).thenAnswer(i -> i.getArgument(0));
        when(reservationRepository.findByCourtIdAndReservationDateAndStatusIn(eq(1L), eq(manana), any())).thenReturn(Collections.emptyList());

        List<CourtBlock> slots = courtBlockService.blockFullDay(1L, manana, "Cierre adelantado");
        assertEquals(16, slots.size());
        assertEquals(LocalTime.of(7, 0), slots.get(0).getStartTime());
        assertEquals(LocalTime.of(22, 0), slots.get(15).getStartTime());
        assertTrue(slots.stream().noneMatch(s -> s.getStartTime().equals(LocalTime.of(23, 0))));
    }

    // 36. Rango con reserva activa -> rechazo completo
    @Test
    void test36_RangeBlockWithActiveReservationRejected() {
        mockCourt(1L, new BigDecimal("40.00"), Court.CourtType.NORMAL, true);
        mockConfig();
        LocalDate manana = hoy.plusDays(1);
        Reservation reserva = new Reservation();
        reserva.setStartTime(LocalTime.of(12, 0));
        reserva.setCourt(courtService.findById(1L));
        when(reservationRepository.findByCourtIdAndReservationDateAndStatusIn(eq(1L), eq(manana), any())).thenReturn(List.of(reserva));

        assertThrows(OperationNotAllowedException.class, () -> 
            courtBlockService.blockRange(1L, manana, LocalTime.of(10, 0), LocalTime.of(15, 0), "Mantenimiento")
        );
    }

    // 37. No duplicar bloqueos activos
    @Test
    void test37_BlockDoNotDuplicateActive() {
        mockCourt(1L, new BigDecimal("40.00"), Court.CourtType.NORMAL, true);
        mockConfig();
        LocalDate manana = hoy.plusDays(1);
        CourtBlock yaActivo = new CourtBlock();
        yaActivo.setActive(true);
        when(courtBlockRepository.findByCourtIdAndDateAndStartTime(1L, manana, LocalTime.of(10, 0)))
                .thenReturn(Optional.of(yaActivo));

        assertThrows(OperationNotAllowedException.class, () ->
            courtBlockService.blockSlot(1L, manana, LocalTime.of(10, 0), "Reparación")
        );
    }

    // 38. Validacion de celular peruano (9 digitos iniciando con 9)
    @Test
    void test38_PeruvianPhoneValidation() {
        String regex = "^9[0-9]{8}$";
        assertTrue("949356493".matches(regex));
        assertTrue("912345678".matches(regex));

        assertFalse("123456789".matches(regex));
        assertFalse("94935649".matches(regex));
        assertFalse("9493564930".matches(regex));
        assertFalse("94935ABCD".matches(regex));
    }

    // 39. Validacion de configuracion contacto oficial
    @Test
    void test39_ContactConfiguration() {
        BusinessConfiguration config = new BusinessConfiguration();
        config.setContactPhone("949356493");
        config.setContactEmail("freddyquinnsoccer@gmail.com");
        assertEquals("949356493", config.getContactPhone());
        assertEquals("freddyquinnsoccer@gmail.com", config.getContactEmail());
    }
}
