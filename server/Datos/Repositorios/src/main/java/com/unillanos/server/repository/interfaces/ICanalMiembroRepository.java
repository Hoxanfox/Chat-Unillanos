package com.unillanos.server.repository.interfaces;

import com.unillanos.server.repository.models.CanalEntity;
import com.unillanos.server.repository.models.CanalMiembroEntity;
import com.unillanos.server.repository.models.RolCanal;

import java.util.List;
import java.util.Optional;

/**
 * Interfaz del repositorio de miembros de canales.
 * Define operaciones para gestionar la relación N:M entre usuarios y canales.
 */
public interface ICanalMiembroRepository {
    
    // --- MÉTODOS DE CONSULTA ---
    
    /**
     * Busca la relación entre un usuario y un canal.
     *
     * @param usuarioId ID del usuario
     * @param canalId ID del canal
     * @return Optional con la relación si existe
     */
    Optional<CanalMiembroEntity> findByUsuarioAndCanal(String usuarioId, String canalId);
    
    /**
     * Obtiene todos los miembros de un canal.
     *
     * @param canalId ID del canal
     * @return Lista de miembros del canal
     */
    List<CanalMiembroEntity> findMiembrosByCanal(String canalId);
    
    /**
     * Obtiene los canales de un usuario con paginación.
     *
     * @param usuarioId ID del usuario
     * @param limit Número máximo de resultados
     * @param offset Desplazamiento inicial
     * @return Lista de canales del usuario
     */
    List<CanalEntity> findCanalesByUsuario(String usuarioId, int limit, int offset);
    
    /**
     * Verifica si un usuario es administrador de un canal.
     *
     * @param usuarioId ID del usuario
     * @param canalId ID del canal
     * @return true si el usuario es administrador
     */
    boolean esAdministrador(String usuarioId, String canalId);
    
    /**
     * Verifica si un usuario es miembro de un canal.
     *
     * @param usuarioId ID del usuario
     * @param canalId ID del canal
     * @return true si el usuario es miembro
     */
    boolean esMiembro(String usuarioId, String canalId);
    
    /**
     * Cuenta la cantidad de miembros de un canal.
     *
     * @param canalId ID del canal
     * @return Cantidad de miembros
     */
    int countMiembros(String canalId);
    
    // --- MÉTODOS DE ESCRITURA ---
    
    /**
     * Agrega un miembro a un canal con un rol específico.
     *
     * @param canalId ID del canal
     * @param usuarioId ID del usuario
     * @param rol Rol del usuario en el canal
     */
    void agregarMiembro(String canalId, String usuarioId, RolCanal rol);
    
    /**
     * Remueve un miembro de un canal.
     *
     * @param canalId ID del canal
     * @param usuarioId ID del usuario
     */
    void removerMiembro(String canalId, String usuarioId);
    
    /**
     * Actualiza el rol de un miembro en un canal.
     *
     * @param canalId ID del canal
     * @param usuarioId ID del usuario
     * @param nuevoRol Nuevo rol del usuario
     */
    void actualizarRol(String canalId, String usuarioId, RolCanal nuevoRol);
}

