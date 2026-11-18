package com.canchas.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EffectiveOperatingHoursDTO {
    private boolean closed;
    private LocalDateTime operationStart;
    private LocalDateTime operationEnd;

    // Aliases
    public LocalDateTime getStartOperation() { return operationStart; }
    public void setStartOperation(LocalDateTime startOperation) { this.operationStart = startOperation; }
    public LocalDateTime getEndOperation() { return operationEnd; }
    public void setEndOperation(LocalDateTime endOperation) { this.operationEnd = endOperation; }

    // Backward compatibility aliases
    public boolean isCerrado() { return closed; }
    public void setCerrado(boolean cerrado) { this.closed = cerrado; }
    public LocalDateTime getInicioOperacion() { return operationStart; }
    public void setInicioOperacion(LocalDateTime inicioOperacion) { this.operationStart = inicioOperacion; }
    public LocalDateTime getFinOperacion() { return operationEnd; }
    public void setFinOperacion(LocalDateTime finOperacion) { this.operationEnd = finOperacion; }
}
