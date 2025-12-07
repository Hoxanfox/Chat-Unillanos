package gestionUsuario.especialista;

import dominio.Usuario;

import java.util.List;
import java.util.UUID;

/**
 * Contrato para el especialista de usuarios.
 * Define la lógica de negocio para gestionar usuarios.
 */
public interface IEspecialistaUsuarios {

    /**
     * Guarda un nuevo usuario en la base de datos.
     * @param usuario Entidad de dominio Usuario
     */
    void guardarUsuario(Usuario usuario);

    /**
     * Obtiene un usuario por su ID.
     * @param idUsuario UUID del usuario
     * @return Usuario encontrado o null
     */
    Usuario obtenerUsuarioPorId(UUID idUsuario);

    /**
     * Obtiene un usuario por su email.
     * @param email Email del usuario
     * @return Usuario encontrado o null
     */
    Usuario obtenerUsuarioPorEmail(String email);

    /**
     * Actualiza un usuario existente.
     * @param usuario Usuario con datos actualizados
     */
    void actualizarUsuario(Usuario usuario);

    /**
     * Elimina un usuario por su ID.
     * @param idUsuario UUID del usuario
     */
    void eliminarUsuario(UUID idUsuario);

    /**
     * Obtiene todos los usuarios.
     * @return Lista de usuarios
     */
    List<Usuario> obtenerTodosUsuarios();

    /**
     * Verifica si existe un usuario con el email dado.
     * @param email Email a verificar
     * @return true si existe, false si no
     */
    boolean existeUsuarioPorEmail(String email);

    /**
     * Actualiza el estado de un usuario (activo, inactivo, baneado).
     * Útil para marcar cuando un usuario se loguea o desloguea.
     * @param idUsuario UUID del usuario
     * @param nuevoEstado Nuevo estado ('activo', 'inactivo', 'baneado')
     */
    void actualizarEstadoUsuario(UUID idUsuario, String nuevoEstado);
}
