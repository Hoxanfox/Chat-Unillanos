package servicio.archivos;

import fachada.FachadaGeneralImpl;
import fachada.gestionArchivos.IFachadaArchivos;

import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * Implementación del servicio que AHORA obtiene la fachada específica
 * que necesita desde la Fachada General.
 */
public class ServicioArchivosImpl implements IServicioArchivos {

    private final IFachadaArchivos fachadaArchivos;

    public ServicioArchivosImpl() {
        // Pide a la central la fachada que necesita.
        this.fachadaArchivos = FachadaGeneralImpl.getInstancia().getFachadaArchivos();
    }

    @Override
    public CompletableFuture<String> subirArchivo(File archivo) {
        return fachadaArchivos.subirArchivo(archivo);
    }
}

