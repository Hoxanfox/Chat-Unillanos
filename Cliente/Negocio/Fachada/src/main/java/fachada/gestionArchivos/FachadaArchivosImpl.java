package fachada.gestionArchivos;

import gestionArchivos.ArchivoServiceImpl;
import gestionArchivos.GestionArchivosImpl;
import gestionArchivos.IArchivoService;
import gestionArchivos.IGestionArchivos;
import repositorio.archivo.IRepositorioArchivo;
import repositorio.archivo.RepositorioArchivoImpl;

import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * Implementación de la Fachada que gestiona las operaciones de archivos.
 * Delega la lógica al componente de gestión correspondiente.
 */
public class FachadaArchivosImpl implements IFachadaArchivos {

    // La fachada depende del componente de gestión.
    private final IGestionArchivos gestionArchivos;

    // Servicio especializado para obtener archivos con caché
    private final IArchivoService archivoService;

    public FachadaArchivosImpl() {
        // En una aplicación real, esto se inyectaría.
        this.gestionArchivos = new GestionArchivosImpl();

        // Inicializar el servicio de archivos con repositorio
        IRepositorioArchivo repositorioArchivo = new RepositorioArchivoImpl();
        this.archivoService = new ArchivoServiceImpl(
            repositorioArchivo,
            gestionArchivos,
            new File("data/archivos")
        );

        System.out.println("✅ [FachadaArchivos]: Inicializada con ArchivoService integrado");
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

    @Override
    public CompletableFuture<File> obtenerArchivoPorFileId(String fileId, File directorioDestino) {
        System.out.println("➡️ [FachadaArchivos]: Obteniendo archivo - FileId: " + fileId);
        System.out.println("   Directorio destino: " + directorioDestino.getAbsolutePath());

        // Delega al servicio especializado que maneja caché y descarga inteligente
        return archivoService.obtenerPorFileId(fileId, directorioDestino)
            .thenApply(archivo -> {
                System.out.println("✅ [FachadaArchivos]: Archivo obtenido exitosamente");
                return archivo;
            })
            .exceptionally(ex -> {
                System.err.println("❌ [FachadaArchivos]: Error al obtener archivo: " + ex.getMessage());
                throw new RuntimeException("Error al obtener archivo: " + fileId, ex);
            });
    }

    @Override
    public CompletableFuture<File> obtenerArchivoPorFileId(String fileId) {
        System.out.println("➡️ [FachadaArchivos]: Obteniendo archivo con directorio automático - FileId: " + fileId);

        // Delega al servicio especializado usando directorio automático
        return archivoService.obtenerPorFileId(fileId)
            .thenApply(archivo -> {
                System.out.println("✅ [FachadaArchivos]: Archivo obtenido exitosamente");
                return archivo;
            })
            .exceptionally(ex -> {
                System.err.println("❌ [FachadaArchivos]: Error al obtener archivo: " + ex.getMessage());
                throw new RuntimeException("Error al obtener archivo: " + fileId, ex);
            });
    }

    @Override
    public CompletableFuture<Boolean> existeArchivoLocalmente(String fileId) {
        System.out.println("➡️ [FachadaArchivos]: Verificando existencia local - FileId: " + fileId);
        return archivoService.existeLocalmente(fileId);
    }
}
