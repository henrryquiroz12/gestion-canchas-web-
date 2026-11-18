package com.canchas.web.controller.admin;

import com.canchas.web.dto.EffectiveOperatingHoursDTO;
import com.canchas.web.model.BusinessConfiguration;
import com.canchas.web.model.Court;
import com.canchas.web.model.CourtBlock;
import com.canchas.web.model.DailyOperatingHours;
import com.canchas.web.service.BusinessConfigurationService;
import com.canchas.web.service.CourtBlockService;
import com.canchas.web.service.CourtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;

@Controller
@RequestMapping("/admin/horarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOperatingHoursController {

    private final BusinessConfigurationService configurationService;
    private final CourtBlockService courtBlockService;
    private final CourtService courtService;
    private final Clock clock;

    @GetMapping
    public String listOperatingHoursAndBlocks(Model model) {
        LocalDate today = LocalDate.now(clock);

        EffectiveOperatingHoursDTO effectiveHours = configurationService.getEffectiveOperatingHours(today);
        BusinessConfiguration configuration = configurationService.getConfiguration();
        DailyOperatingHours specialHours = configurationService.getDailyOperatingHours(today).orElse(null);

        DateTimeFormatter esFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.forLanguageTag("es-PE"));
        String fechaLarga = today.format(esFormatter);

        List<LocalTime> horasDesde = new ArrayList<>();
        List<LocalTime> horasHasta = new ArrayList<>();
        if (!effectiveHours.isClosed() && effectiveHours.getOperationStart() != null && effectiveHours.getOperationEnd() != null) {
            LocalTime start = effectiveHours.getOperationStart().toLocalTime();
            LocalTime end = effectiveHours.getOperationEnd().toLocalTime();
            boolean endsAtMidnight = end.equals(LocalTime.MIDNIGHT);

            LocalTime cur = start;
            while (true) {
                if (endsAtMidnight) {
                    horasDesde.add(cur);
                    cur = cur.plusHours(1);
                    if (cur.equals(LocalTime.MIDNIGHT)) break;
                } else {
                    if (cur.isBefore(end)) {
                        horasDesde.add(cur);
                        cur = cur.plusHours(1);
                    } else {
                        break;
                    }
                }
            }

            LocalTime curEnd = start.plusHours(1);
            while (true) {
                horasHasta.add(curEnd);
                if (endsAtMidnight && curEnd.equals(LocalTime.MIDNIGHT)) break;
                if (!endsAtMidnight && curEnd.equals(end)) break;
                curEnd = curEnd.plusHours(1);
            }
        }

        model.addAttribute("horarioEfectivo", effectiveHours);
        model.addAttribute("effectiveHours", effectiveHours);
        model.addAttribute("horarioEspecial", specialHours);
        model.addAttribute("specialHours", specialHours);
        model.addAttribute("configuracion", configuration);
        model.addAttribute("configuration", configuration);
        model.addAttribute("fechaOperativa", today);
        model.addAttribute("operatingDate", today);
        model.addAttribute("fechaLarga", fechaLarga);
        model.addAttribute("horasDesde", horasDesde);
        model.addAttribute("horasHasta", horasHasta);
        model.addAttribute("bloqueos", courtBlockService.findAllActive());
        model.addAttribute("blocks", courtBlockService.findAllActive());
        model.addAttribute("bloqueosAgrupados", courtBlockService.findGroupedBlocks());
        model.addAttribute("groupedBlocks", courtBlockService.findGroupedBlocks());
        model.addAttribute("canchas", courtService.findAvailable());
        model.addAttribute("courts", courtService.findAvailable());
        model.addAttribute("activeSection", "horarios");

        return "admin/horarios/lista";
    }

    @PostMapping("/cerrar-23")
    public String closeTodayAt23(RedirectAttributes ra) {
        try {
            LocalDate today = LocalDate.now(clock);
            configurationService.saveSpecialHours(today, LocalTime.of(7, 0), LocalTime.of(23, 0), false, "Cierre excepcional a las 23:00");
            ra.addFlashAttribute("mensajeExito", "Cierre excepcional aplicado para hoy a las 23:00.");
        } catch (Exception e) {
            ra.addFlashAttribute("mensajeError", "Error al aplicar cierre a las 23:00: " + e.getMessage());
        }
        return "redirect:/admin/horarios";
    }

    @PostMapping("/restaurar-00")
    public String restoreClosingAtMidnight(RedirectAttributes ra) {
        try {
            LocalDate today = LocalDate.now(clock);
            configurationService.saveSpecialHours(today, LocalTime.of(7, 0), LocalTime.MIDNIGHT, false, "Horario habitual hasta medianoche");
            ra.addFlashAttribute("mensajeExito", "Horario habitual restaurado hasta las 00:00 (medianoche) para hoy.");
        } catch (Exception e) {
            ra.addFlashAttribute("mensajeError", "Error al restaurar horario habitual: " + e.getMessage());
        }
        return "redirect:/admin/horarios";
    }

    @PostMapping("/extender-hoy")
    public String extendToday(RedirectAttributes ra) {
        return restoreClosingAtMidnight(ra);
    }

    @PostMapping("/especial")
    public String saveSpecialHours(@RequestParam(required = false) LocalTime horaFin,
                                   @RequestParam(required = false, defaultValue = "false") boolean cerrado,
                                   @RequestParam(required = false) String motivo,
                                   RedirectAttributes ra) {
        try {
            LocalDate today = LocalDate.now(clock);
            LocalTime openingTime = LocalTime.of(7, 0);
            LocalTime closingTime = (horaFin != null) ? horaFin : LocalTime.MIDNIGHT;
            if (cerrado) {
                openingTime = null;
                closingTime = null;
            }
            configurationService.saveSpecialHours(today, openingTime, closingTime, cerrado, motivo);
            ra.addFlashAttribute("mensajeExito", "Horario operativo para hoy guardado correctamente.");
        } catch (Exception e) {
            ra.addFlashAttribute("mensajeError", "Error al guardar horario: " + e.getMessage());
        }
        return "redirect:/admin/horarios";
    }

    @PostMapping("/bloqueos")
    public String createBlock(@RequestParam Long canchaId,
                              @RequestParam(required = false, defaultValue = "SLOT") String tipoBloqueo,
                              @RequestParam(required = false) LocalTime horaInicio,
                              @RequestParam(required = false) LocalTime horaFin,
                              @RequestParam(required = false) String motivoGeneral,
                              @RequestParam(required = false) String motivoDetalle,
                              @RequestParam(required = false) String motivo,
                              RedirectAttributes ra) {
        try {
            LocalDate today = LocalDate.now(clock);

            String finalReason = motivo;
            if (motivoGeneral != null && !motivoGeneral.isBlank()) {
                if (motivoDetalle != null && !motivoDetalle.isBlank()) {
                    finalReason = motivoGeneral + " - " + motivoDetalle.trim();
                } else {
                    finalReason = motivoGeneral;
                }
            }
            if (finalReason == null || finalReason.trim().isBlank()) {
                throw new IllegalArgumentException("El motivo del bloqueo es obligatorio.");
            }

            if ("TODO_EL_DIA".equalsIgnoreCase(tipoBloqueo)) {
                var blocked = courtBlockService.blockFullDay(canchaId, today, finalReason);
                ra.addFlashAttribute("mensajeExito", "Cancha bloqueada durante todo el día (" + blocked.size() + " slots).");
            } else if ("RANGO".equalsIgnoreCase(tipoBloqueo)) {
                if (horaInicio == null || horaFin == null) {
                    throw new IllegalArgumentException("Debe seleccionar las horas de inicio y fin para el rango.");
                }
                var blocked = courtBlockService.blockRange(canchaId, today, horaInicio, horaFin, finalReason);
                ra.addFlashAttribute("mensajeExito", "Rango de horas bloqueado correctamente (" + blocked.size() + " slots).");
            } else {
                if (horaInicio == null) {
                    throw new IllegalArgumentException("Debe seleccionar el slot de horario.");
                }
                courtBlockService.blockSlot(canchaId, today, horaInicio, finalReason);
                ra.addFlashAttribute("mensajeExito", "Slot bloqueado correctamente.");
            }
        } catch (Exception e) {
            ra.addFlashAttribute("mensajeError", "Error al crear bloqueo: " + e.getMessage());
        }
        return "redirect:/admin/horarios";
    }

    @PostMapping("/bloqueos/{id}/desbloquear")
    public String unblock(@PathVariable Long id, RedirectAttributes ra) {
        try {
            courtBlockService.unblockById(id);
            ra.addFlashAttribute("mensajeExito", "Slot desbloqueado.");
        } catch (Exception e) {
            ra.addFlashAttribute("mensajeError", "Error al desbloquear: " + e.getMessage());
        }
        return "redirect:/admin/horarios";
    }

    @PostMapping("/bloqueos/desbloquear-grupo")
    public String unblockGroup(@RequestParam("ids") List<Long> ids, RedirectAttributes ra) {
        try {
            courtBlockService.unblockByIds(ids);
            ra.addFlashAttribute("mensajeExito", "Bloqueo(s) retirado(s) correctamente.");
        } catch (Exception e) {
            ra.addFlashAttribute("mensajeError", "Error al desbloquear: " + e.getMessage());
        }
        return "redirect:/admin/horarios";
    }
}
