package servicio.autenticacion;

import dto.vistaLogin.DTOAutenticacion;
import dto.vistaRegistro.DTORegistro;
import fachada.FachadaGeneralImpl;
import fachada.gestionUsuarios.autenticacion.IFachadaAutenticacionUsuario;

import java.util.concurrent.CompletableFuture;

/**
 * Implementación del servicio que AHORA obtiene la fachada específica
 * que necesita desde la Fachada General.
 */
public class ServicioAutenticacion implements IServicioAutenticacion {

    private final IFachadaAutenticacionUsuario fachadaAutenticacion;

    public ServicioAutenticacion() {
        // Pide a la central la fachada que necesita.
        this.fachadaAutenticacion = FachadaGeneralImpl.getInstancia().getFachadaAutenticacion();
    }

    @Override
    public CompletableFuture<Boolean> autenticar(DTOAutenticacion datos) {
        return fachadaAutenticacion.autenticarUsuario(datos);
    }

    @Override
    public CompletableFuture<Boolean> registrar(DTORegistro datos, byte[] fotoBytes) {
        return fachadaAutenticacion.registrarUsuario(datos, fotoBytes);
    }
}

