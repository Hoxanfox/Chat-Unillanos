package com.unillanos.server.logs;

import java.util.Map;

/**
 * Interfaz para observadores de eventos de logging.
 * 
 * Permite que otros componentes del sistema se suscriban a eventos
 * de logging en tiempo real, implementando el patrón Observer.
 * 
 * @author Chat-Unillanos Team
 * @version 1.0.0
 */
@FunctionalInterface
public interface LogObserver {
    
    /**
     * Método llamado cuando ocurre un evento de logging.
     * 
     * @param tipo Tipo de evento (LOG, CONEXION, DESCONEXION, etc.)
     * @param datos Datos del evento en formato Map
     */
    void onLogEvent(String tipo, Map<String, Object> datos);
}
