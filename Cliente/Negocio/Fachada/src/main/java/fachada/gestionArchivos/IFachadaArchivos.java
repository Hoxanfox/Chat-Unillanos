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

    /**
     * ✅ NUEVO: Reproduce un archivo de audio. Si existe en caché local, lo reproduce desde ahí.
     * Si no existe, lo descarga y reproduce desde memoria.
     *
     * @param fileId El ID del archivo de audio en el servidor
     * @return CompletableFuture que se completa cuando inicia la reproducción
     */
    CompletableFuture<Void> reproducirAudio(String fileId);

    /**
     * Descarga un archivo de audio a la carpeta local de audios.
     *
     * @param fileId El ID del archivo de audio en el servidor
     * @return CompletableFuture que se completa con el archivo de audio descargado
     */
    CompletableFuture<File> descargarAudioALocal(String fileId);

    /**
     * ✅ NUEVO: Guarda un audio que viene en Base64 (desde PUSH del servidor) como archivo físico
     * y en la base de datos local para uso offline.
     *
     * @param base64Audio El contenido del audio en Base64
     * @param mensajeId El ID del mensaje (usado para generar nombre único)
     * @return CompletableFuture que se completa con el archivo guardado
     */
    CompletableFuture<File> guardarAudioDesdeBase64(String base64Audio, String mensajeId);

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

    /**
     * Verifica si un archivo existe localmente sin descargarlo.
     *
     * @param fileId El identificador del archivo
     * @return CompletableFuture que se completa con true si existe localmente
     */
    CompletableFuture<Boolean> existeArchivoLocalmente(String fileId);

    /**
     * Verifica si un archivo ya existe localmente (en disco y en BD).
     *
     * @param fileId El identificador del archivo en el servidor
     * @return CompletableFuture que se completa con true si existe, false si no
     */
    CompletableFuture<Boolean> existeLocalmente(String fileId);

    /**
     * Obtiene la ruta local de un archivo si ya existe en disco.
     *
     * @param fileId El identificador del archivo en el servidor
     * @return CompletableFuture que se completa con el archivo local, o null si no existe
     */
    CompletableFuture<File> obtenerRutaLocal(String fileId);
}
