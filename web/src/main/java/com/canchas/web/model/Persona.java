package com.canchas.web.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "personas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Persona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 50, message = "El nombre no puede superar 50 caracteres")
    @Column(name = "nombres", nullable = false, length = 50)
    private String nombres;

    @NotBlank(message = "El apellido paterno es obligatorio")
    @Size(max = 50, message = "El apellido paterno no puede superar 50 caracteres")
    @Column(name = "apellido_paterno", nullable = false, length = 50)
    private String apellidoPaterno;

    @NotBlank(message = "El apellido materno es obligatorio")
    @Size(max = 50, message = "El apellido materno no puede superar 50 caracteres")
    @Column(name = "apellido_materno", nullable = false, length = 50)
    private String apellidoMaterno;

    @Pattern(regexp = "^$|^[0-9]{8}$", message = "El DNI debe contener exactamente 8 dígitos")
    @Column(length = 8, unique = true)
    private String dni;

    @Email(message = "Ingresa un email válido")
    @Column(unique = true, length = 100)
    private String email;

    @Pattern(regexp = "^$|^9[0-9]{8}$", message = "El celular debe contener 9 dígitos y comenzar con 9")
    @Column(name = "telefono", length = 9)
    private String phoneNumber;

    @Column(name = "direccion", length = 150)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "genero", length = 20, nullable = false)
    private Gender gender = Gender.PREFER_NOT_TO_SAY;

    @Enumerated(EnumType.STRING)
    @Column(name = "departamento", length = 50)
    private PeruDepartment department;

    @Column(name = "provincia", length = 80)
    private String province;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDate registeredAt;

    @PrePersist
    public void onPrePersist() {
        if (registeredAt == null) {
            registeredAt = com.canchas.web.util.LimaTime.today();
        }
        if (gender == null) {
            gender = Gender.PREFER_NOT_TO_SAY;
        }
        if (nombres == null || nombres.isBlank()) {
            nombres = "Usuario";
        }
        if (apellidoPaterno == null || apellidoPaterno.isBlank()) {
            apellidoPaterno = "";
        }
        if (apellidoMaterno == null || apellidoMaterno.isBlank()) {
            apellidoMaterno = "";
        }
    }

    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        if (nombres != null && !nombres.isBlank()) {
            sb.append(nombres.trim());
        }
        if (apellidoPaterno != null && !apellidoPaterno.isBlank() && !"-".equals(apellidoPaterno.trim())) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(apellidoPaterno.trim());
        }
        if (apellidoMaterno != null && !apellidoMaterno.isBlank() && !"-".equals(apellidoMaterno.trim())) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(apellidoMaterno.trim());
        }
        return sb.toString();
    }

    public void setFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            this.nombres = "";
            this.apellidoPaterno = "";
            this.apellidoMaterno = "";
            return;
        }
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            this.nombres = parts[0];
            this.apellidoPaterno = "";
            this.apellidoMaterno = "";
        } else if (parts.length == 2) {
            this.nombres = parts[0];
            this.apellidoPaterno = parts[1];
            this.apellidoMaterno = "";
        } else if (parts.length == 3) {
            this.nombres = parts[0];
            this.apellidoPaterno = parts[1];
            this.apellidoMaterno = parts[2];
        } else {
            // 4 or more words: e.g. "Juan Carlos Quispe Flores" -> nombres="Juan Carlos", apellidoPaterno="Quispe", apellidoMaterno="Flores"
            this.apellidoMaterno = parts[parts.length - 1];
            this.apellidoPaterno = parts[parts.length - 2];
            StringBuilder n = new StringBuilder();
            for (int i = 0; i < parts.length - 2; i++) {
                if (i > 0) n.append(" ");
                n.append(parts[i]);
            }
            this.nombres = n.toString();
        }
    }
}
