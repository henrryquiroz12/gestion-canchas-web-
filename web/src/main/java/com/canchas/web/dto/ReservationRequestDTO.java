package com.canchas.web.dto;

import com.canchas.web.model.Reservation;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.time.LocalTime;

public class ReservationRequestDTO {

    // Optional: Only ADMIN can specify clientId, otherwise authenticated user is used
    private Long clientId;

    @NotNull(message = "Selecciona una cancha")
    private Long courtId;

    @NotNull(message = "Selecciona la fecha")
    @FutureOrPresent(message = "No se puede reservar en una fecha anterior a hoy")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate reservationDate;

    @NotNull(message = "Selecciona la hora de inicio")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @NotNull(message = "Selecciona el método de pago")
    private Reservation.PaymentMethod paymentMethod;

    public ReservationRequestDTO() {}

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public Long getCourtId() { return courtId; }
    public void setCourtId(Long courtId) { this.courtId = courtId; }

    public LocalDate getReservationDate() { return reservationDate; }
    public void setReservationDate(LocalDate reservationDate) { this.reservationDate = reservationDate; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public Reservation.PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(Reservation.PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    // Backward compatibility aliases
    public Long getClienteId() { return clientId; }
    public void setClienteId(Long clienteId) { this.clientId = clienteId; }

    public Long getCanchaId() { return courtId; }
    public void setCanchaId(Long canchaId) { this.courtId = canchaId; }

    public LocalDate getFechaAlquiler() { return reservationDate; }
    public void setFechaAlquiler(LocalDate fechaAlquiler) { this.reservationDate = fechaAlquiler; }

    public LocalTime getHoraInicio() { return startTime; }
    public void setHoraInicio(LocalTime horaInicio) { this.startTime = horaInicio; }

    public Reservation.PaymentMethod getMetodoPago() { return paymentMethod; }
    public void setMetodoPago(Reservation.PaymentMethod metodoPago) { this.paymentMethod = metodoPago; }
}
