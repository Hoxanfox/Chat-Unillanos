package servicio.usuario;

import dto.vistaLobby.DTOUsuario;

import java.util.concurrent.CompletableFuture;

/**
 * Contrato para el servicio de usuario.
 * Define las operaciones de negocio relacionadas con usuarios.
 */
public interface IServicioUsuario {

    /**
     * Obtiene la información de un usuario por su ID.
     * @param userId ID del usuario
     * @return CompletableFuture con el DTOUsuario
     */
    CompletableFuture<DTOUsuario> obtenerInformacionUsuario(String userId);

    /**
     * Actualiza la información de un usuario.
     * @param dtoUsuario DTO con la información actualizada
     * @return CompletableFuture que se completa cuando se actualiza
     */
    CompletableFuture<Void> actualizarInformacionUsuario(DTOUsuario dtoUsuario);

    /**
     * Guarda un nuevo usuario.
     * @param dtoUsuario DTO con la información del usuario
     * @return CompletableFuture que se completa cuando se guarda
     */
    CompletableFuture<Void> guardarUsuario(DTOUsuario dtoUsuario);
}

