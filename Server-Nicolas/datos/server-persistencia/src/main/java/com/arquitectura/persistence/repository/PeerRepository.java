package com.arquitectura.persistence.repository;

import com.arquitectura.domain.Peer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PeerRepository  extends JpaRepository<Peer, UUID> {
    Peer findByPeerId(UUID peerId);

    Optional<Peer> findByIp(String ip);
}
