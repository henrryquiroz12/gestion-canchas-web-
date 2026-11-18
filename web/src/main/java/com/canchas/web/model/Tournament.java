package com.canchas.web.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "torneos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tournament {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 100)
    private String slug;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String description;

    @Column(name = "formato", length = 50)
    private String format;

    @Column(name = "precio_por_equipo", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerTeam;

    @Column(name = "fecha_inicio")
    private LocalDate startDate;

    @Column(name = "fecha_fin")
    private LocalDate endDate;

    @Column(name = "cupos_maximos")
    private Integer maxTeams;

    @Column(name = "inscripciones_abiertas", nullable = false)
    private boolean registrationsOpen = true;

    @Column(name = "estado", nullable = false, length = 30)
    private String status = "ACTIVO";

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public boolean isFree() {
        return pricePerTeam == null || pricePerTeam.compareTo(BigDecimal.ZERO) == 0;
    }
}
