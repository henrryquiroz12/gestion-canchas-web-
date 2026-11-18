package com.canchas.web.dto;

import com.canchas.web.model.Gender;
import com.canchas.web.model.PeruDepartment;
import com.canchas.web.model.User;
import jakarta.validation.constraints.*;
import lombok.Data;
import com.canchas.web.validation.AccountRules;

@Data
public class UserRequestDTO {

    @NotBlank(message = "El usuario es obligatorio")
    @Size(min = 4, max = 30, message = "El usuario debe tener entre 4 y 30 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9._]+$", message = "El usuario solo puede contener letras, números, puntos y guiones bajos")
    private String username;

    @Pattern(regexp = AccountRules.OPTIONAL_DNI, message = "El DNI debe contener exactamente 8 dígitos")
    private String dni;

    @Pattern(regexp = "^$|.{6,}$", message = "La contraseña debe tener al menos 6 caracteres")
    private String password;

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(max = 100, message = "El nombre completo no puede superar los 100 caracteres")
    @Pattern(regexp = AccountRules.NAME, message = AccountRules.NAME_MESSAGE)
    private String fullName;

    @NotBlank(message = "El email es obligatorio")
    @Pattern(regexp = AccountRules.EMAIL, flags = Pattern.Flag.CASE_INSENSITIVE, message = AccountRules.EMAIL_MESSAGE)
    private String email;

    @NotBlank(message = "El celular es obligatorio")
    @Pattern(regexp = AccountRules.PHONE, message = "El celular debe contener 9 dígitos y comenzar con 9")
    private String phoneNumber;

    @Size(max = 150, message = "La dirección no puede superar los 150 caracteres")
    private String address;

    private PeruDepartment department;

    @Size(max = 80, message = "La provincia no puede superar los 80 caracteres")
    private String province;
    @NotNull(message = "Elige una opción")
    private Gender gender;

    @NotNull(message = "Selecciona un rol")
    private User.Role role;

    private Boolean active;

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
    public User.Role getRol() { return role; }
    public void setRol(User.Role rol) { this.role = rol; }
    public Boolean getActivo() { return active; }
    public void setActivo(Boolean activo) { this.active = activo; }
}
