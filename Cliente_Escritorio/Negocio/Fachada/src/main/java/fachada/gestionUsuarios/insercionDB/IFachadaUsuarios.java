package fachada.gestionUsuarios.insercionDB;

import dto.vistaLobby.DTOUsuario;

import java.util.concurrent.CompletableFuture;

/**
 * Contrato para la fachada que gestiona operaciones de usuarios.
 */
public interface IFachadaUsuarios {

    /**
     * Obtiene un usuario por su ID desde el repositorio local.
     * @param userId ID del usuario
     * @return CompletableFuture con el DTOUsuario
     */
    CompletableFuture<DTOUsuario> obtenerUsuarioPorId(String userId);

    /**
     * Obtiene un usuario por su email desde el repositorio local.
     * @param email Email del usuario
     * @return CompletableFuture con el DTOUsuario
     */
    CompletableFuture<DTOUsuario> obtenerUsuarioPorEmail(String email);

    /**
     * Guarda un nuevo usuario en el repositorio local.
     * @param dtoUsuario DTO con la información del usuario
     * @return CompletableFuture que se completa cuando se guarda
     */
    CompletableFuture<Void> guardarUsuario(DTOUsuario dtoUsuario);

    /**
     * Actualiza la información de un usuario en el repositorio local.
     * @param dtoUsuario DTO con la información actualizada
     * @return CompletableFuture que se completa cuando se actualiza
     */
    CompletableFuture<Void> actualizarUsuario(DTOUsuario dtoUsuario);

    /**
     * Elimina un usuario del repositorio local.
     * @param userId ID del usuario a eliminar
     * @return CompletableFuture que se completa cuando se elimina
     */
    CompletableFuture<Void> eliminarUsuario(String userId);
}

