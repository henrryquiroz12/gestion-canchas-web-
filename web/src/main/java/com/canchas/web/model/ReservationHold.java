package com.canchas.web.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.*;

@Entity
@Table(name = "retenciones_horario", indexes = @Index(name = "idx_retencion_slot", columnList = "court_id,slot_date,start_time,expires_at"))
@Getter @Setter
public class ReservationHold {
    @Id @Column(name = "owner_key", length = 36)
    private String ownerKey;
    @Column(length = 36)
    private String token;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "court_id")
    private Court court;
    @Column(name = "slot_date")
    private LocalDate date;
    @Column(name = "start_time")
    private LocalTime startTime;
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    @Enumerated(EnumType.STRING) @Column(length = 16, nullable = false)
    private Status status;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reservation_id")
    private Reservation reservation;
    public enum Status { ACTIVE, CANCELLED, CONFIRMED }
    public boolean isActive(LocalDateTime now) {
        return status == Status.ACTIVE && expiresAt != null && expiresAt.isAfter(now);
    }
}
