package com.canchas.web.service.impl;

import com.canchas.web.dto.ClientProfileDTO;
import com.canchas.web.dto.UserRegistrationDTO;
import com.canchas.web.dto.UserRequestDTO;
import com.canchas.web.exception.DuplicateDataException;
import com.canchas.web.exception.ResourceNotFoundException;
import com.canchas.web.model.Gender;
import com.canchas.web.model.Persona;
import com.canchas.web.model.Role;
import com.canchas.web.model.User;
import com.canchas.web.repository.RoleRepository;
import com.canchas.web.repository.UserRepository;
import com.canchas.web.repository.UserRoleRepository;
import com.canchas.web.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service("userService")
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    @Override
    @Transactional(readOnly = true)
    public List<com.canchas.web.dto.ClientSearchResultDTO> searchClients(String criterion, String value) {
        var search = com.canchas.web.validation.ClientSearchCriteria.parse(criterion, value);
        if (search.value().isEmpty()) return List.of();
        var limit = PageRequest.of(0, 10);
        List<User> users;
        if ("NOMBRE".equals(search.criterion())) {
            users = userRepository.searchClientsByName(search.value(), limit);
        } else if (search.value().length() == 8) {
            users = userRepository.searchClientsByExactDni(search.value(), limit);
        } else {
            users = userRepository.searchClientsByDniPrefix(search.value(), limit);
        }
        return users.stream().map(com.canchas.web.dto.ClientSearchResultDTO::from).toList();
    }

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public boolean existsByDni(String dni) {
        return userRepository.existsByDni(dni);
    }

    private Role getOrCreateRole(String roleName, String description) {
        return roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(new Role(roleName, description)));
    }

    @Override
    @Transactional
    public User create(UserRequestDTO dto) {
        if (dto.getDni() != null && !dto.getDni().isBlank()) {
            if (!dto.getDni().matches("^[0-9]{8}$")) {
                throw new IllegalArgumentException("El DNI debe contener exactamente 8 dígitos.");
            }
            if (userRepository.existsByDni(dto.getDni())) {
                throw new DuplicateDataException("dni", "El DNI ya se encuentra registrado.");
            }
        }
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new DuplicateDataException("username", "El nombre de usuario '" + dto.getUsername() + "' ya está en uso");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateDataException("email", "El correo '" + dto.getEmail() + "' ya está registrado");
        }
        if (dto.getRole() == User.Role.CLIENTE) {
            if (dto.getPhoneNumber() == null || !dto.getPhoneNumber().matches("^9[0-9]{8}$")) {
                throw new IllegalArgumentException("El celular debe contener 9 dígitos y comenzar con 9.");
            }
        } else if (dto.getPhoneNumber() != null && !dto.getPhoneNumber().isBlank()) {
            if (!dto.getPhoneNumber().matches("^9[0-9]{8}$")) {
                throw new IllegalArgumentException("El celular debe contener 9 dígitos y comenzar con 9.");
            }
        }

        Persona persona = new Persona();
        persona.setFullName(dto.getFullName());
        persona.setDni(dto.getDni() != null && !dto.getDni().isBlank() ? dto.getDni() : null);
        persona.setEmail(dto.getEmail());
        persona.setPhoneNumber(dto.getPhoneNumber());
        persona.setAddress(dto.getAddress());
        persona.setDepartment(dto.getDepartment());
        persona.setProvince(com.canchas.web.util.PeruLocationCatalog.getCanonicalProvinceName(dto.getDepartment(), dto.getProvince()));
        persona.setGender(dto.getGender() != null ? dto.getGender() : Gender.PREFER_NOT_TO_SAY);
        persona.setRegisteredAt(com.canchas.web.util.LimaTime.today());

        User user = new User();
        user.setPersona(persona);
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setActive(true);

        Set<Role> roles = new HashSet<>();
        if (dto.getRole() == User.Role.ADMIN) {
            roles.add(getOrCreateRole("ROLE_ADMIN", "Administrador"));
            roles.add(getOrCreateRole("ROLE_CLIENTE", "Cliente"));
        } else {
            roles.add(getOrCreateRole("ROLE_CLIENTE", "Cliente"));
        }
        user.setRoles(roles);

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User registerClient(UserRegistrationDTO dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }
        if (dto.getDni() == null || !dto.getDni().matches("^[0-9]{8}$")) {
            throw new IllegalArgumentException("El DNI debe contener exactamente 8 dígitos.");
        }
        if (dto.getPhoneNumber() == null || !dto.getPhoneNumber().matches("^9[0-9]{8}$")) {
            throw new IllegalArgumentException("El celular debe contener 9 dígitos y comenzar con 9.");
        }
        if (dto.getDepartment() == null) {
            throw new IllegalArgumentException("El departamento es obligatorio para el cliente.");
        }
        if (dto.getProvince() == null || dto.getProvince().isBlank()) {
            throw new IllegalArgumentException("La provincia es obligatoria para el cliente.");
        }
        if (!com.canchas.web.util.PeruLocationCatalog.isValidProvince(dto.getDepartment(), dto.getProvince())) {
            throw new IllegalArgumentException("La provincia no pertenece al departamento seleccionado.");
        }
        if (userRepository.existsByDni(dto.getDni())) {
            throw new DuplicateDataException("dni", "El DNI ya se encuentra registrado.");
        }
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new DuplicateDataException("username", "El nombre de usuario '" + dto.getUsername() + "' ya está en uso");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateDataException("email", "El correo '" + dto.getEmail() + "' ya está registrado");
        }

        Persona persona = new Persona();
        persona.setFullName(dto.getFullName());
        persona.setDni(dto.getDni());
        persona.setEmail(dto.getEmail());
        persona.setPhoneNumber(dto.getPhoneNumber());
        persona.setDepartment(dto.getDepartment());
        persona.setProvince(com.canchas.web.util.PeruLocationCatalog.getCanonicalProvinceName(dto.getDepartment(), dto.getProvince()));
        persona.setGender(dto.getGender() != null ? dto.getGender() : Gender.PREFER_NOT_TO_SAY);
        persona.setAddress(dto.getAddress());
        persona.setRegisteredAt(com.canchas.web.util.LimaTime.today());

        User user = new User();
        user.setPersona(persona);
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setActive(true);

        Set<Role> roles = new HashSet<>();
        roles.add(getOrCreateRole("ROLE_CLIENTE", "Cliente"));
        user.setRoles(roles);

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User update(Long id, UserRequestDTO dto) {
        User user = findById(id);
        if (!user.getUsername().equals(dto.getUsername()) && userRepository.existsByUsername(dto.getUsername())) {
            throw new DuplicateDataException("username", "El nombre de usuario '" + dto.getUsername() + "' ya está en uso");
        }
        if (!user.getEmail().equals(dto.getEmail()) && userRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateDataException("email", "El correo '" + dto.getEmail() + "' ya está registrado");
        }
        if (dto.getDni() != null && !dto.getDni().isBlank()) {
            if (!dto.getDni().matches("^[0-9]{8}$")) {
                throw new IllegalArgumentException("El DNI debe contener exactamente 8 dígitos.");
            }
            if ((user.getDni() == null || !user.getDni().equals(dto.getDni())) && userRepository.existsByDni(dto.getDni())) {
                throw new DuplicateDataException("dni", "El DNI ya se encuentra registrado.");
            }
            user.setDni(dto.getDni());
        }
        if (dto.getRole() == User.Role.CLIENTE) {
            if (dto.getPhoneNumber() == null || !dto.getPhoneNumber().matches("^9[0-9]{8}$")) {
                throw new IllegalArgumentException("El celular debe contener 9 dígitos y comenzar con 9.");
            }
        } else if (dto.getPhoneNumber() != null && !dto.getPhoneNumber().isBlank()) {
            if (!dto.getPhoneNumber().matches("^9[0-9]{8}$")) {
                throw new IllegalArgumentException("El celular debe contener 9 dígitos y comenzar con 9.");
            }
        }
        user.setUsername(dto.getUsername());
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setAddress(dto.getAddress());
        if (dto.getDepartment() != null) {
            user.setDepartment(dto.getDepartment());
        }
        user.setGender(dto.getGender() != null ? dto.getGender() : Gender.PREFER_NOT_TO_SAY);
        user.setRole(dto.getRole());
        if (dto.getActive() != null) {
            user.setActive(dto.getActive());
        }
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User updateProfile(Long id, ClientProfileDTO dto) {
        User user = findById(id);
        if (!user.getEmail().equals(dto.getEmail()) && userRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateDataException("email", "El correo '" + dto.getEmail() + "' ya está registrado");
        }
        if (dto.getPhoneNumber() == null || !dto.getPhoneNumber().matches("^9[0-9]{8}$")) {
            throw new IllegalArgumentException("El celular debe contener 9 dígitos y comenzar con 9.");
        }
        if (dto.getDepartment() == null) {
            throw new IllegalArgumentException("El departamento es obligatorio.");
        }
        if (dto.getProvince() == null || dto.getProvince().isBlank()) {
            throw new IllegalArgumentException("La provincia es obligatoria.");
        }
        if (!com.canchas.web.util.PeruLocationCatalog.isValidProvince(dto.getDepartment(), dto.getProvince())) {
            throw new IllegalArgumentException("La provincia no pertenece al departamento seleccionado.");
        }
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setDepartment(dto.getDepartment());
        user.setProvince(com.canchas.web.util.PeruLocationCatalog.getCanonicalProvinceName(dto.getDepartment(), dto.getProvince()));
        user.setGender(dto.getGender() != null ? dto.getGender() : Gender.PREFER_NOT_TO_SAY);
        user.setAddress(dto.getAddress());
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        User user = findById(id);
        userRepository.delete(user);
    }

    @Override
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
    }

    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + username));
    }

    @Override
    public List<User> findByRole(User.Role role) {
        return userRepository.findByRole(role);
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public Optional<User> findActiveClientByDni(String dni) {
        if (dni == null || !dni.matches("^[0-9]{8}$")) {
            return Optional.empty();
        }
        return userRepository.findByDniAndActiveTrue(dni)
                .filter(u -> u.hasRole("ROLE_CLIENTE") || u.hasRole("CLIENTE"));
    }

    @Override
    public long countClients() {
        return userRoleRepository.countByRoleId(2L);
    }

    @Override
    public long countAdmins() {
        return userRoleRepository.countByRoleId(1L);
    }

    @Override
    public Page<User> findClientsPaginated(int page, int size, String search) {
        int pageNumber = Math.max(0, page);
        int pageSize = size > 0 ? size : 15;
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by("id").ascending());

        if (search != null && !search.trim().isBlank()) {
            return userRepository.searchClients(search.trim(), pageable);
        }

        List<Long> ids = userRoleRepository.findUserIdsByRoleId(2L, pageable);
        long total = userRoleRepository.countByRoleId(2L);

        if (ids.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, total);
        }

        List<User> clients = userRepository.findByIdsJPQL(ids);
        return new PageImpl<>(clients, pageable, total);
    }
}
