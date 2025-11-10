package com.arquitectura.persistence.repository;

import com.arquitectura.domain.User;
import com.arquitectura.domain.Peer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    User findByUserId(UUID userId);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.peerId WHERE u.userId = :id")
    Optional<User> findByIdWithPeer(@Param("id") UUID id);
    
    /**
     * Cuenta cuántos usuarios están asociados a un peer específico.
     */
    long countByPeerId(Peer peer);
    
    /**
     * Busca todos los usuarios asociados a un peer específico.
     */
    List<User> findByPeerId(Peer peer);
}