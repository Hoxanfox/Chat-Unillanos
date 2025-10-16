package servicio.usuario;

import dto.vistaLobby.DTOUsuario;
import fachada.FachadaGeneralImpl;
import fachada.IFachadaGeneral;
import fachada.gestionUsuarios.insercionDB.IFachadaUsuarios;
import fachada.gestionUsuarios.session.IFachadaLobby;

import java.util.concurrent.CompletableFuture;

public class ServicioUsuarioImpl implements IServicioUsuario {

    private final IFachadaUsuarios fachadaUsuarios;
    private final IFachadaLobby fachadaLobby;

    public ServicioUsuarioImpl() {
        IFachadaGeneral fachadaGeneral = FachadaGeneralImpl.getInstancia();
        this.fachadaUsuarios = fachadaGeneral.getFachadaUsuarios();
        this.fachadaLobby = fachadaGeneral.getFachadaLobby(); // Asume que existe este método
        System.out.println("✅ [ServicioUsuario]: Inicializado con FachadaUsuarios y FachadaLobby.");
    }

    @Override
    public CompletableFuture<DTOUsuario> obtenerInformacionUsuario(String userId) {
        System.out.println("➡️ [ServicioUsuario]: Obteniendo información del usuario: " + userId);
        return fachadaUsuarios.obtenerUsuarioPorId(userId);
    }

    @Override
    public CompletableFuture<Void> actualizarInformacionUsuario(DTOUsuario dtoUsuario) {
        System.out.println("➡️ [ServicioUsuario]: Actualizando información del usuario: " + dtoUsuario.getId());
        return fachadaUsuarios.actualizarUsuario(dtoUsuario);
    }

    @Override
    public CompletableFuture<Void> guardarUsuario(DTOUsuario dtoUsuario) {
        System.out.println("➡️ [ServicioUsuario]: Guardando nuevo usuario: " + dtoUsuario.getId());
        return fachadaUsuarios.guardarUsuario(dtoUsuario);
    }

    @Override
    public CompletableFuture<DTOUsuario> cargarInformacionUsuarioLogueado() {
        System.out.println("➡️ [ServicioUsuario]: Cargando información del usuario logueado.");
        return CompletableFuture.supplyAsync(() -> {
            try {
                return fachadaLobby.cargarInformacionUsuarioLogueado();
            } catch (Exception e) {
                System.err.println("❌ [ServicioUsuario]: Error al cargar usuario logueado: " + e.getMessage());
                throw new RuntimeException("No se pudo cargar la información del usuario logueado", e);
            }
        });
    }

    @Override
    public boolean haySesionActiva() {
        System.out.println("➡️ [ServicioUsuario]: Verificando sesión activa.");
        return fachadaLobby.haySesionActiva();
    }

    @Override
    public String obtenerUserIdSesion() {
        System.out.println("➡️ [ServicioUsuario]: Obteniendo userId de la sesión.");
        return fachadaLobby.obtenerUserIdSesion();
    }

    @Override
    public void cerrarSesion() {
        System.out.println("➡️ [ServicioUsuario]: Cerrando sesión del usuario.");
        fachadaLobby.cerrarSesion();
    }
}
