package com.arquitectura.persistence.repository;

import com.arquitectura.domain.Channel;
import com.arquitectura.domain.enums.TipoCanal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChannelRepository extends JpaRepository<Channel, Integer> {
    @Query("SELECT c FROM Channel c JOIN c.membresias m1 JOIN c.membresias m2 " +
            "WHERE c.tipo = :tipo " +
            "AND m1.usuario.userId = :user1Id " +
            "AND m2.usuario.userId = :user2Id " +
            "AND SIZE(c.membresias) = 2")
    Optional<Channel> findDirectChannelBetweenUsers(@Param("tipo") TipoCanal tipo,
                                                    @Param("user1Id") int user1Id,
                                                    @Param("user2Id") int user2Id);

    @Query("SELECT DISTINCT c FROM Channel c LEFT JOIN FETCH c.owner o LEFT JOIN FETCH c.membresias m LEFT JOIN FETCH m.usuario u")
    List<Channel> findAllWithMembresiasAndUsuarios();
}