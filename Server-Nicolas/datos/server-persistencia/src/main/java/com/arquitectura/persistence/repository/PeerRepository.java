package com.arquitectura.persistence.repository;

import com.arquitectura.domain.Peer;
import com.arquitectura.domain.enums.EstadoPeer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio para la gestión de peers en la red P2P.
 * Proporciona métodos para consultar, actualizar y gestionar el estado de los peers.
 */
public interface PeerRepository extends JpaRepository<Peer, UUID> {
    
    /**
     * Busca un peer por su ID
     */
    Peer findByPeerId(UUID peerId);

    /**
     * Busca un peer por su dirección IP
     */
    Optional<Peer> findByIp(String ip);

    /**
     * Busca un peer por su IP y puerto
     * Útil para evitar duplicados en la red
     */
    List<Peer> findByIpAndPuerto(String ip, int puerto);

    /**
     * Obtiene todos los peers con un estado específico
     * @param conectado Estado del peer (ONLINE, OFFLINE, DESCONOCIDO)
     */
    List<Peer> findByConectado(EstadoPeer conectado);

    /**
     * Obtiene todos los peers ordenados por el último latido (más reciente primero)
     */
    List<Peer> findAllByOrderByUltimoLatidoDesc();

    /**
     * Obtiene peers activos (ONLINE) ordenados por último latido
     */
    @Query("SELECT p FROM Peer p WHERE p.conectado = 'ONLINE' ORDER BY p.ultimoLatido DESC")
    List<Peer> findPeersActivos();

    /**
     * Actualiza el estado de un peer
     */
    @Modifying
    @Query("UPDATE Peer p SET p.conectado = :estado WHERE p.peerId = :peerId")
    void actualizarEstado(@Param("peerId") UUID peerId, @Param("estado") EstadoPeer estado);

    /**
     * Actualiza el timestamp del último latido de un peer
     */
    @Modifying
    @Query("UPDATE Peer p SET p.ultimoLatido = :timestamp WHERE p.peerId = :peerId")
    void actualizarLatido(@Param("peerId") UUID peerId, @Param("timestamp") LocalDateTime timestamp);

    /**
     * Actualiza el estado y el timestamp del último latido en una sola operación
     */
    @Modifying
    @Query("UPDATE Peer p SET p.conectado = :estado, p.ultimoLatido = :timestamp WHERE p.peerId = :peerId")
    void actualizarEstadoYLatido(@Param("peerId") UUID peerId, @Param("estado") EstadoPeer estado, @Param("timestamp") LocalDateTime timestamp);

    /**
     * Encuentra peers que no han enviado latido desde hace X segundos
     * @param limiteTimeout Fecha/hora límite para considerar un peer inactivo
     */
    @Query("SELECT p FROM Peer p WHERE p.ultimoLatido < :limiteTimeout AND p.conectado = 'ONLINE'")
    List<Peer> findPeersInactivos(@Param("limiteTimeout") LocalDateTime limiteTimeout);

    /**
     * Cuenta el número de peers activos (ONLINE)
     */
    @Query("SELECT COUNT(p) FROM Peer p WHERE p.conectado = 'ONLINE'")
    long contarPeersActivos();



}
