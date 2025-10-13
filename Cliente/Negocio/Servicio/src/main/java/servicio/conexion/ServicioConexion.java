package servicio.conexion;

import fachada.FachadaGeneralImpl;

import fachada.gestionConexion.IFachadaConexion;

import java.util.concurrent.CompletableFuture;

/**
 * Implementación del servicio de conexión que AHORA obtiene su fachada
 * desde la Fachada General.
 */
public class ServicioConexion implements IServicioConexion {

    private final IFachadaConexion fachadaConexion;

    public ServicioConexion() {
        // Pide a la central la fachada que necesita.
        this.fachadaConexion = FachadaGeneralImpl.getInstancia().getFachadaConexion();
    }

    @Override
    public CompletableFuture<Boolean> conectar() {
        return fachadaConexion.conectar();
    }
}

