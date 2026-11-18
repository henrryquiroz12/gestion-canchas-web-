package com.canchas.web.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalTime;

@Data
public class AvailableSlotDTO {

    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal basePrice;
    private BigDecimal lightingSurcharge;
    private BigDecimal totalPrice;
    private SlotStatus status;

    public enum SlotStatus {
        DISPONIBLE, OCUPADO, BLOQUEADO
    }

    // Backward compatibility aliases
    public LocalTime getHoraInicio() { return startTime; }
    public void setHoraInicio(LocalTime horaInicio) { this.startTime = horaInicio; }
    public LocalTime getHoraFin() { return endTime; }
    public void setHoraFin(LocalTime horaFin) { this.endTime = horaFin; }
    public BigDecimal getPrecioBase() { return basePrice; }
    public void setPrecioBase(BigDecimal precioBase) { this.basePrice = precioBase; }
    public BigDecimal getRecargoIluminacion() { return lightingSurcharge; }
    public void setRecargoIluminacion(BigDecimal recargoIluminacion) { this.lightingSurcharge = recargoIluminacion; }
    public BigDecimal getPrecioTotal() { return totalPrice; }
    public void setPrecioTotal(BigDecimal precioTotal) { this.totalPrice = precioTotal; }
    public SlotStatus getEstado() { return status; }
    public void setEstado(SlotStatus estado) { this.status = estado; }
}
