package repositorio.usuario;

import dominio.Usuario;

import java.util.List;
import java.util.UUID;

/**
 * Contrato para el Repositorio de Usuarios.
 * Define operaciones CRUD básicas.
 */
public interface IRepositorioUsuario {

    /**
     * Guarda un nuevo usuario en la base de datos.
     * @param usuario Entidad de dominio Usuario
     */
    void guardar(Usuario usuario);

    /**
     * Obtiene un usuario por su ID.
     * @param idUsuario UUID del usuario
     * @return Usuario encontrado o null
     */
    Usuario obtenerPorId(UUID idUsuario);

    /**
     * Obtiene un usuario por su email.
     * @param email Email del usuario
     * @return Usuario encontrado o null
     */
    Usuario obtenerPorEmail(String email);

    /**
     * Actualiza un usuario existente.
     * @param usuario Usuario con datos actualizados
     */
    void actualizar(Usuario usuario);

    /**
     * Elimina un usuario por su ID.
     * @param idUsuario UUID del usuario
     */
    void eliminar(UUID idUsuario);

    /**
     * Obtiene todos los usuarios.
     * @return Lista de usuarios
     */
    List<Usuario> obtenerTodos();

    /**
     * Verifica si existe un usuario con el email dado.
     * @param email Email a verificar
     * @return true si existe, false si no
     */
    boolean existePorEmail(String email);

    /**
     * Actualiza el estado de un usuario.
     * @param idUsuario UUID del usuario
     * @param nuevoEstado Nuevo estado ('activo', 'inactivo', 'baneado')
     */
    void actualizarEstado(UUID idUsuario, String nuevoEstado);
}
