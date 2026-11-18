package com.canchas.web.controller.admin;

import com.canchas.web.dto.DashboardStatsDTO;
import com.canchas.web.dto.EffectiveOperatingHoursDTO;
import com.canchas.web.model.Reservation;
import com.canchas.web.model.User;
import com.canchas.web.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final ReservationService reservationService;
    private final CourtService courtService;
    private final UserService userService;
    private final BusinessConfigurationService configurationService;
    private final InternalReceiptService receiptService;

    @GetMapping
    public String dashboard(Authentication authentication, Model model) {
        LocalDate today = com.canchas.web.util.LimaTime.today();

        // Stats
        DashboardStatsDTO stats = reservationService.getDashboardStats();
        model.addAttribute("canchasHabilitadas", courtService.findAvailable().size());
        model.addAttribute("totalClientesStat", userService.countClients());
        model.addAttribute("alquileresSemana", stats.getAlquileresSemana());
        model.addAttribute("gananciasSemana", stats.getGananciasSemana());
        model.addAttribute("ventasPorMes", stats.getVentasPorMes());
        model.addAttribute("ventasPorTipoCancha", stats.getVentasPorTipoCancha());
        model.addAttribute("anioActual", today.getYear());

        // Horario Operativo Hoy
        EffectiveOperatingHoursDTO operatingHoursToday = configurationService.getEffectiveOperatingHours(today);
        model.addAttribute("horarioHoy", operatingHoursToday);
        model.addAttribute("hoy", today);

        // Reservas Hoy
        List<Reservation> reservationsToday = reservationService.findByDate(today);
        List<Reservation> pendingToday = reservationsToday.stream()
                .filter(a -> a.getStatus() == Reservation.ReservationStatus.PENDIENTE)
                .collect(Collectors.toList());
        model.addAttribute("reservasHoy", reservationsToday);
        model.addAttribute("pendientesHoy", pendingToday.size());

        // Comprobantes Recientes (máx 15 líneas para evitar desbordes)
        model.addAttribute("facturas", receiptService.findAllPaginated(0, 15).getContent());

        model.addAttribute("activeSection", "dashboard");
        return "admin/dashboard";
    }
}
