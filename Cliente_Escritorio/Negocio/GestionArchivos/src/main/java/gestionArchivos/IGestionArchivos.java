package gestionArchivos;

import observador.ISujeto;
import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * Contrato para el componente de negocio que gestiona toda la lógica
 * relacionada con la manipulación y transferencia de archivos.
 */
public interface IGestionArchivos extends ISujeto {

    /**
     * Orquesta el proceso completo de subir un archivo al servidor,
     * manejando el protocolo de división por chunks.
     *
     * @param archivo El archivo local que se desea subir.
     * @return Una promesa (CompletableFuture) que se resolverá con un identificador
     * único del archivo en el servidor si la subida es exitosa, o
     * se completará excepcionalmente si ocurre un error.
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
     * Solicita al servidor la descarga de un archivo específico.
     * El archivo se descarga por chunks y se guarda en el directorio especificado.
     *
     * @param fileId El identificador único del archivo en el servidor
     * @param directorioDestino El directorio donde se guardará el archivo descargado
     * @return CompletableFuture que se completa con el File descargado
     */
    CompletableFuture<File> descargarArchivo(String fileId, File directorioDestino);

    /**
     * Descarga un archivo desde el servidor en memoria (como array de bytes).
     * Útil para reproducir audio sin guardar en disco.
     *
     * @param fileId El identificador único del archivo en el servidor
     * @return CompletableFuture que se completa con los bytes del archivo
     */
    CompletableFuture<byte[]> descargarArchivoEnMemoria(String fileId);
}