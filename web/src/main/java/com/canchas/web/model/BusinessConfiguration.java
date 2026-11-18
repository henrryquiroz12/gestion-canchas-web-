package com.canchas.web.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(name = "configuracion_negocio")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusinessConfiguration {

    @Id
    private Short id = 1;

    @NotBlank
    @Column(name = "nombre_empresa", nullable = false, length = 100)
    private String businessName;

    @Column(name = "ciudad", length = 80)
    private String city;

    @Column(name = "direccion", length = 200)
    private String address;

    @NotBlank(message = "El teléfono de contacto es obligatorio")
    @Pattern(regexp = "^9[0-9]{8}$", message = "El teléfono debe contener 9 dígitos y comenzar con 9")
    @Column(name = "telefono", length = 9, nullable = false)
    private String contactPhone;

    @NotBlank(message = "El correo de contacto es obligatorio")
    @Email(message = "El correo de contacto no es válido")
    @Column(name = "email_contacto", length = 100, nullable = false)
    private String contactEmail;

    @NotNull
    @Column(name = "hora_apertura_default", nullable = false)
    private LocalTime defaultOpeningTime;

    @NotNull
    @Column(name = "hora_cierre_default", nullable = false)
    private LocalTime defaultClosingTime;

    @NotNull
    @Column(name = "hora_inicio_iluminacion", nullable = false)
    private LocalTime lightingStartTime;

    @NotNull
    @DecimalMin(value = "0.00")
    @Column(name = "recargo_iluminacion", nullable = false, precision = 8, scale = 2)
    private BigDecimal lightingSurcharge;

    // Backward compatibility aliases
    public String getNombreEmpresa() { return businessName; }
    public void setNombreEmpresa(String nombreEmpresa) { this.businessName = nombreEmpresa; }
    public String getCompanyName() { return businessName; }
    public void setCompanyName(String companyName) { this.businessName = companyName; }
    public String getCiudad() { return city; }
    public void setCiudad(String ciudad) { this.city = ciudad; }
    public String getDireccion() { return address; }
    public void setDireccion(String direccion) { this.address = direccion; }
    public String getTelefono() { return contactPhone; }
    public void setTelefono(String telefono) { this.contactPhone = telefono; }
    public String getTelefonoContacto() { return contactPhone; }
    public void setTelefonoContacto(String telefonoContacto) { this.contactPhone = telefonoContacto; }
    public String getEmailContacto() { return contactEmail; }
    public void setEmailContacto(String emailContacto) { this.contactEmail = emailContacto; }
    public LocalTime getHoraAperturaDefault() { return defaultOpeningTime; }
    public void setHoraAperturaDefault(LocalTime horaAperturaDefault) { this.defaultOpeningTime = horaAperturaDefault; }
    public LocalTime getHoraCierreDefault() { return defaultClosingTime; }
    public void setHoraCierreDefault(LocalTime horaCierreDefault) { this.defaultClosingTime = horaCierreDefault; }
    public LocalTime getHoraInicioIluminacion() { return lightingStartTime; }
    public void setHoraInicioIluminacion(LocalTime horaInicioIluminacion) { this.lightingStartTime = horaInicioIluminacion; }
    public BigDecimal getRecargoIluminacion() { return lightingSurcharge; }
    public void setRecargoIluminacion(BigDecimal recargoIluminacion) { this.lightingSurcharge = recargoIluminacion; }
}
