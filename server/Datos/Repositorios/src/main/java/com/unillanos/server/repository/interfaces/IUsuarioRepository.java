package com.unillanos.server.repository.interfaces;

import com.unillanos.server.entity.EstadoUsuario;
import com.unillanos.server.entity.UsuarioEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interfaz del repositorio de usuarios.
 * Define operaciones CRUD para la entidad Usuario.
 */
public interface IUsuarioRepository {
    
    // --- MÉTODOS DE CONSULTA ---
    
    /**
     * Busca un usuario por su ID.
     *
     * @param id ID del usuario
     * @return Optional con el usuario si existe
     */
    Optional<UsuarioEntity> findById(String id);
    
    /**
     * Busca un usuario por su email.
     *
     * @param email Email del usuario
     * @return Optional con el usuario si existe
     */
    Optional<UsuarioEntity> findByEmail(String email);
    
    /**
     * Verifica si existe un usuario con el email dado.
     *
     * @param email Email a verificar
     * @return true si existe un usuario con ese email
     */
    boolean existsByEmail(String email);
    
    /**
     * Verifica si existe un usuario con el ID dado.
     *
     * @param id ID a verificar
     * @return true si existe un usuario con ese ID
     */
    boolean existsById(String id);
    
    /**
     * Obtiene todos los usuarios con paginación.
     *
     * @param limit Número máximo de resultados
     * @param offset Desplazamiento inicial
     * @return Lista de usuarios
     */
    List<UsuarioEntity> findAll(int limit, int offset);
    
    /**
     * Obtiene todos los usuarios.
     *
     * @return Lista de usuarios
     */
    List<UsuarioEntity> findAll();

    /**
     * Obtiene usuarios por estado con paginación.
     */
    List<UsuarioEntity> findByEstado(EstadoUsuario estado, int limit, int offset);

    /**
     * Cuenta todos los usuarios.
     */
    int countAll();

    /**
     * Cuenta usuarios por estado.
     */
    int countByEstado(EstadoUsuario estado);
    
    // --- MÉTODOS DE ESCRITURA ---
    
    /**
     * Guarda un nuevo usuario.
     *
     * @param usuario Entidad del usuario
     * @return Usuario guardado
     */
    UsuarioEntity save(UsuarioEntity usuario);
    
    /**
     * Actualiza un usuario existente.
     *
     * @param usuario Entidad del usuario con datos actualizados
     */
    void update(UsuarioEntity usuario);
    
    /**
     * Actualiza el estado de un usuario.
     *
     * @param id ID del usuario
     * @param estado Nuevo estado
     */
    void updateEstado(String id, EstadoUsuario estado);
    
    /**
     * Actualiza la dirección IP de un usuario.
     *
     * @param id ID del usuario
     * @param ipAddress Nueva dirección IP
     */
    void updateIpAddress(String id, String ipAddress);
    
    /**
     * Elimina un usuario por su ID.
     *
     * @param id ID del usuario
     */
    void deleteById(String id);
    
    /**
     * Obtiene los usuarios más activos del día basado en cantidad de mensajes enviados.
     *
     * @param limit Número máximo de usuarios a retornar
     * @return Lista de mapas con información del usuario (nombre, mensajes)
     */
    List<Map<String, Object>> getTopActivos(int limit);
}

