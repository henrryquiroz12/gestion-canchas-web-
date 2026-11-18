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
@Table(name = "bloqueo_cancha")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourtBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancha_id", nullable = false)
    private Court court;

    @NotNull
    @Column(name = "fecha", nullable = false)
    private LocalDate date;

    @NotNull
    @Column(name = "hora_inicio", nullable = false)
    private LocalTime startTime;

    @Column(name = "motivo", length = 200)
    private String reason;

    @Column(name = "activo", nullable = false)
    private boolean active = true;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = com.canchas.web.util.LimaTime.nowDateTime();
        }
    }

    // Backward compatibility aliases
    public Court getCancha() { return court; }
    public void setCancha(Court cancha) { this.court = cancha; }
    public LocalDate getFecha() { return date; }
    public void setFecha(LocalDate fecha) { this.date = fecha; }
    public LocalTime getHoraInicio() { return startTime; }
    public void setHoraInicio(LocalTime horaInicio) { this.startTime = horaInicio; }
    public String getMotivo() { return reason; }
    public void setMotivo(String motivo) { this.reason = motivo; }
    public boolean isActivo() { return active; }
    public void setActivo(boolean activo) { this.active = activo; }
}
