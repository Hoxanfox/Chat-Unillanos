package servicio.archivos;

import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * Contrato para el servicio que expone la lógica de gestión de archivos
 * a la capa de Presentación (Controladores).
 */
public interface IServicioArchivos {

    /**
     * Inicia el proceso de subida de un archivo.
     * @param archivo El archivo seleccionado por el usuario en la vista.
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
     * Obtiene un archivo por su fileId. Si ya existe localmente, lo devuelve inmediatamente.
     * Si no existe, lo descarga desde el servidor.
     *
     * @param fileId El identificador del archivo en el servidor (ej: "user_photos/deivid1.jpg")
     * @param directorioDestino El directorio donde se guardará/buscará el archivo
     * @return CompletableFuture que se completa con el archivo local listo para usar
     */
    CompletableFuture<File> obtenerArchivoPorFileId(String fileId, File directorioDestino);

    /**
     * Obtiene un archivo por su fileId usando directorio automático.
     *
     * @param fileId El identificador del archivo en el servidor
     * @return CompletableFuture que se completa con el archivo local
     */
    CompletableFuture<File> obtenerArchivoPorFileId(String fileId);
}
