package com.arquitectura.domain.enums;

/**
 * Enum que representa el estado de conexión de un peer en la red P2P.
 */
public enum EstadoPeer {
    /**
     * El peer está activo y respondiendo a heartbeats
     */
    ONLINE,
    
    /**
     * El peer no está respondiendo o se ha desconectado
     */
    OFFLINE,
    
    /**
     * Estado inicial o cuando no se ha podido determinar el estado
     */
    DESCONOCIDO
}
