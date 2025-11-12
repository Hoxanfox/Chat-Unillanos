// java
package com.arquitectura.controlador.peer;

import java.util.List;
import java.util.Map;

/**
 * Interfaz para broadcasting de actualizaciones de lista de contactos
 * a todos los peers conectados en la red P2P
 */
public interface IContactListBroadcaster {

    /**
     * Difunde una actualización completa de la lista de contactos a todos los clientes
     * @param data Datos completos de la lista de contactos con formato Map
     */
    void broadcastContactListUpdate(Map<String, Object> data);

    /**
     * Difunde una actualización de usuario específico a todos los peers activos
     * @param userId ID del usuario cuyo estado cambió
     * @param action Acción realizada (ONLINE, OFFLINE, etc)
     */
    void broadcastUserStatusUpdate(String userId, String action);

    /**
     * Difunde una actualización de usuario a peers específicos
     * @param userId ID del usuario
     * @param action Acción realizada
     * @param targetPeerIds Lista de IDs de peers destino
     */
    void broadcastToSpecificPeers(String userId, String action, List<String> targetPeerIds);

    /**
     * Notifica a todos los peers sobre un cambio en el sistema
     * @param message Mensaje de notificación
     */
    void notifyAllPeers(String message);
}
