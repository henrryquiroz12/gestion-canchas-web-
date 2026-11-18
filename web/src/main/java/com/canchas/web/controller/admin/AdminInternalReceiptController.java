package com.canchas.web.controller.admin;

import com.canchas.web.exception.ResourceNotFoundException;
import com.canchas.web.model.InternalReceipt;
import com.canchas.web.model.Reservation;
import com.canchas.web.service.InternalReceiptService;
import com.canchas.web.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/facturas")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminInternalReceiptController {

    private final InternalReceiptService receiptService;
    private final ReservationService reservationService;

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            Model model) {
        Page<InternalReceipt> receiptsPage = receiptService.findAllPaginated(page, size);
        model.addAttribute("receiptsPage", receiptsPage);
        model.addAttribute("facturas", receiptsPage.getContent());
        model.addAttribute("receipts", receiptsPage.getContent());
        model.addAttribute("activeSection", "facturas");
        return "admin/facturas/lista";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        InternalReceipt receipt = receiptService.findById(id);
        model.addAttribute("factura", receipt);
        model.addAttribute("receipt", receipt);
        model.addAttribute("activeSection", "facturas");
        return "admin/facturas/detalle";
    }

    @PostMapping("/generar/{alquilerId}")
    public String generateReceipt(@PathVariable Long alquilerId, RedirectAttributes ra) {
        try {
            Reservation reservation = reservationService.findById(alquilerId);
            if (reservation.getStatus() != Reservation.ReservationStatus.CONFIRMADO &&
                reservation.getStatus() != Reservation.ReservationStatus.COMPLETADO) {
                ra.addFlashAttribute("mensajeError", "Solo se puede generar comprobante para reservas CONFIRMADAS o COMPLETADAS.");
                return "redirect:/admin/facturas";
            }

            receiptService.generate(alquilerId);
            ra.addFlashAttribute("mensajeExito", "Comprobante generado correctamente.");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/admin/facturas";
    }

    @PostMapping("/{id}/anular")
    public String cancelReceipt(@PathVariable Long id, RedirectAttributes ra) {
        try {
            receiptService.cancel(id);
            ra.addFlashAttribute("mensajeExito", "Comprobante anulado correctamente.");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/admin/facturas/" + id;
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
        try {
            InternalReceipt receipt = receiptService.findById(id);
            byte[] pdf = receiptService.generatePdf(id);
            if (pdf == null || pdf.length == 0) {
                return ResponseEntity.internalServerError().build();
            }
            String num = (receipt.getReceiptNumber() != null && !receipt.getReceiptNumber().isBlank())
                    ? receipt.getReceiptNumber() : String.format("FQ-%06d", receipt.getId());
            String filename = num + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdf.length)
                    .body(pdf);
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.notFound().build();
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
