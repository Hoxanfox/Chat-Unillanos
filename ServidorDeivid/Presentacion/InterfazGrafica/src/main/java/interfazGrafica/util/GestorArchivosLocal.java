package interfazGrafica.util;

import logger.LoggerCentral;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Gestor de archivos local para la interfaz gráfica del servidor
 * Maneja la copia de archivos al directorio Bucket/user_photos
 */
public class GestorArchivosLocal {

    private static final String TAG = "GestorArchivosLocal";
    private static final String BUCKET_PATH = "./Bucket/";
    private static final String USER_PHOTOS_DIR = "user_photos/";
    
    /**
     * Guarda una foto de usuario en el Bucket local
     * @param archivoFoto Archivo de imagen a guardar
     * @return ID del archivo guardado (ruta relativa desde Bucket/) o null si hubo error
     */
    public static String guardarFotoUsuario(File archivoFoto) {
        try {
            // Validar que el archivo existe
            if (archivoFoto == null || !archivoFoto.exists()) {
                LoggerCentral.error(TAG, "Archivo no válido o no existe");
                return null;
            }

            // Crear directorio si no existe
            Path directorioDestino = Paths.get(BUCKET_PATH + USER_PHOTOS_DIR);
            if (!Files.exists(directorioDestino)) {
                Files.createDirectories(directorioDestino);
                LoggerCentral.info(TAG, "Directorio creado: " + directorioDestino);
            }

            // Generar nombre único para el archivo
            String extension = obtenerExtension(archivoFoto.getName());
            String nombreUnico = UUID.randomUUID().toString() + extension;
            
            // Ruta completa de destino
            Path archivoDestino = directorioDestino.resolve(nombreUnico);

            // Copiar archivo al Bucket
            Files.copy(archivoFoto.toPath(), archivoDestino, StandardCopyOption.REPLACE_EXISTING);

            // Retornar ID relativo (desde Bucket/)
            String fileId = USER_PHOTOS_DIR + nombreUnico;
            LoggerCentral.info(TAG, "✓ Foto guardada: " + fileId + " (" + archivoFoto.length() + " bytes)");

            return fileId;

        } catch (IOException e) {
            LoggerCentral.error(TAG, "Error al guardar foto: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Elimina una foto de usuario del Bucket
     * @param fileId ID del archivo (ruta relativa desde Bucket/)
     * @return true si se eliminó correctamente
     */
    public static boolean eliminarFotoUsuario(String fileId) {
        try {
            if (fileId == null || fileId.isEmpty()) {
                return false;
            }

            Path archivoAEliminar = Paths.get(BUCKET_PATH + fileId);
            
            if (Files.exists(archivoAEliminar)) {
                Files.delete(archivoAEliminar);
                LoggerCentral.info(TAG, "✓ Foto eliminada: " + fileId);
                return true;
            } else {
                LoggerCentral.warn(TAG, "Archivo no encontrado para eliminar: " + fileId);
                return false;
            }

        } catch (IOException e) {
            LoggerCentral.error(TAG, "Error al eliminar foto: " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene la extensión de un archivo
     * @param nombreArchivo Nombre del archivo
     * @return Extensión con punto (ej: ".jpg") o string vacío si no tiene
     */
    private static String obtenerExtension(String nombreArchivo) {
        int ultimoPunto = nombreArchivo.lastIndexOf('.');
        if (ultimoPunto > 0 && ultimoPunto < nombreArchivo.length() - 1) {
            return nombreArchivo.substring(ultimoPunto);
        }
        return "";
    }

    /**
     * Valida que un archivo sea una imagen válida
     * @param archivo Archivo a validar
     * @return true si es una imagen válida
     */
    public static boolean esImagenValida(File archivo) {
        if (archivo == null || !archivo.exists() || !archivo.isFile()) {
            return false;
        }

        String nombre = archivo.getName().toLowerCase();
        return nombre.endsWith(".jpg") || 
               nombre.endsWith(".jpeg") || 
               nombre.endsWith(".png") || 
               nombre.endsWith(".gif");
    }

    /**
     * Obtiene la ruta completa de un archivo en el Bucket
     * @param fileId ID del archivo (ruta relativa desde Bucket/)
     * @return Path completo del archivo
     */
    public static Path obtenerRutaCompleta(String fileId) {
        return Paths.get(BUCKET_PATH + fileId);
    }
}

