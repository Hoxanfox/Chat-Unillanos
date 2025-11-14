package com.arquitectura.logicaPeers.notifier;

import com.arquitectura.DTO.Comunicacion.DTORequest;
import com.arquitectura.DTO.Comunicacion.DTOResponse;

import java.util.UUID;

public interface PeerNotifier {
    void notificarCambioUsuarioATodosLosPeers(UUID usuarioId, String username, String nuevoEstado, UUID peerId, String peerIp, Integer peerPuerto);
}

