package com.canchas.web.controller.admin;

import com.canchas.web.dto.ReportFilterDTO;
import com.canchas.web.model.Reservation;
import com.canchas.web.repository.ReservationRepository;
import com.canchas.web.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/reportes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {

    private final ReservationRepository reservationRepository;
    private final ReportService reportService;

    @GetMapping
    public String index(
            @RequestParam(required = false) LocalDate fechaDesde,
            @RequestParam(required = false) LocalDate fechaHasta,
            Model model) {

        ZoneId zoneId = ZoneId.of("America/Lima");
        LocalDate now = LocalDate.now(zoneId);

        if (fechaDesde == null) {
            fechaDesde = now.withDayOfMonth(1);
        }
        if (fechaHasta == null) {
            fechaHasta = now.withDayOfMonth(now.lengthOfMonth());
        }

        if (fechaDesde.isAfter(fechaHasta)) {
            model.addAttribute("mensajeError", "La fecha de inicio no puede ser posterior a la fecha de fin.");
            fechaDesde = now.withDayOfMonth(1);
            fechaHasta = now.withDayOfMonth(now.lengthOfMonth());
        }

        List<Reservation> reservations = reservationRepository.findByReservationDateBetween(fechaDesde, fechaHasta);

        // Métricas
        long totalReservas = reservations.size();
        long pendientes = reservations.stream().filter(a -> a.getStatus() == Reservation.ReservationStatus.PENDIENTE).count();
        long confirmadas = reservations.stream().filter(a -> a.getStatus() == Reservation.ReservationStatus.CONFIRMADO).count();
        long completadas = reservations.stream().filter(a -> a.getStatus() == Reservation.ReservationStatus.COMPLETADO).count();
        long canceladas = reservations.stream().filter(a -> a.getStatus() == Reservation.ReservationStatus.CANCELADO).count();

        BigDecimal montoReservas = reservations.stream()
                .filter(a -> a.getStatus() != Reservation.ReservationStatus.CANCELADO)
                .map(Reservation::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Reporte por Cancha
        Map<String, Map<String, Object>> reservasPorCancha = new HashMap<>();
        for (Reservation a : reservations) {
            if (a.getStatus() == Reservation.ReservationStatus.CANCELADO) continue;
            String nombreCancha = a.getCourt().getName();
            reservasPorCancha.putIfAbsent(nombreCancha, new HashMap<>(Map.of("cantidad", 0L, "monto", BigDecimal.ZERO)));
            Map<String, Object> data = reservasPorCancha.get(nombreCancha);
            data.put("cantidad", ((Long) data.get("cantidad")) + 1);
            data.put("monto", ((BigDecimal) data.get("monto")).add(a.getTotalAmount()));
        }

        model.addAttribute("fechaDesde", fechaDesde);
        model.addAttribute("fechaHasta", fechaHasta);
        model.addAttribute("totalReservas", totalReservas);
        model.addAttribute("pendientes", pendientes);
        model.addAttribute("confirmadas", confirmadas);
        model.addAttribute("completadas", completadas);
        model.addAttribute("canceladas", canceladas);
        model.addAttribute("montoReservas", montoReservas);
        model.addAttribute("reservasPorCancha", reservasPorCancha);
        model.addAttribute("activeSection", "reportes");

        return "admin/reportes/lista";
    }

    @GetMapping("/excel")
    public ResponseEntity<byte[]> downloadExcel(
            @RequestParam LocalDate fechaDesde,
            @RequestParam LocalDate fechaHasta) {

        ReportFilterDTO filter = new ReportFilterDTO();
        filter.setStartDate(fechaDesde);
        filter.setEndDate(fechaHasta);
        filter.setReportType("ALQUILERES");

        byte[] excel = reportService.generateExcel(filter);
        String filename = "reporte-reservas-" + fechaDesde + "_" + fechaHasta + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excel);
    }
}
