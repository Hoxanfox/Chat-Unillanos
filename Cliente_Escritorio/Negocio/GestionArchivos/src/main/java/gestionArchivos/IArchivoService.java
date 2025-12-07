package gestionArchivos;

import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * Servicio especializado para obtener archivos por su identificador (fileId).
 * Gestiona la lógica de verificación local y descarga desde el servidor.
 * 
 * Este servicio coordina entre el repositorio (para metadatos/caché) 
 * y el gestor de archivos (para descargas reales desde el servidor).
 */
public interface IArchivoService {

    /**
     * Obtiene un archivo usando su identificador del servidor.
     * Si el archivo ya existe localmente, lo devuelve inmediatamente.
     * Si no existe, lo descarga desde el servidor.
     * 
     * @param fileId El identificador único del archivo en el servidor (ej: "user_photos/deivid1.jpg")
     * @param directorioDestino El directorio donde se guardará/buscará el archivo
     * @return CompletableFuture que se completa con el archivo local listo para usar
     * @throws IllegalArgumentException si fileId es nulo o vacío
     * @throws java.io.IOException si hay problemas al crear directorios o escribir archivos
     * @throws RuntimeException si falla la descarga desde el servidor
     */
    CompletableFuture<File> obtenerPorFileId(String fileId, File directorioDestino);

    /**
     * Obtiene un archivo usando su identificador del servidor.
     * Utiliza un directorio por defecto basado en el tipo de archivo.
     * 
     * @param fileId El identificador único del archivo en el servidor
     * @return CompletableFuture que se completa con el archivo local
     * @throws IllegalArgumentException si fileId es nulo o vacío
     */
    CompletableFuture<File> obtenerPorFileId(String fileId);

    /**
     * Verifica si un archivo ya está disponible localmente sin descargarlo.
     * 
     * @param fileId El identificador del archivo
     * @return CompletableFuture que se completa con true si existe localmente
     */
    CompletableFuture<Boolean> existeLocalmente(String fileId);

    /**
     * Obtiene la ruta local de un archivo si ya fue descargado.
     * 
     * @param fileId El identificador del archivo
     * @return CompletableFuture que se completa con el File local o null si no existe
     */
    CompletableFuture<File> obtenerRutaLocal(String fileId);
}

