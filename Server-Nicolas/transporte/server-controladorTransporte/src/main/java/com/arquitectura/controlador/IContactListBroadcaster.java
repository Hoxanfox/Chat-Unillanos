// java
package com.arquitectura.controlador;

import java.util.UUID;

/**
 * Interfaz para hacer broadcast de la lista de contactos a todos los clientes conectados
 */
public interface IContactListBroadcaster {
    /**
     * Envía la lista de contactos actualizada a todos los clientes conectados
     * @param contactListData Los datos de la lista de contactos
     */
    void broadcastContactListUpdate(Object contactListData);

    /**
     * Firma nueva usada por RequestDispatcher.
     * Default delega a broadcastContactListUpdate para compatibilidad
     * con implementaciones existentes.
     * @param userId Usuario origen (puede ser utilizado por la implementación)
     * @param contactListData Datos de la lista de contactos
     */
    default void broadcastContactList(UUID userId, Object contactListData) {
        // Implementations that need the userId can override this method.
        broadcastContactListUpdate(contactListData);
    }
}
