package com.arquitectura.persistence.repository;

import com.arquitectura.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param; // <-- 1. IMPORTA ESTO
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    User findByUserId(UUID userId);

    // --- AQUÍ ESTÁ LA CORRECCIÓN ---
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.peerId WHERE u.userId = :id")
    Optional<User> findByIdWithPeer(@Param("id") UUID id); // <-- 2. AGREGA @Param("id") AQUÍ
}