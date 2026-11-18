package com.canchas.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourtBlockGroupDTO {

    private Long courtId;
    private String courtName;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String reason;
    private int totalSlots;
    private boolean isFullDay;

    @Builder.Default
    private List<Long> ids = new ArrayList<>();

    public String getFormattedSchedule() {
        String finStr = (endTime != null && endTime.equals(LocalTime.MIDNIGHT)) ? "00:00" : (endTime != null ? endTime.toString().substring(0, 5) : "");
        String inicioStr = (startTime != null) ? startTime.toString().substring(0, 5) : "";

        if (isFullDay) {
            return inicioStr + " - " + finStr + " (Todo el día)";
        }
        return inicioStr + " - " + finStr;
    }

    public String getIdsAsString() {
        if (ids == null || ids.isEmpty()) return "";
        return ids.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
    }

    // Backward compatibility aliases for Thymeleaf / Spanish methods
    public Long getCanchaId() { return courtId; }
    public void setCanchaId(Long canchaId) { this.courtId = canchaId; }
    public String getCanchaNombre() { return courtName; }
    public void setCanchaNombre(String canchaNombre) { this.courtName = canchaNombre; }
    public LocalDate getFecha() { return date; }
    public void setFecha(LocalDate fecha) { this.date = fecha; }
    public LocalTime getHoraInicio() { return startTime; }
    public void setHoraInicio(LocalTime horaInicio) { this.startTime = horaInicio; }
    public LocalTime getHoraFin() { return endTime; }
    public void setHoraFin(LocalTime horaFin) { this.endTime = horaFin; }
    public String getMotivo() { return reason; }
    public void setMotivo(String motivo) { this.reason = motivo; }
    public boolean isEsTodoElDia() { return isFullDay; }
    public void setEsTodoElDia(boolean esTodoElDia) { this.isFullDay = esTodoElDia; }
    public String getHorarioTexto() { return getFormattedSchedule(); }
    public String getIdsComoCadena() { return getIdsAsString(); }
}
