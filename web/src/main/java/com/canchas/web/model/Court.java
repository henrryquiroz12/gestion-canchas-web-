package com.canchas.web.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "canchas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Court {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre de la cancha es obligatorio")
    @Size(max = 80, message = "El nombre no puede superar los 80 caracteres")
    @Column(name = "nombre", nullable = false, length = 80)
    private String name;

    @NotNull(message = "Selecciona el tipo de cancha")
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private CourtType type;

    @NotNull(message = "El precio por hora es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
    @Column(name = "precio_hora", nullable = false, precision = 8, scale = 2)
    private BigDecimal hourlyRate;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(min = 20, max = 200, message = "La descripción debe tener entre 20 y 200 caracteres")
    @Column(name = "descripcion", length = 255)
    private String description;

    @Column(name = "imagen_url", length = 255)
    private String imageUrl;

    @Column(name = "disponible", nullable = false)
    private boolean available = true;

    public enum CourtType {
        NORMAL, GRANDE
    }

    // Backward compatibility aliases for Thymeleaf / code
    public String getNombre() { return name; }
    public void setNombre(String nombre) { this.name = nombre; }
    public CourtType getTipo() { return type; }
    public void setTipo(CourtType tipo) { this.type = tipo; }
    public BigDecimal getPrecioHora() { return hourlyRate; }
    public void setPrecioHora(BigDecimal precioHora) { this.hourlyRate = precioHora; }
    public String getDescripcion() { return description; }
    public void setDescripcion(String descripcion) { this.description = descripcion; }
    public String getImagenUrl() { return imageUrl; }
    public void setImagenUrl(String imagenUrl) { this.imageUrl = imagenUrl; }
    public boolean isDisponible() { return available; }
    public void setDisponible(boolean disponible) { this.available = disponible; }
}
