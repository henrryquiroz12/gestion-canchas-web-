package com.canchas.web.dto;

import com.canchas.web.model.Gender;
import com.canchas.web.model.PeruDepartment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import com.canchas.web.validation.AccountRules;

@Data
public class ClientProfileDTO {

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    @Pattern(regexp = AccountRules.NAME, message = AccountRules.NAME_MESSAGE)
    private String fullName;

    @NotBlank(message = "El email es obligatorio")
    @Pattern(regexp = AccountRules.EMAIL, flags = Pattern.Flag.CASE_INSENSITIVE, message = AccountRules.EMAIL_MESSAGE)
    private String email;

    @NotBlank(message = "El celular es obligatorio")
    @Pattern(regexp = AccountRules.PHONE, message = "El celular debe contener 9 dígitos y comenzar con 9")
    private String phoneNumber;

    @NotNull(message = "Selecciona tu departamento")
    private PeruDepartment department;

    @NotBlank(message = "Selecciona tu provincia")
    @Size(max = 80, message = "La provincia no puede superar los 80 caracteres")
    private String province;

    @NotNull(message = "Elige una opción")
    private Gender gender;

    @Size(max = 150, message = "La dirección no puede superar los 150 caracteres")
    private String address;

    // Backward compatibility aliases
    public String getNombreCompleto() { return fullName; }
    public void setNombreCompleto(String nombreCompleto) { this.fullName = nombreCompleto; }
    public String getTelefono() { return phoneNumber; }
    public void setTelefono(String telefono) { this.phoneNumber = telefono; }
    public String getDireccion() { return address; }
    public void setDireccion(String direccion) { this.address = direccion; }
    public PeruDepartment getDepartamento() { return department; }
    public void setDepartamento(PeruDepartment departamento) { this.department = departamento; }
    public String getProvincia() { return province; }
    public void setProvincia(String provincia) { this.province = provincia; }
    public Gender getGenero() { return gender; }
    public void setGenero(Gender genero) { this.gender = genero; }
}
