package com.arquitectura.controlador;

import com.arquitectura.DTO.Comunicacion.DTORequest;

import java.util.Set;

/**
 * Interfaz común para todos los controladores del sistema.
 * Cada controlador maneja un conjunto específico de acciones relacionadas.
 */
public interface IController {
    
    /**
     * Maneja una acción específica del controlador
     * @param action La acción a ejecutar (en minúsculas)
     * @param request El request completo con payload
     * @param handler El handler del cliente para enviar respuestas
     * @return true si la acción fue manejada por este controlador, false si no corresponde
     */
    boolean handleAction(String action, DTORequest request, IClientHandler handler);
    
    /**
     * Retorna las acciones que este controlador puede manejar
     * @return Set de acciones soportadas (en minúsculas)
     */
    Set<String> getSupportedActions();
}
