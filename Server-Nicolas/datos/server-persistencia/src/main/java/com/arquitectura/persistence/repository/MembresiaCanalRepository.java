package com.arquitectura.persistence.repository;

import com.arquitectura.domain.MembresiaCanal;
import com.arquitectura.domain.MembresiaCanalId;
import com.arquitectura.domain.enums.EstadoMembresia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MembresiaCanalRepository extends JpaRepository<MembresiaCanal, MembresiaCanalId> {
    List<MembresiaCanal> findAllByUsuarioUserIdAndEstado(UUID userId, EstadoMembresia estado);
    List<MembresiaCanal> findAllByCanal_ChannelIdAndEstado(UUID channelId, EstadoMembresia estado);
    @Query("SELECT m FROM MembresiaCanal m JOIN FETCH m.canal c JOIN FETCH c.owner WHERE m.id.idUsuario = :userId AND m.estado = :estado")
    List<MembresiaCanal> findActiveMembresiasByUserIdWithDetails(@Param("userId") UUID userId, @Param("estado") EstadoMembresia estado);
    @Query("SELECT m FROM MembresiaCanal m JOIN FETCH m.canal c JOIN FETCH c.owner WHERE m.id.idUsuario = :userId AND m.estado = :estado")
    List<MembresiaCanal> findPendingMembresiasByUserIdWithDetails(@Param("userId") UUID userId, @Param("estado") EstadoMembresia estado);
}