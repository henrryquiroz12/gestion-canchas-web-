package com.canchas.web.controller;

import com.canchas.web.dto.AvailableSlotDTO;
import com.canchas.web.service.AvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/canchas")
@RequiredArgsConstructor
public class PublicAvailabilityController {

    private final AvailabilityService availabilityService;

    @GetMapping("/{id}/disponibilidad")
    public ResponseEntity<List<AvailableSlotDTO>> getAvailability(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {

        List<AvailableSlotDTO> slots = availabilityService.getAvailability(id, fecha);
        return ResponseEntity.ok(slots);
    }
}
