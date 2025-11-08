package com.arquitectura.controlador;

import java.util.UUID;

/**
 * Interfaz que define las operaciones para manejar una conexión P2P con otro servidor
 * Similar a IClientHandler pero para comunicación peer-to-peer
 */
public interface IPeerHandler {
    
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
    
    /**
     * Envía un mensaje al peer
     * @param message Mensaje en formato JSON
     */
    void sendMessage(String message);
    
    /**
     * Verifica si la conexión está activa
     * @return true si está conectado, false en caso contrario
     */
    boolean isConnected();
    
    /**
     * Cierra la conexión con el peer
     */
    void disconnect();
    
    /**
     * Fuerza el cierre de la conexión
     */
    void forceDisconnect();
    
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

