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

    @Override
    public CompletableFuture<String> subirArchivoParaRegistro(File archivo) {
        return fachadaArchivos.subirArchivoParaRegistro(archivo);
    }

    @Override
    public CompletableFuture<File> obtenerArchivoPorFileId(String fileId, File directorioDestino) {
        System.out.println("➡️ [ServicioArchivos]: Solicitando archivo - FileId: " + fileId);
        return fachadaArchivos.obtenerArchivoPorFileId(fileId, directorioDestino);
    }

    @Override
    public CompletableFuture<File> obtenerArchivoPorFileId(String fileId) {
        System.out.println("➡️ [ServicioArchivos]: Solicitando archivo con directorio auto - FileId: " + fileId);
        return fachadaArchivos.obtenerArchivoPorFileId(fileId);
    }

    @Override
    public CompletableFuture<Boolean> existeLocalmente(String fileId) {
        System.out.println("➡️ [ServicioArchivos]: Verificando existencia local - FileId: " + fileId);
        return fachadaArchivos.existeLocalmente(fileId);
    }

    @Override
    public CompletableFuture<File> obtenerRutaLocal(String fileId) {
        System.out.println("➡️ [ServicioArchivos]: Obteniendo ruta local - FileId: " + fileId);
        return fachadaArchivos.obtenerRutaLocal(fileId);
    }
}
