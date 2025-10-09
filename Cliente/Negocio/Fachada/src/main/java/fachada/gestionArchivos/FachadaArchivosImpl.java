package fachada.gestionArchivos;

import gestionArchivos.GestionArchivosImpl;
import gestionArchivos.IGestionArchivos;

import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * Implementación de la Fachada que gestiona las operaciones de archivos.
 * Delega la lógica al componente de gestión correspondiente.
 */
public class FachadaArchivosImpl implements IFachadaArchivos {

    // La fachada depende del componente de gestión.
    private final IGestionArchivos gestionArchivos;

    public FachadaArchivosImpl() {
        // En una aplicación real, esto se inyectaría.
        this.gestionArchivos = new GestionArchivosImpl();
    }

    @Override
    public CompletableFuture<String> subirArchivo(File archivo) {
        // Simplemente delega la llamada.
        return gestionArchivos.subirArchivo(archivo);
    }
}
