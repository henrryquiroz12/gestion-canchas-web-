package com.canchas.web.service.impl;

import com.canchas.web.dto.ReportFilterDTO;
import com.canchas.web.model.InternalReceipt;
import com.canchas.web.model.Reservation;
import com.canchas.web.model.User;
import com.canchas.web.repository.InternalReceiptRepository;
import com.canchas.web.repository.ReservationRepository;
import com.canchas.web.repository.UserRepository;
import com.canchas.web.service.ReportService;
import com.canchas.web.util.ExcelGenerator;
import com.canchas.web.util.PdfGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service("reportService")
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReservationRepository reservationRepository;
    private final InternalReceiptRepository receiptRepository;
    private final UserRepository userRepository;
    private final ExcelGenerator excelGenerator;
    private final PdfGenerator pdfGenerator;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public byte[] generateExcel(ReportFilterDTO filter) {
        return excelGenerator.generate(getSheetName(filter.getReportType()), getHeaders(filter.getReportType()), getRows(filter));
    }

    @Override
    public byte[] generatePdf(ReportFilterDTO filter) {
        return pdfGenerator.generate(getSheetName(filter.getReportType()), getHeaders(filter.getReportType()), getRows(filter));
    }

    private String getSheetName(String type) {
        return switch (type) {
            case "FACTURAS" -> "Facturas";
            case "CLIENTES" -> "Clientes";
            default -> "Alquileres";
        };
    }

    private List<String> getHeaders(String type) {
        return switch (type) {
            case "FACTURAS" -> List.of("N° Factura", "Cliente", "Fecha Emisión", "Monto", "Estado");
            case "CLIENTES" -> List.of("Cliente", "DNI", "Email", "Total Alquileres", "Gasto Total");
            default -> List.of("ID Reserva", "Cliente", "Cancha", "Fecha", "Hora Inicio", "Hora Fin", "Estado", "Método de Pago", "Precio Base", "Iluminación", "Monto Total");
        };
    }

    private List<List<String>> getRows(ReportFilterDTO filter) {
        return switch (filter.getReportType()) {
            case "FACTURAS" -> receiptRows(filter);
            case "CLIENTES" -> clientRows(filter);
            default -> reservationRows(filter);
        };
    }

    private List<List<String>> reservationRows(ReportFilterDTO filter) {
        List<List<String>> rows = new ArrayList<>();
        reservationRepository.findByReservationDateBetween(filter.getStartDate(), filter.getEndDate())
            .forEach(a -> {
                rows.add(List.of(
                    String.valueOf(a.getId()),
                    a.getClient().getFullName(),
                    a.getCourt().getName(),
                    a.getReservationDate().format(DATE_FORMATTER),
                    a.getStartTime().toString(),
                    a.getEndTime().toString(),
                    a.getStatus().name(),
                    a.getPaymentMethod().name(),
                    "S/ " + (a.getAppliedBasePrice() != null ? a.getAppliedBasePrice().toString() : a.getTotalAmount().toString()),
                    "S/ " + (a.getAppliedLightingSurcharge() != null ? a.getAppliedLightingSurcharge().toString() : "0.00"),
                    "S/ " + a.getTotalAmount().toString()
                ));
            });
        return rows;
    }

    private List<List<String>> receiptRows(ReportFilterDTO filter) {
        List<List<String>> rows = new ArrayList<>();
        receiptRepository.findByIssuedAtBetween(filter.getStartDate().atStartOfDay(), filter.getEndDate().atTime(23, 59, 59, 999999999))
            .forEach(f -> rows.add(List.of(
                    f.getReceiptNumber(),
                    f.getReservation().getClient().getFullName(),
                    f.getIssuedAt().format(DATE_TIME_FORMATTER),
                    "S/ " + f.getTotalAmount(),
                    f.getStatus().name()
            )));
        return rows;
    }

    private List<List<String>> clientRows(ReportFilterDTO filter) {
        List<List<String>> rows = new ArrayList<>();
        List<Reservation> periodReservations = reservationRepository.findByReservationDateBetween(filter.getStartDate(), filter.getEndDate());
        Map<User, List<Reservation>> clientMap = periodReservations.stream()
            .filter(a -> a.getClient() != null && a.getStatus() != Reservation.ReservationStatus.CANCELADO)
            .collect(Collectors.groupingBy(Reservation::getClient));

        for (Map.Entry<User, List<Reservation>> entry : clientMap.entrySet()) {
            User client = entry.getKey();
            List<Reservation> clientReservations = entry.getValue();
            BigDecimal totalSpent = clientReservations.stream().map(Reservation::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            rows.add(List.of(
                    client.getFullName(),
                    client.getDni() != null ? client.getDni() : "-",
                    client.getEmail() != null ? client.getEmail() : "-",
                    String.valueOf(clientReservations.size()),
                    "S/ " + totalSpent
            ));
        }
        return rows;
    }
}
