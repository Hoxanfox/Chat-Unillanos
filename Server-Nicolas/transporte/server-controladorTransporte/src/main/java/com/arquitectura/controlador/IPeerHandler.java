package com.arquitectura.controlador;

import com.arquitectura.DTO.usuarios.UserResponseDto;
import java.util.UUID;

/**
 * Interfaz que define las operaciones para manejar una conexión P2P con otro servidor
 * Extiende IClientHandler para ser compatible con el RequestDispatcher
 */
public interface IPeerHandler extends IClientHandler {
    
    /**
     * Obtiene el ID único del peer conectado
     * @return UUID del peer
     */
    UUID getPeerId();
    
    /**
     * Obtiene la dirección IP del peer
     * @return IP del peer
     */
    String getPeerIp();
    
    /**
     * Obtiene el puerto del peer
     * @return Puerto del peer
     */
    Integer getPeerPort();
    
    // sendMessage() ya viene de IClientHandler
    
    // isConnected() - método específico de peer
    /**
     * Verifica si la conexión está activa
     * @return true si está conectado, false en caso contrario
     */
    boolean isConnected();
    
    // disconnect() - método de cierre
    /**
     * Cierra la conexión con el peer
     */
    void disconnect();
    
    // forceDisconnect() ya viene de IClientHandler
    
    /**
     * Obtiene el timestamp del último heartbeat recibido
     * @return Timestamp en milisegundos
     */
    long getLastHeartbeat();
    
    /**
     * Actualiza el timestamp del último heartbeat
     */
    void updateHeartbeat();
}
