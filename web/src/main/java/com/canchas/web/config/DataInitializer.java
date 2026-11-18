package com.canchas.web.config;

import com.canchas.web.model.Gender;
import com.canchas.web.model.PeruDepartment;
import com.canchas.web.model.Persona;
import com.canchas.web.model.Role;
import com.canchas.web.model.User;
import com.canchas.web.repository.RoleRepository;
import com.canchas.web.repository.UserRepository;
import com.canchas.web.util.LimaTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${fqs.admin.username:Mark2026}")
    private String adminUsername;

    @Value("${fqs.admin.password:Proyectoweb2026}")
    private String adminPassword;

    @Value("${fqs.admin.email:mark2026@fredyqueensoccer.com}")
    private String adminEmail;

    @Override
    public void run(String... args) {
        Role roleAdmin = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN", "Administrador")));
        Role roleCliente = roleRepository.findByName("ROLE_CLIENTE")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_CLIENTE", "Cliente")));

        if (userRepository.countAdmins() == 0 && userRepository.findByUsername("Mark2026").isEmpty()) {
            Persona persona = new Persona();
            persona.setNombres("Marlon");
            persona.setApellidoPaterno("Quipuzco");
            persona.setApellidoMaterno("Vargas");
            persona.setDni("70000001");
            persona.setEmail(adminEmail);
            persona.setPhoneNumber("949356493");
            persona.setAddress("Trujillo, Perú");
            persona.setGender(Gender.MALE);
            persona.setDepartment(PeruDepartment.LA_LIBERTAD);
            persona.setProvince("Trujillo");
            persona.setRegisteredAt(LimaTime.today());

            User admin = new User();
            admin.setPersona(persona);
            admin.setNickname("Mark");
            admin.setUsername(adminUsername);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setActive(true);

            Set<Role> roles = new HashSet<>();
            roles.add(roleAdmin);
            roles.add(roleCliente);
            admin.setRoles(roles);

            userRepository.save(admin);
            System.out.println("✅ Administrador creado exitosamente: Marlon Quipuzco Vargas (username: " + adminUsername + ").");
        } else {
            System.out.println("ℹ️ Ya existe usuario administrador. Omitiendo creación inicial.");
        }
    }
}
