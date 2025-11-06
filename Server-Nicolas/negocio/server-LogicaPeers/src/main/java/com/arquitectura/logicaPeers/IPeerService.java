package com.arquitectura.logicaPeers;

import com.arquitectura.DTO.Comunicacion.DTORequest;
import com.arquitectura.DTO.Comunicacion.DTOResponse;
import com.arquitectura.DTO.p2p.PeerResponseDto;
import com.arquitectura.domain.Peer;

import java.util.List;
import java.util.UUID;

/**
 * Interfaz del servicio de gestión de peers en la red P2P.
 * Define las operaciones para administrar peers, heartbeats y retransmisión de peticiones.
 */
public interface IPeerService {
    
    // ==================== GESTIÓN DE PEERS ====================
    
    /**
     * Agrega un nuevo peer a la red P2P.
     * Si el peer ya existe, actualiza su información.
     * 
     * @param ip Dirección IP del peer
     * @param puerto Puerto del peer
     * @return DTO con la información del peer agregado
     * @throws Exception si hay error al agregar el peer
     */
    PeerResponseDto agregarPeer(String ip, int puerto) throws Exception;
    
    /**
     * Agrega un nuevo peer con nombre de servidor.
     * 
     * @param ip Dirección IP del peer
     * @param puerto Puerto del peer
     * @param nombreServidor Nombre descriptivo del servidor
     * @return DTO con la información del peer agregado
     * @throws Exception si hay error al agregar el peer
     */
    PeerResponseDto agregarPeer(String ip, int puerto, String nombreServidor) throws Exception;
    
    /**
     * Lista todos los peers disponibles en la red.
     * 
     * @return Lista de DTOs con información de todos los peers
     */
    List<PeerResponseDto> listarPeersDisponibles();
    
    /**
     * Lista solo los peers que están activos (ONLINE).
     * 
     * @return Lista de DTOs con información de peers activos
     */
    List<PeerResponseDto> listarPeersActivos();
    
    /**
     * Obtiene la información de un peer específico por su ID.
     * 
     * @param peerId ID del peer
     * @return DTO con la información del peer
     * @throws Exception si el peer no existe
     */
    PeerResponseDto obtenerPeer(UUID peerId) throws Exception;
    
    /**
     * Actualiza el estado de un peer.
     * 
     * @param peerId ID del peer
     * @param estado Nuevo estado ("ONLINE", "OFFLINE", "DESCONOCIDO")
     * @throws Exception si el peer no existe
     */
    void actualizarEstadoPeer(UUID peerId, String estado) throws Exception;
    
    /**
     * Elimina un peer de la red.
     * 
     * @param peerId ID del peer a eliminar
     * @throws Exception si el peer no existe
     */
    void eliminarPeer(UUID peerId) throws Exception;
    
    // ==================== HEARTBEAT ====================
    
    /**
     * Reporta un latido (heartbeat) de un peer.
     * Actualiza el timestamp del último latido y marca el peer como ONLINE.
     * 
     * @param peerId ID del peer que reporta el latido
     * @throws Exception si el peer no existe
     */
    void reportarLatido(UUID peerId) throws Exception;
    
    /**
     * Reporta un latido con información completa del peer.
     * Si el peer no existe, lo crea.
     * 
     * @param peerId ID del peer
     * @param ip IP del peer
     * @param puerto Puerto del peer
     * @throws Exception si hay error al procesar el latido
     */
    void reportarLatido(UUID peerId, String ip, int puerto) throws Exception;
    
    /**
     * Verifica qué peers han excedido el timeout de heartbeat
     * y los marca como OFFLINE.
     * 
     * @return Número de peers marcados como inactivos
     */
    int verificarPeersInactivos();
    
    /**
     * Obtiene el intervalo configurado para heartbeats en milisegundos.
     * 
     * @return Intervalo de heartbeat en ms
     */
    long obtenerIntervaloHeartbeat();
    
    // ==================== RETRANSMISIÓN ====================
    
    /**
     * Retransmite una petición a otro peer en la red.
     * 
     * @param peerDestinoId ID del peer destino
     * @param peticionOriginal Petición original a retransmitir
     * @return Respuesta del peer destino
     * @throws Exception si hay error en la retransmisión
     */
    DTOResponse retransmitirPeticion(UUID peerDestinoId, DTORequest peticionOriginal) throws Exception;
    
    // ==================== PEER ACTUAL ====================
    
    /**
     * Obtiene la información del peer actual (este servidor).
     * 
     * @return Entidad Peer representando este servidor
     */
    Peer obtenerPeerActual();
    
    /**
     * Obtiene el ID del peer actual.
     * 
     * @return UUID del peer actual
     */
    UUID obtenerPeerActualId();
    
    // ==================== ESTADÍSTICAS ====================
    
    /**
     * Cuenta el número total de peers en la red.
     * 
     * @return Número total de peers
     */
    long contarTotalPeers();
    
    /**
     * Cuenta el número de peers activos (ONLINE).
     * 
     * @return Número de peers activos
     */
    long contarPeersActivos();
    
    /**
     * Cuenta el número de peers inactivos (OFFLINE).
     * 
     * @return Número de peers inactivos
     */
    long contarPeersInactivos();
}
