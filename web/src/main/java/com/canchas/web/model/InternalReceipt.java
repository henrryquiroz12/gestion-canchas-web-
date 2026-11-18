package com.canchas.web.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "facturas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InternalReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El alquiler es obligatorio")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alquiler_id", nullable = false, unique = true)
    private Reservation reservation;

    @Size(max = 20, message = "El número de factura no puede superar los 20 caracteres")
    @Column(name = "numero_factura", unique = true, length = 20)
    private String receiptNumber;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime issuedAt;

    @NotNull(message = "El monto total es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    @Column(name = "monto_total", nullable = false, precision = 8, scale = 2)
    private BigDecimal totalAmount;

    @NotNull(message = "El estado es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private ReceiptStatus status = ReceiptStatus.EMITIDA;

    @PrePersist
    protected void onPrePersist() {
        if (issuedAt == null) {
            issuedAt = LocalDateTime.now(java.time.Clock.system(java.time.ZoneId.of("America/Lima")));
        }
    }

    public enum ReceiptStatus {
        EMITIDA, ANULADA
    }

    // Backward compatibility aliases
    public Reservation getAlquiler() { return reservation; }
    public void setAlquiler(Reservation alquiler) { this.reservation = alquiler; }
    public String getNumeroFactura() { return receiptNumber; }
    public void setNumeroFactura(String numeroFactura) { this.receiptNumber = numeroFactura; }
    public LocalDateTime getFechaEmision() { return issuedAt; }
    public void setFechaEmision(LocalDateTime fechaEmision) { this.issuedAt = fechaEmision; }
    public BigDecimal getMontoTotal() { return totalAmount; }
    public void setMontoTotal(BigDecimal montoTotal) { this.totalAmount = montoTotal; }
    public ReceiptStatus getEstado() { return status; }
    public void setEstado(ReceiptStatus estado) { this.status = estado; }
}
