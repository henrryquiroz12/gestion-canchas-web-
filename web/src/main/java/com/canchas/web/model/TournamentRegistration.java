package com.canchas.web.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inscripciones_torneo", uniqueConstraints = {
    @UniqueConstraint(name = "uk_torneo_cliente", columnNames = {"torneo_id", "cliente_id"}),
    @UniqueConstraint(name = "uk_torneo_equipo", columnNames = {"torneo_id", "nombre_equipo"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TournamentRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "torneo_id", nullable = false)
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private User client;

    @Column(name = "nombre_equipo", nullable = false, length = 100)
    private String teamName;

    @Column(name = "monto_aplicado", nullable = false, precision = 10, scale = 2)
    private BigDecimal appliedAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private TournamentRegistrationStatus status;

    @Column(name = "fecha_inscripcion", nullable = false)
    private LocalDateTime registrationDate;

    @PrePersist
    protected void onCreate() {
        if (registrationDate == null) {
            registrationDate = LocalDateTime.now();
        }
    }
}