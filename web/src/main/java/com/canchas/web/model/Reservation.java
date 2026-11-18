package com.canchas.web.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "alquileres")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Selecciona un cliente")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private User client;

    @NotNull(message = "Selecciona una cancha")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancha_id", nullable = false)
    private Court court;

    @NotNull(message = "La fecha de alquiler es obligatoria")
    @Column(name = "fecha_alquiler", nullable = false)
    private LocalDate reservationDate;

    @NotNull(message = "La hora de inicio es obligatoria")
    @Column(name = "hora_inicio", nullable = false)
    private LocalTime startTime;

    @NotNull(message = "La hora de fin es obligatoria")
    @Column(name = "hora_fin", nullable = false)
    private LocalTime endTime;

    @NotNull(message = "El estado es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private ReservationStatus status = ReservationStatus.PENDIENTE;

    @NotNull(message = "Selecciona el método de pago")
    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", length = 20)
    private PaymentMethod paymentMethod;

    @NotNull(message = "El precio base aplicado es obligatorio")
    @DecimalMin(value = "0.00", message = "El precio base debe ser mayor o igual a 0")
    @Column(name = "precio_base_aplicado", nullable = false, precision = 8, scale = 2)
    private BigDecimal appliedBasePrice;

    @NotNull(message = "El recargo por iluminación aplicado es obligatorio")
    @DecimalMin(value = "0.00", message = "El recargo debe ser mayor o igual a 0")
    @Column(name = "recargo_iluminacion_aplicado", nullable = false, precision = 8, scale = 2)
    private BigDecimal appliedLightingSurcharge = BigDecimal.ZERO;

    @NotNull(message = "El monto total es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    @Column(name = "monto_total", nullable = false, precision = 8, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onPrePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now(java.time.Clock.system(java.time.ZoneId.of("America/Lima")));
        }
    }

    public enum ReservationStatus {
        PENDIENTE, CONFIRMADO, CANCELADO, COMPLETADO
    }

    public enum PaymentMethod {
        TARJETA, EFECTIVO, YAPE, PLIN, TRANSFERENCIA
    }

    // Backward compatibility aliases for Thymeleaf and legacy code
    public User getCliente() { return client; }
    public void setCliente(User cliente) { this.client = cliente; }
    public Court getCancha() { return court; }
    public void setCancha(Court cancha) { this.court = cancha; }
    public LocalDate getFechaAlquiler() { return reservationDate; }
    public void setFechaAlquiler(LocalDate fechaAlquiler) { this.reservationDate = fechaAlquiler; }
    public LocalTime getHoraInicio() { return startTime; }
    public void setHoraInicio(LocalTime horaInicio) { this.startTime = horaInicio; }
    public LocalTime getHoraFin() { return endTime; }
    public void setHoraFin(LocalTime horaFin) { this.endTime = horaFin; }
    public ReservationStatus getEstado() { return status; }
    public void setEstado(ReservationStatus estado) { this.status = estado; }
    public PaymentMethod getMetodoPago() { return paymentMethod; }
    public void setMetodoPago(PaymentMethod metodoPago) { this.paymentMethod = metodoPago; }
    public BigDecimal getPrecioBaseAplicado() { return appliedBasePrice; }
    public void setPrecioBaseAplicado(BigDecimal precioBaseAplicado) { this.appliedBasePrice = precioBaseAplicado; }
    public BigDecimal getRecargoIluminacionAplicado() { return appliedLightingSurcharge; }
    public void setRecargoIluminacionAplicado(BigDecimal recargoIluminacionAplicado) { this.appliedLightingSurcharge = recargoIluminacionAplicado; }
    public BigDecimal getMontoTotal() { return totalAmount; }
    public void setMontoTotal(BigDecimal montoTotal) { this.totalAmount = montoTotal; }
    public LocalDateTime getFechaCreacion() { return createdAt; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.createdAt = fechaCreacion; }
}
