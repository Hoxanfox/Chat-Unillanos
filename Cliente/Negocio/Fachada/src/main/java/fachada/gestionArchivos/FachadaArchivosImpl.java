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

    @Override
    public CompletableFuture<String> subirArchivoParaRegistro(File archivo) {
        // Simplemente delega la llamada.
        return gestionArchivos.subirArchivoParaRegistro(archivo);
    }

    @Override
    public CompletableFuture<File> descargarArchivo(String fileId, File directorioDestino) {
        // Delega la descarga al componente de gestión
        return gestionArchivos.descargarArchivo(fileId, directorioDestino);
    }

    @Override
    public CompletableFuture<byte[]> descargarArchivoEnMemoria(String fileId) {
        System.out.println("➡️ [FachadaArchivos]: Delegando descarga en memoria al gestor - FileId: " + fileId);
        // Delega la descarga en memoria al componente de gestión
        return gestionArchivos.descargarArchivoEnMemoria(fileId);
    }
}
