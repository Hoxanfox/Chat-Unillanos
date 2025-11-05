package servicio.usuario;

import dto.vistaLobby.DTOUsuario;
import java.io.File;
import java.util.concurrent.CompletableFuture;

public interface IServicioUsuario {
    CompletableFuture<DTOUsuario> obtenerInformacionUsuario(String userId);
    CompletableFuture<Void> actualizarInformacionUsuario(DTOUsuario dtoUsuario);
    CompletableFuture<Void> guardarUsuario(DTOUsuario dtoUsuario);

    // Nuevos métodos para sesión
    CompletableFuture<DTOUsuario> cargarInformacionUsuarioLogueado();
    boolean haySesionActiva();
    String obtenerUserIdSesion();
    CompletableFuture<Boolean> cerrarSesion();

    /**
     * Obtiene la foto de perfil del usuario.
     *
     * @param fileId El identificador del archivo
     * @return CompletableFuture con el archivo de la foto
     */
    CompletableFuture<File> obtenerFotoPerfil(String fileId);
}
