package servicio.usuario;

import dto.vistaLobby.DTOUsuario;
import fachada.FachadaGeneralImpl;
import fachada.IFachadaGeneral;
import fachada.gestionUsuarios.insercionDB.IFachadaUsuarios;

import java.util.concurrent.CompletableFuture;

/**
 * Implementación del servicio de usuario.
 * Coordina las operaciones relacionadas con usuarios a través de la fachada.
 */
public class ServicioUsuarioImpl implements IServicioUsuario {

    private final IFachadaUsuarios fachadaUsuarios;

    public ServicioUsuarioImpl() {
        IFachadaGeneral fachadaGeneral = FachadaGeneralImpl.getInstancia();
        this.fachadaUsuarios = fachadaGeneral.getFachadaUsuarios();
        System.out.println("✅ [ServicioUsuario]: Inicializado con FachadaUsuarios.");
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
}

