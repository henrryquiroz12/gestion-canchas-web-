package com.canchas.web.service;

import com.canchas.web.dto.ClientProfileDTO;
import com.canchas.web.dto.UserRegistrationDTO;
import com.canchas.web.dto.UserRequestDTO;
import com.canchas.web.model.User;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface UserService {
    List<com.canchas.web.dto.ClientSearchResultDTO> searchClients(String criterion, String value);

    boolean existsByDni(String dni);

    User create(UserRequestDTO dto);
    User registerClient(UserRegistrationDTO dto);
    User update(Long id, UserRequestDTO dto);
    User updateProfile(Long id, ClientProfileDTO dto);
    void delete(Long id);
    User findById(Long id);
    User findByUsername(String username);
    List<User> findByRole(User.Role role);
    List<User> findAll();
    Optional<User> findActiveClientByDni(String dni);

    long countClients();
    long countAdmins();
    Page<User> findClientsPaginated(int page, int size, String search);
}
