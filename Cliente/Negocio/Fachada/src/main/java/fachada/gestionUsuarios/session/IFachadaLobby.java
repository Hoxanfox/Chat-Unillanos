package fachada.gestionUsuarios.session;

import dto.vistaLobby.DTOUsuario;
import java.util.concurrent.CompletableFuture;

public interface IFachadaLobby {
    String obtenerUserIdSesion();
    boolean haySesionActiva();
    DTOUsuario cargarInformacionUsuarioLogueado();
    CompletableFuture<Boolean> cerrarSesion();
}
