package com.unillanos.server.repository.interfaces;

import com.unillanos.server.entity.ContactoEntity;

import java.util.List;
import java.util.Optional;

/**
 * Interfaz para el repositorio de contactos.
 */
public interface IContactoRepository {
    
    /**
     * Guarda una nueva relación de contacto.
     */
    ContactoEntity save(ContactoEntity contacto);
    
    /**
     * Busca una relación de contacto por ID.
     */
    Optional<ContactoEntity> findById(String id);
    
    /**
     * Busca relación entre dos usuarios.
     */
    Optional<ContactoEntity> findByUsuarios(String usuarioId, String contactoId);
    
    /**
     * Obtiene todos los contactos activos de un usuario.
     */
    List<ContactoEntity> findContactosActivos(String usuarioId);
    
    /**
     * Obtiene solicitudes pendientes enviadas por un usuario.
     */
    List<ContactoEntity> findSolicitudesEnviadas(String usuarioId);
    
    /**
     * Obtiene solicitudes pendientes recibidas por un usuario.
     */
    List<ContactoEntity> findSolicitudesRecibidas(String usuarioId);
    
    /**
     * Verifica si dos usuarios son contactos activos.
     */
    boolean sonContactos(String usuarioId, String contactoId);
    
    /**
     * Verifica si hay una solicitud pendiente entre dos usuarios.
     */
    boolean tieneSolicitudPendiente(String usuarioId, String contactoId);
    
    /**
     * Actualiza el estado de una relación.
     */
    void actualizarEstado(String id, String nuevoEstado);
    
    /**
     * Elimina una relación de contacto.
     */
    void delete(String id);
}
