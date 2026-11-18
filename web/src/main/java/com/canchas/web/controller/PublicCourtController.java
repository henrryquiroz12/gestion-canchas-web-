package com.canchas.web.controller;

import com.canchas.web.model.Court;
import com.canchas.web.service.CourtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class PublicCourtController {

    private final CourtService courtService;

    @GetMapping("/canchas")
    public String listCourts(
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false, defaultValue = "numero") String orden,
            Model model) {

        List<Court> courts = new java.util.ArrayList<>(courtService.findAvailable());

        // 1. Filtrar
        if (tipo != null && !tipo.trim().isEmpty()) {
            try {
                Court.CourtType courtType = Court.CourtType.valueOf(tipo.toUpperCase());
                courts = courts.stream()
                        .filter(c -> c.getType() == courtType)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                // Ignore invalid filter
            }
        }

        // 2. Ordenar
        switch (orden) {
            case "precioAsc":
                courts.sort(Comparator.comparing(Court::getHourlyRate));
                break;
            case "precioDesc":
                courts.sort(Comparator.comparing(Court::getHourlyRate).reversed());
                break;
            case "numero":
            default:
                courts.sort(Comparator.comparing(Court::getId));
                break;
        }

        model.addAttribute("canchas", courts);
        model.addAttribute("courts", courts);
        model.addAttribute("filtroTipo", tipo);
        model.addAttribute("filtroOrden", orden);
        model.addAttribute("activeSection", "canchas");

        return "canchas/listado";
    }

    @GetMapping("/canchas/{id}")
    public String courtDetail(@PathVariable Long id, Model model) {
        Court court = courtService.findById(id);

        if (court == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cancha no encontrada");
        }

        model.addAttribute("cancha", court);
        model.addAttribute("court", court);
        model.addAttribute("fechaHoyLima", LocalDate.now(ZoneId.of("America/Lima")).toString());
        model.addAttribute("activeSection", "canchas");
        return "canchas/detalle";
    }
}
