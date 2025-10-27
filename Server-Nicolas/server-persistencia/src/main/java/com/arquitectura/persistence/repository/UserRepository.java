package com.arquitectura.persistence.repository;

import com.arquitectura.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository // 1. Le dice a Spring que esta es una clase de persistencia.
public interface UserRepository extends JpaRepository<User, Integer> { // 2. Hereda todos los métodos CRUD básicos (save, findById, findAll, delete, etc.).

    // 3. Spring Data JPA crea automáticamente la implementación de este método basándose en su nombre.
    Optional<User> findByUsername(String username);
}