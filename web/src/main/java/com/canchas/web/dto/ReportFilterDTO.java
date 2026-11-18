package com.canchas.web.dto;

import java.time.LocalDate;
import lombok.Data;

@Data
public class ReportFilterDTO {
    private LocalDate startDate;
    private LocalDate endDate;
    private String reportType;

    // Backward compatibility aliases
    public LocalDate getFechaInicio() { return startDate; }
    public void setFechaInicio(LocalDate fechaInicio) { this.startDate = fechaInicio; }
    public LocalDate getFechaFin() { return endDate; }
    public void setFechaFin(LocalDate fechaFin) { this.endDate = fechaFin; }
    public String getTipoReporte() { return reportType; }
    public void setTipoReporte(String tipoReporte) { this.reportType = tipoReporte; }
}
