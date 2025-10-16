package com.unillanos.server.repository.interfaces;

import com.unillanos.server.entity.CanalEntity;

import java.util.List;
import java.util.Optional;

/**
 * Interfaz del repositorio de canales.
 * Define operaciones CRUD para la entidad Canal.
 */
public interface ICanalRepository {
    
    // --- MÉTODOS DE CONSULTA ---
    
    /**
     * Busca un canal por su ID.
     *
     * @param id ID del canal
     * @return Optional con el canal si existe
     */
    Optional<CanalEntity> findById(String id);
    
    /**
     * Busca un canal por su nombre.
     *
     * @param nombre Nombre del canal
     * @return Optional con el canal si existe
     */
    Optional<CanalEntity> findByNombre(String nombre);
    
    /**
     * Verifica si existe un canal con el nombre dado.
     *
     * @param nombre Nombre a verificar
     * @return true si existe un canal con ese nombre
     */
    boolean existsByNombre(String nombre);
    
    /**
     * Obtiene todos los canales activos con paginación.
     *
     * @param limit Número máximo de resultados
     * @param offset Desplazamiento inicial
     * @return Lista de canales
     */
    List<CanalEntity> findAll(int limit, int offset);
    
    /**
     * Obtiene los canales de un usuario específico con paginación.
     *
     * @param usuarioId ID del usuario
     * @param limit Número máximo de resultados
     * @param offset Desplazamiento inicial
     * @return Lista de canales del usuario
     */
    List<CanalEntity> findByUsuario(String usuarioId, int limit, int offset);
    
    /**
     * Cuenta la cantidad de miembros de un canal.
     *
     * @param canalId ID del canal
     * @return Cantidad de miembros
     */
    int countMiembros(String canalId);
    
    // --- MÉTODOS DE ESCRITURA ---
    
    /**
     * Guarda un nuevo canal.
     *
     * @param canal Entidad del canal
     * @return Canal guardado
     */
    CanalEntity save(CanalEntity canal);
    
    /**
     * Actualiza un canal existente.
     *
     * @param canal Entidad del canal con datos actualizados
     */
    void update(CanalEntity canal);
    
    /**
     * Actualiza el estado activo de un canal.
     *
     * @param id ID del canal
     * @param activo Nuevo estado
     */
    void updateActivo(String id, boolean activo);
    
    /**
     * Elimina un canal por su ID.
     *
     * @param id ID del canal
     */
    void deleteById(String id);
}

