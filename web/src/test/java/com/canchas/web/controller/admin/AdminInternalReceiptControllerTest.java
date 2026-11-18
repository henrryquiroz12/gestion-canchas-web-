package com.canchas.web.controller.admin;

import com.canchas.web.model.InternalReceipt;
import com.canchas.web.model.Reservation;
import com.canchas.web.service.InternalReceiptService;
import com.canchas.web.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class AdminInternalReceiptControllerTest {

    private MockMvc mockMvc;

    @Mock
    private InternalReceiptService receiptService;

    @Mock
    private ReservationService reservationService;

    @InjectMocks
    private AdminInternalReceiptController controller;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void testGenerarFacturaAlquilerValido() throws Exception {
        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setStatus(Reservation.ReservationStatus.CONFIRMADO);
        when(reservationService.findById(1L)).thenReturn(reservation);

        mockMvc.perform(post("/admin/facturas/generar/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/facturas"))
                .andExpect(flash().attributeExists("mensajeExito"));
    }

    @Test
    public void testGenerarFacturaAlquilerCancelado() throws Exception {
        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setStatus(Reservation.ReservationStatus.CANCELADO);
        when(reservationService.findById(1L)).thenReturn(reservation);

        mockMvc.perform(post("/admin/facturas/generar/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/facturas"))
                .andExpect(flash().attributeExists("mensajeError"));
    }

    @Test
    public void testGenerarFacturaDuplicada() throws Exception {
        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setStatus(Reservation.ReservationStatus.CONFIRMADO);
        when(reservationService.findById(1L)).thenReturn(reservation);
        doThrow(new RuntimeException("Este alquiler ya tiene una factura generada")).when(receiptService).generate(1L);

        mockMvc.perform(post("/admin/facturas/generar/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/facturas"))
                .andExpect(flash().attributeExists("mensajeError"));
    }

    @Test
    public void testDescargarPdf() throws Exception {
        InternalReceipt receipt = new InternalReceipt();
        receipt.setId(1L);
        receipt.setReceiptNumber("FQ-000001");
        when(receiptService.findById(1L)).thenReturn(receipt);
        byte[] pdfBytes = "%PDF-1.4 mock content".getBytes();
        when(receiptService.generatePdf(1L)).thenReturn(pdfBytes);

        mockMvc.perform(get("/admin/facturas/1/pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"FQ-000001.pdf\""))
                .andExpect(content().contentType("application/pdf"))
                .andExpect(content().bytes(pdfBytes));
    }
}
