package controlador.usuario;

import dto.vistaLobby.DTOUsuario;
import java.io.File;
import java.util.concurrent.CompletableFuture;

public interface IControladorUsuario {
    CompletableFuture<DTOUsuario> obtenerInformacionUsuario(String userId);
    CompletableFuture<Void> actualizarInformacionUsuario(DTOUsuario dtoUsuario);
    CompletableFuture<Void> guardarUsuario(DTOUsuario dtoUsuario);
    CompletableFuture<DTOUsuario> cargarInformacionUsuarioLogueado();
    boolean haySesionActiva();
    String obtenerUserIdSesion();
    CompletableFuture<Boolean> cerrarSesion();

    /**
     * Obtiene la foto de perfil del usuario desde el fileId.
     * Descarga el archivo si no existe localmente.
     *
     * @param fileId El identificador del archivo (ej: "user_photos/deivid1.jpg")
     * @return CompletableFuture con el archivo de la foto de perfil
     */
    CompletableFuture<File> obtenerFotoPerfil(String fileId);
}
