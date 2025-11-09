package com.arquitectura.fachada.p2p;

import com.arquitectura.DTO.Comunicacion.DTORequest;
import com.arquitectura.DTO.Comunicacion.DTOResponse;

import java.util.List;
import java.util.UUID;

/**
 * Fachada especializada para operaciones relacionadas con la red P2P.
 */
public interface IP2PFachada {

    /**
     * Agrega un nuevo peer a la red P2P.
     * @param ip Dirección IP del peer
     * @param puerto Puerto del peer
     * @return DTO con la información del peer agregado
     * @throws Exception si hay error al agregar el peer
     */
    com.arquitectura.DTO.p2p.PeerResponseDto agregarPeer(String ip, int puerto) throws Exception;

    /**
     * Agrega un nuevo peer con nombre de servidor.
     * @param ip Dirección IP del peer
     * @param puerto Puerto del peer
     * @param nombreServidor Nombre descriptivo del servidor
     * @return DTO con la información del peer agregado
     * @throws Exception si hay error al agregar el peer
     */
    com.arquitectura.DTO.p2p.PeerResponseDto agregarPeer(String ip, int puerto, String nombreServidor) throws Exception;

    /**
     * Lista todos los peers disponibles en la red.
     * @return Lista de DTOs con información de todos los peers
     */
    List<com.arquitectura.DTO.p2p.PeerResponseDto> listarPeersDisponibles();

    /**
     * Lista solo los peers que están activos (ONLINE).
     * @return Lista de DTOs con información de peers activos
     */
    List<com.arquitectura.DTO.p2p.PeerResponseDto> listarPeersActivos();

    /**
     * Reporta un latido (heartbeat) de un peer.
     * @param peerId ID del peer que reporta el latido
     * @throws Exception si el peer no existe
     */
    void reportarLatido(UUID peerId) throws Exception;

    /**
     * Reporta un latido con información completa del peer.
     * @param peerId ID del peer
     * @param ip IP del peer
     * @param puerto Puerto del peer
     * @throws Exception si el peer no existe
     */
    void reportarLatido(UUID peerId, String ip, int puerto) throws Exception;

    /**
     * Obtiene el intervalo de heartbeat configurado.
     * @return Intervalo en milisegundos
     */
    long obtenerIntervaloHeartbeat();

    /**
     * Retransmite una petición a otro peer.
     * @param peerDestinoId ID del peer destino
     * @param peticionOriginal Petición original a retransmitir
     * @return Respuesta del peer destino
     * @throws Exception si hay error en la retransmisión
     */
    DTOResponse retransmitirPeticion(UUID peerDestinoId, DTORequest peticionOriginal) throws Exception;

    /**
     * Descarga un archivo completo desde otro peer usando las rutas startFileDownload y requestFileChunk.
     * @param peerDestinoId ID del peer desde donde descargar el archivo
     * @param fileId ID del archivo a descargar
     * @return Bytes del archivo completo descargado
     * @throws Exception si hay error en la descarga
     */
    byte[] descargarArchivoDesdePeer(UUID peerDestinoId, String fileId) throws Exception;

    /**
     * Busca en qué peer está conectado un usuario específico.
     * @param usuarioId ID del usuario a buscar
     * @return DTO con información del usuario y el peer donde está conectado
     * @throws Exception si el usuario no existe
     */
    com.arquitectura.DTO.p2p.UserLocationResponseDto buscarUsuario(UUID usuarioId) throws Exception;
}
