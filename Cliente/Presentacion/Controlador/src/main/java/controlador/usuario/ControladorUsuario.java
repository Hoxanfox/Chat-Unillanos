package controlador.usuario;

import dto.vistaLobby.DTOUsuario;
import servicio.usuario.IServicioUsuario;
import servicio.usuario.ServicioUsuarioImpl;
import java.util.concurrent.CompletableFuture;

public class ControladorUsuario implements IControladorUsuario {

    private final IServicioUsuario servicioUsuario;

    public ControladorUsuario() {
        this.servicioUsuario = new ServicioUsuarioImpl();
        System.out.println("✅ [ControladorUsuario]: Inicializado.");
    }

    @Override
    public CompletableFuture<DTOUsuario> obtenerInformacionUsuario(String userId) {
        System.out.println("🎮 [ControladorUsuario]: Solicitando información del usuario: " + userId);
        return servicioUsuario.obtenerInformacionUsuario(userId);
    }

    @Override
    public CompletableFuture<Void> actualizarInformacionUsuario(DTOUsuario dtoUsuario) {
        System.out.println("🎮 [ControladorUsuario]: Actualizando información del usuario: " + dtoUsuario.getId());
        return servicioUsuario.actualizarInformacionUsuario(dtoUsuario);
    }

    @Override
    public CompletableFuture<Void> guardarUsuario(DTOUsuario dtoUsuario) {
        System.out.println("🎮 [ControladorUsuario]: Guardando nuevo usuario: " + dtoUsuario.getId());
        return servicioUsuario.guardarUsuario(dtoUsuario);
    }

    @Override
    public CompletableFuture<DTOUsuario> cargarInformacionUsuarioLogueado() {
        System.out.println("🎮 [ControladorUsuario]: Cargando información del usuario logueado.");
        return servicioUsuario.cargarInformacionUsuarioLogueado();
    }

    @Override
    public boolean haySesionActiva() {
        System.out.println("🎮 [ControladorUsuario]: Verificando sesión activa.");
        return servicioUsuario.haySesionActiva();
    }

    @Override
    public String obtenerUserIdSesion() {
        System.out.println("🎮 [ControladorUsuario]: Obteniendo userId de la sesión.");
        return servicioUsuario.obtenerUserIdSesion();
    }

    @Override
    public void cerrarSesion() {
        System.out.println("🎮 [ControladorUsuario]: Cerrando sesión del usuario.");
        servicioUsuario.cerrarSesion();
    }
}
