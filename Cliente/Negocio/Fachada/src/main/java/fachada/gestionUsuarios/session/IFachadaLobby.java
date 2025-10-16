package fachada.gestionUsuarios.session;

import dto.vistaLobby.DTOUsuario;

public interface IFachadaLobby {
    String obtenerUserIdSesion();
    boolean haySesionActiva();
    DTOUsuario cargarInformacionUsuarioLogueado();  // Debe retornar DTOUsuario, no void
    void cerrarSesion();
}
