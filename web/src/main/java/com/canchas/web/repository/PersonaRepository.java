package com.canchas.web.repository;

import com.canchas.web.model.Persona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PersonaRepository extends JpaRepository<Persona, Long> {

    @Query("SELECT p FROM Persona p WHERE p.dni = :dni")
    Optional<Persona> findByDni(@Param("dni") String dni);

    @Query("SELECT p FROM Persona p WHERE p.email = :email")
    Optional<Persona> findByEmail(@Param("email") String email);

    @Query("SELECT COUNT(p) > 0 FROM Persona p WHERE p.dni = :dni")
    boolean existsByDni(@Param("dni") String dni);

    @Query("SELECT COUNT(p) > 0 FROM Persona p WHERE p.email = :email")
    boolean existsByEmail(@Param("email") String email);
}
