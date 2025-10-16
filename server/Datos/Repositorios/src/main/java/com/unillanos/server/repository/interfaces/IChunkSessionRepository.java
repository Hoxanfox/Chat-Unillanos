package com.unillanos.server.repository.interfaces;

import com.unillanos.server.entity.ChunkSessionEntity;
import com.unillanos.server.entity.EstadoSesion;

import java.util.List;
import java.util.Optional;

/**
 * Interfaz del repositorio para gestionar sesiones de subida de archivos por chunks.
 */
public interface IChunkSessionRepository {
    
    /**
     * Inicia una nueva sesión de subida de archivo.
     *
     * @param session Entidad de la sesión a crear
     * @return Sesión guardada con ID generado
     */
    ChunkSessionEntity iniciarSesion(ChunkSessionEntity session);
    
    /**
     * Registra que un chunk específico ha sido recibido.
     *
     * @param sessionId ID de la sesión
     * @param numeroChunk Número del chunk recibido
     */
    void registrarChunk(String sessionId, int numeroChunk);
    
    /**
     * Obtiene una sesión por su ID.
     *
     * @param sessionId ID de la sesión
     * @return Sesión encontrada o Optional vacío
     */
    Optional<ChunkSessionEntity> obtenerSesion(String sessionId);
    
    /**
     * Actualiza el estado de una sesión.
     *
     * @param sessionId ID de la sesión
     * @param nuevoEstado Nuevo estado de la sesión
     */
    void actualizarEstado(String sessionId, EstadoSesion nuevoEstado);
    
    /**
     * Actualiza la última actividad de una sesión.
     *
     * @param sessionId ID de la sesión
     */
    void actualizarUltimaActividad(String sessionId);
    
    /**
     * Actualiza una sesión completa.
     *
     * @param session Sesión a actualizar
     */
    void actualizarSesion(ChunkSessionEntity session);
    
    /**
     * Elimina una sesión.
     *
     * @param sessionId ID de la sesión a eliminar
     */
    void eliminarSesion(String sessionId);
    
    /**
     * Obtiene todas las sesiones de un usuario.
     *
     * @param usuarioId ID del usuario
     * @return Lista de sesiones del usuario
     */
    List<ChunkSessionEntity> obtenerSesionesPorUsuario(String usuarioId);
    
    /**
     * Limpia sesiones expiradas.
     *
     * @param horasExpiracion Número de horas de expiración
     * @return Número de sesiones eliminadas
     */
    int limpiarSesionesExpiradas(int horasExpiracion);
    
    /**
     * Obtiene todas las sesiones activas.
     *
     * @return Lista de sesiones activas
     */
    List<ChunkSessionEntity> obtenerSesionesActivas();
}
