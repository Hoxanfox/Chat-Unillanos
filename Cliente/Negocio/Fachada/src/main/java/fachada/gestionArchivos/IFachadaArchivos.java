package fachada.gestionArchivos;

import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * Contrato para la Fachada que gestiona las operaciones de archivos.
 * Es el único punto de entrada a esta lógica de negocio desde el Servicio.
 */
public interface IFachadaArchivos {

    /**
     * Delega la tarea de subir un archivo al componente de gestión correspondiente.
     * @param archivo El archivo local que se desea subir.
     * @return Una promesa que se resolverá con el identificador del archivo en el servidor.
     */
    CompletableFuture<String> subirArchivo(File archivo);

    /**
     * Sube un archivo al servidor durante el proceso de registro (sin autenticación).
     *
     * @param archivo El archivo a subir
     * @return CompletableFuture que se completa con el ID del archivo subido
     */
    CompletableFuture<String> subirArchivoParaRegistro(File archivo);

    /**
     * Descarga un archivo desde el servidor.
     *
     * @param fileId El ID del archivo en el servidor
     * @param directorioDestino El directorio donde se guardará el archivo descargado
     * @return CompletableFuture que se completa con el archivo descargado
     */
    CompletableFuture<File> descargarArchivo(String fileId, File directorioDestino);

    /**
     * Descarga un archivo en memoria (como array de bytes) sin guardarlo en disco.
     * Útil para reproducir audio directamente.
     *
     * @param fileId El ID del archivo en el servidor
     * @return CompletableFuture que se completa con los bytes del archivo
     */
    CompletableFuture<byte[]> descargarArchivoEnMemoria(String fileId);
}
