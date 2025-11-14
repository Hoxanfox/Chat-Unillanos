package com.arquitectura.logicaPeers.transfer;

import com.arquitectura.DTO.Comunicacion.DTORequest;
import com.arquitectura.DTO.Comunicacion.DTOResponse;

import java.util.UUID;

public interface FileTransferService {
    byte[] descargarArchivoDesdePeer(UUID peerDestinoId, String fileId) throws Exception;
}

