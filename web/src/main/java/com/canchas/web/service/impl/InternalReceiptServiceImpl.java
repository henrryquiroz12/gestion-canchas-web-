package com.canchas.web.service.impl;

import com.canchas.web.exception.OperationNotAllowedException;
import com.canchas.web.exception.ResourceNotFoundException;
import com.canchas.web.model.InternalReceipt;
import com.canchas.web.model.Reservation;
import com.canchas.web.repository.InternalReceiptRepository;
import com.canchas.web.service.InternalReceiptService;
import com.canchas.web.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service("internalReceiptService")
@RequiredArgsConstructor
public class InternalReceiptServiceImpl implements InternalReceiptService {

    private final InternalReceiptRepository receiptRepository;
    private final ReservationService reservationService;

    @Override
    @Transactional
    public InternalReceipt generate(Long reservationId) {
        Reservation reservation = reservationService.findById(reservationId);
        receiptRepository.findByReservationId(reservationId).ifPresent(f -> {
            throw new OperationNotAllowedException("Este alquiler ya tiene una factura generada");
        });

        InternalReceipt receipt = new InternalReceipt();
        receipt.setReservation(reservation);
        receipt.setTotalAmount(reservation.getTotalAmount());
        receipt.setStatus(InternalReceipt.ReceiptStatus.EMITIDA);
        
        receipt = receiptRepository.saveAndFlush(receipt);
        receipt.setReceiptNumber(String.format("FQ-%06d", receipt.getId()));
        return receiptRepository.save(receipt);
    }

    @Override
    @Transactional(readOnly = true)
    public InternalReceipt findByReservationId(Long reservationId) {
        return receiptRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("No hay factura para el alquiler: " + reservationId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InternalReceipt> findByDateRange(LocalDate start, LocalDate end) {
        return receiptRepository.findByIssuedAtBetween(start.atStartOfDay(), end.atTime(LocalTime.MAX));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InternalReceipt> findAll() {
        return receiptRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InternalReceipt> findAllPaginated(int page, int size) {
        int pageNumber = Math.max(0, page);
        int pageSize = size > 0 ? size : 15;
        return receiptRepository.findAllPaginated(PageRequest.of(pageNumber, pageSize));
    }

    @Override
    @Transactional(readOnly = true)
    public InternalReceipt findById(Long id) {
        return receiptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comprobante no encontrado con id: " + id));
    }

    @Override
    @Transactional
    public void cancel(Long id) {
        InternalReceipt receipt = findById(id);
        if (receipt.getStatus() == InternalReceipt.ReceiptStatus.ANULADA) {
            throw new OperationNotAllowedException("El comprobante ya está anulado");
        }
        receipt.setStatus(InternalReceipt.ReceiptStatus.ANULADA);
        receiptRepository.save(receipt);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generatePdf(Long id) {
        InternalReceipt receipt = findById(id);
        Reservation reservation = receipt.getReservation();

        try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            com.lowagie.text.Document document = new com.lowagie.text.Document();
            com.lowagie.text.pdf.PdfWriter.getInstance(document, out);
            document.open();

            com.lowagie.text.Font titleFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 18);
            com.lowagie.text.Font normalFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA, 12);
            com.lowagie.text.Font smallItalic = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_OBLIQUE, 10);

            document.add(new com.lowagie.text.Paragraph("FREDY QUEEN SOCCER", titleFont));
            document.add(new com.lowagie.text.Paragraph("Trujillo, Perú\n\n", normalFont));

            document.add(new com.lowagie.text.Paragraph("COMPROBANTE INTERNO", com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 14)));
            document.add(new com.lowagie.text.Paragraph("Número: " + receipt.getReceiptNumber() + "\n\n", normalFont));

            document.add(new com.lowagie.text.Paragraph("Detalle del Cliente:", com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 12)));
            String clientName = (reservation.getClient() != null && reservation.getClient().getFullName() != null)
                    ? reservation.getClient().getFullName() : "Cliente";
            String clientDni = (reservation.getClient() != null && reservation.getClient().getDni() != null && !reservation.getClient().getDni().isBlank())
                    ? "DNI: " + reservation.getClient().getDni() : "Sin DNI";
            document.add(new com.lowagie.text.Paragraph("Nombre: " + clientName, normalFont));
            document.add(new com.lowagie.text.Paragraph("Documento: " + clientDni + "\n\n", normalFont));

            document.add(new com.lowagie.text.Paragraph("Detalle de Reserva:", com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 12)));
            String courtName = (reservation.getCourt() != null) ? reservation.getCourt().getName() : "Cancha";
            document.add(new com.lowagie.text.Paragraph("Cancha: " + courtName, normalFont));
            document.add(new com.lowagie.text.Paragraph("Fecha: " + reservation.getReservationDate().toString(), normalFont));
            document.add(new com.lowagie.text.Paragraph("Horario: " + reservation.getStartTime() + " - " + reservation.getEndTime(), normalFont));
            String paymentMethod = (reservation.getPaymentMethod() != null) ? reservation.getPaymentMethod().name() : "EFECTIVO";
            document.add(new com.lowagie.text.Paragraph("Método de Pago: " + paymentMethod + "\n\n", normalFont));

            document.add(new com.lowagie.text.Paragraph("Conceptos:", com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 12)));
            java.math.BigDecimal basePrice = (reservation.getAppliedBasePrice() != null)
                    ? reservation.getAppliedBasePrice() : receipt.getTotalAmount();
            java.math.BigDecimal lightingSurcharge = (reservation.getAppliedLightingSurcharge() != null)
                    ? reservation.getAppliedLightingSurcharge() : java.math.BigDecimal.ZERO;

            document.add(new com.lowagie.text.Paragraph("Precio base cancha (1 hora): S/ " + String.format(java.util.Locale.US, "%.2f", basePrice), normalFont));
            if (lightingSurcharge.compareTo(java.math.BigDecimal.ZERO) > 0) {
                document.add(new com.lowagie.text.Paragraph("Recargo iluminación nocturna: S/ " + String.format(java.util.Locale.US, "%.2f", lightingSurcharge), normalFont));
            }

            document.add(new com.lowagie.text.Paragraph("\nTOTAL: S/ " + String.format(java.util.Locale.US, "%.2f", receipt.getTotalAmount()) + "\n\n", com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 14)));

            document.add(new com.lowagie.text.Paragraph("Estado: " + receipt.getStatus(), normalFont));
            document.add(new com.lowagie.text.Paragraph("Fecha de emisión: " + (receipt.getIssuedAt() != null ? receipt.getIssuedAt().toString() : "N/A") + "\n\n", normalFont));

            document.add(new com.lowagie.text.Paragraph("Documento interno de Fredy Queen Soccer.", smallItalic));

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar PDF de comprobante: " + e.getMessage(), e);
        }
    }
}
