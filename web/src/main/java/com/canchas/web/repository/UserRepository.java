package com.canchas.web.repository;

import com.canchas.web.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u JOIN FETCH u.persona p WHERE EXISTS (SELECT r.id FROM u.roles r WHERE r.name = 'ROLE_CLIENTE') AND p.dni = :dni ORDER BY u.id")
    List<User> searchClientsByExactDni(@Param("dni") String dni, Pageable limit);

    @Query("SELECT u FROM User u JOIN FETCH u.persona p WHERE EXISTS (SELECT r.id FROM u.roles r WHERE r.name = 'ROLE_CLIENTE') AND p.dni LIKE CONCAT(:prefix, '%') ORDER BY p.dni, u.id")
    List<User> searchClientsByDniPrefix(@Param("prefix") String prefix, Pageable limit);

    @Query("SELECT u FROM User u JOIN FETCH u.persona p WHERE EXISTS (SELECT r.id FROM u.roles r WHERE r.name = 'ROLE_CLIENTE') AND LOWER(CONCAT(p.nombres, ' ', p.apellidoPaterno, ' ', p.apellidoMaterno)) LIKE LOWER(CONCAT('%', :name, '%')) ORDER BY p.nombres, p.apellidoPaterno, p.apellidoMaterno, u.id")
    List<User> searchClientsByName(@Param("name") String name, Pageable limit);

    @Query("SELECT u FROM User u WHERE u.username = :username")
    Optional<User> findByUsername(@Param("username") String username);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.username = :username")
    boolean existsByUsername(@Param("username") String username);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.persona.dni = :dni")
    boolean existsByDni(@Param("dni") String dni);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.persona.email = :email")
    boolean existsByEmail(@Param("email") String email);

    @Query("SELECT u FROM User u WHERE u.persona.dni = :dni")
    Optional<User> findByDni(@Param("dni") String dni);

    @Query("SELECT u FROM User u WHERE u.persona.dni = :dni AND u.active = true")
    Optional<User> findByDniAndActiveTrue(@Param("dni") String dni);

    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE (:role = 'ADMIN' AND (r.name = 'ROLE_ADMIN' OR r.name = 'ADMIN')) OR (:role = 'CLIENTE' AND (r.name = 'ROLE_CLIENTE' OR r.name = 'CLIENTE'))")
    List<User> findByRoleName(@Param("role") String role);

    default List<User> findByRole(User.Role role) {
        return findByRoleName(role.name());
    }

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = 'ROLE_CLIENTE'")
    long countClients();

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = 'ROLE_ADMIN'")
    long countAdmins();

    @Query(value = "SELECT u FROM User u JOIN u.roles r WHERE r.name = 'ROLE_CLIENTE' ORDER BY u.id ASC",
           countQuery = "SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = 'ROLE_CLIENTE'")
    Page<User> findClients(Pageable pageable);

    @Query(value = "SELECT u FROM User u JOIN u.roles r WHERE r.name = 'ROLE_CLIENTE' AND (u.persona.dni = :search OR u.username = :search OR u.persona.email = :search OR LOWER(u.persona.nombres) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.persona.apellidoPaterno) LIKE LOWER(CONCAT('%', :search, '%'))) ORDER BY u.id ASC",
           countQuery = "SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = 'ROLE_CLIENTE' AND (u.persona.dni = :search OR u.username = :search OR u.persona.email = :search OR LOWER(u.persona.nombres) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.persona.apellidoPaterno) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> searchClients(@Param("search") String search, Pageable pageable);

    @Query("SELECT u.id FROM User u JOIN u.roles r WHERE r.name = 'ROLE_CLIENTE' ORDER BY u.id ASC")
    List<Long> findClientIdsJPQL(Pageable pageable);

    @Query("SELECT u.id FROM User u JOIN u.roles r WHERE r.name = 'ROLE_CLIENTE' AND (u.persona.dni = :search OR u.username = :search OR u.persona.email = :search OR LOWER(u.persona.nombres) LIKE LOWER(CONCAT(:search, '%')) OR LOWER(u.persona.apellidoPaterno) LIKE LOWER(CONCAT(:search, '%'))) ORDER BY u.id ASC")
    List<Long> searchClientIdsJPQL(@Param("search") String search, Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = 'ROLE_CLIENTE' AND (u.persona.dni = :search OR u.username = :search OR u.persona.email = :search OR LOWER(u.persona.nombres) LIKE LOWER(CONCAT(:search, '%')) OR LOWER(u.persona.apellidoPaterno) LIKE LOWER(CONCAT(:search, '%')))")
    long countSearchClientsJPQL(@Param("search") String search);

    @Query("SELECT u FROM User u WHERE u.id IN :ids ORDER BY u.id ASC")
    List<User> findByIdsJPQL(@Param("ids") List<Long> ids);
}
