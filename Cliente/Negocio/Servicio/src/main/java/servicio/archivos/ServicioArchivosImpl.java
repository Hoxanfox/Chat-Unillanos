package servicio.archivos;

import fachada.gestionArchivos.FachadaArchivosImpl;
import fachada.gestionArchivos.IFachadaArchivos;

import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * Implementación del servicio que expone la lógica de gestión de archivos
 * a la capa de Presentación.
 */
public class ServicioArchivosImpl implements IServicioArchivos {

    // El servicio depende de la fachada.
    private final IFachadaArchivos fachadaArchivos;

    public ServicioArchivosImpl() {
        // En una aplicación real, esto se inyectaría.
        this.fachadaArchivos = new FachadaArchivosImpl();
    }

    @Override
    public CompletableFuture<String> subirArchivo(File archivo) {
        // Simplemente delega la llamada.
        return fachadaArchivos.subirArchivo(archivo);
    }
}
