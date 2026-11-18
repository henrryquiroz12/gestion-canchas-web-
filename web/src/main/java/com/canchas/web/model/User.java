package com.canchas.web.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "persona_id", nullable = false, unique = true)
    private Persona persona;

    @Column(name = "apodo", length = 50)
    private String nickname;

    @NotBlank(message = "El usuario es obligatorio")
    @Size(min = 4, max = 50, message = "El usuario debe tener entre 4 y 50 caracteres")
    @Column(name = "usuario", nullable = false, unique = true, length = 50)
    private String username;

    @NotBlank(message = "La contraseña es obligatoria")
    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "activo", nullable = false)
    private boolean active = true;

    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.MERGE})
    @JoinTable(
        name = "usuario_roles",
        joinColumns = @JoinColumn(name = "usuario_id"),
        inverseJoinColumns = @JoinColumn(name = "rol_id")
    )
    private Set<com.canchas.web.model.Role> roles = new HashSet<>();

    // Backward compatibility enum for User.Role
    public enum Role {
        CLIENTE, ADMIN
    }

    private Persona ensurePersona() {
        if (this.persona == null) {
            this.persona = new Persona();
        }
        return this.persona;
    }

    // Backward compatibility delegators to Persona
    public String getFullName() {
        return persona != null ? persona.getFullName() : "";
    }

    public void setFullName(String fullName) {
        ensurePersona().setFullName(fullName);
    }

    public String getDni() {
        return persona != null ? persona.getDni() : null;
    }

    public void setDni(String dni) {
        ensurePersona().setDni(dni);
    }

    public String getEmail() {
        return persona != null ? persona.getEmail() : null;
    }

    public void setEmail(String email) {
        ensurePersona().setEmail(email);
    }

    public String getPhoneNumber() {
        return persona != null ? persona.getPhoneNumber() : null;
    }

    public void setPhoneNumber(String phoneNumber) {
        ensurePersona().setPhoneNumber(phoneNumber);
    }

    public String getAddress() {
        return persona != null ? persona.getAddress() : null;
    }

    public void setAddress(String address) {
        ensurePersona().setAddress(address);
    }

    public Gender getGender() {
        return persona != null ? persona.getGender() : Gender.PREFER_NOT_TO_SAY;
    }

    public void setGender(Gender gender) {
        ensurePersona().setGender(gender);
    }

    public PeruDepartment getDepartment() {
        return persona != null ? persona.getDepartment() : null;
    }

    public void setDepartment(PeruDepartment department) {
        ensurePersona().setDepartment(department);
    }

    public String getProvince() {
        return persona != null ? persona.getProvince() : null;
    }

    public void setProvince(String province) {
        ensurePersona().setProvince(province);
    }

    public LocalDate getRegisteredAt() {
        return persona != null ? persona.getRegisteredAt() : null;
    }

    public void setRegisteredAt(LocalDate registeredAt) {
        ensurePersona().setRegisteredAt(registeredAt);
    }

    public String getApodo() {
        return nickname;
    }

    public void setApodo(String apodo) {
        this.nickname = apodo;
    }

    public Set<com.canchas.web.model.Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<com.canchas.web.model.Role> roles) {
        this.roles = roles;
    }

    // Role check and getter/setter for compatibility
    public boolean hasRole(String roleName) {
        if (roles == null) return false;
        String normalized = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
        return roles.stream().anyMatch(r -> r != null && r.getName() != null && (r.getName().equalsIgnoreCase(normalized) || r.getName().equalsIgnoreCase(roleName)));
    }

    public Role getRole() {
        if (hasRole("ROLE_ADMIN") || hasRole("ADMIN")) {
            return Role.ADMIN;
        }
        return Role.CLIENTE;
    }

    public void setRole(Role role) {
        if (this.roles == null) {
            this.roles = new HashSet<>();
        }
        this.roles.clear();
        if (role == Role.ADMIN) {
            this.roles.add(new com.canchas.web.model.Role(1L, "ROLE_ADMIN", "Administrador"));
        } else {
            this.roles.add(new com.canchas.web.model.Role(2L, "ROLE_CLIENTE", "Cliente"));
        }
    }

    // Lifecycle methods
    @PrePersist
    public void onPrePersist() {
        ensurePersona();
        if (persona.getRegisteredAt() == null) {
            persona.setRegisteredAt(com.canchas.web.util.LimaTime.today());
        }
    }

    public void onCreate() {
        onPrePersist();
    }

    public void alCrear() {
        onPrePersist();
    }

    // Compatibility aliases
    public String getNombreCompleto() { return getFullName(); }
    public void setNombreCompleto(String nombreCompleto) { setFullName(nombreCompleto); }
    public String getTelefono() { return getPhoneNumber(); }
    public void setTelefono(String telefono) { setPhoneNumber(telefono); }
    public String getDireccion() { return getAddress(); }
    public void setDireccion(String direccion) { setAddress(direccion); }
    public Gender getGenero() { return getGender(); }
    public void setGenero(Gender genero) { setGender(genero); }
    public PeruDepartment getDepartamento() { return getDepartment(); }
    public void setDepartamento(PeruDepartment departamento) { setDepartment(departamento); }
    public String getProvincia() { return getProvince(); }
    public void setProvincia(String provincia) { setProvince(provincia); }
    public Role getRol() { return getRole(); }
    public void setRol(Role rol) { setRole(rol); }
    public boolean isActivo() { return active; }
    public void setActivo(boolean activo) { this.active = activo; }
    public LocalDate getFechaRegistro() { return getRegisteredAt(); }
    public void setFechaRegistro(LocalDate fechaRegistro) { setRegisteredAt(fechaRegistro); }
}
