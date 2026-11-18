package com.canchas.web.controller;

import com.canchas.web.dto.AvailableSlotDTO;
import com.canchas.web.service.AvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/disponibilidad")
@RequiredArgsConstructor
public class AvailabilityApiController {

    private final AvailabilityService availabilityService;

    @GetMapping("/slots")
    public ResponseEntity<List<AvailableSlotDTO>> getSlots(
            @RequestParam Long canchaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        List<AvailableSlotDTO> slots = availabilityService.getAvailability(canchaId, fecha);
        return ResponseEntity.ok(slots);
    }
}
