package com.canchas.web.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "horario_operacion_dia")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyOperatingHours {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "fecha", nullable = false, unique = true)
    private LocalDate date;

    @Column(name = "hora_apertura")
    private LocalTime openingTime;

    @Column(name = "hora_cierre")
    private LocalTime closingTime;

    @Column(name = "cerrado", nullable = false)
    private boolean closed = false;

    @Column(name = "motivo", length = 200)
    private String reason;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now(java.time.Clock.system(java.time.ZoneId.of("America/Lima")));
    }

    // Backward compatibility aliases
    public LocalDate getFecha() { return date; }
    public void setFecha(LocalDate fecha) { this.date = fecha; }
    public LocalTime getHoraApertura() { return openingTime; }
    public void setHoraApertura(LocalTime horaApertura) { this.openingTime = horaApertura; }
    public LocalTime getHoraCierre() { return closingTime; }
    public void setHoraCierre(LocalTime horaCierre) { this.closingTime = horaCierre; }
    public boolean isCerrado() { return closed; }
    public void setCerrado(boolean cerrado) { this.closed = cerrado; }
    public String getMotivo() { return reason; }
    public void setMotivo(String motivo) { this.reason = motivo; }
}
