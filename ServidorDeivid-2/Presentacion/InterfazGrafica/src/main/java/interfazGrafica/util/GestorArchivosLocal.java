package interfazGrafica.util;

import logger.LoggerCentral;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.UUID;

/**
 * Gestor de archivos local para la interfaz gráfica del servidor
 * Maneja SOLO la copia física de archivos al directorio Bucket/user_photos
 * NO accede a repositorio - respeta la arquitectura en capas
 */
public class GestorArchivosLocal {

    private static final String TAG = "GestorArchivosLocal";
    private static final String BUCKET_PATH = "./Bucket/";
    private static final String USER_PHOTOS_DIR = "user_photos/";

    /**
     * Guarda una foto de usuario en el Bucket local (solo archivo físico)
     * @param archivoFoto Archivo de imagen a guardar
     * @return Objeto con la info del archivo guardado (fileId, hash, tamaño) o null si hubo error
     */
    public static ArchivoInfo guardarFotoUsuario(File archivoFoto) {
        try {
            LoggerCentral.info(TAG, "📸 Iniciando guardado físico de foto de usuario...");

            // Validar que el archivo existe
            if (archivoFoto == null || !archivoFoto.exists()) {
                LoggerCentral.error(TAG, "❌ Archivo no válido o no existe");
                return null;
            }

            LoggerCentral.info(TAG, "✓ Archivo válido: " + archivoFoto.getName() + " (" + archivoFoto.length() + " bytes)");

            // Crear directorio si no existe
            Path directorioDestino = Paths.get(BUCKET_PATH + USER_PHOTOS_DIR);
            if (!Files.exists(directorioDestino)) {
                Files.createDirectories(directorioDestino);
                LoggerCentral.info(TAG, "✓ Directorio creado: " + directorioDestino);
            }

            // Generar nombre único para el archivo
            String extension = obtenerExtension(archivoFoto.getName());
            String nombreUnico = UUID.randomUUID().toString() + extension;
            
            // Ruta completa de destino
            Path archivoDestino = directorioDestino.resolve(nombreUnico);

            // Copiar archivo al Bucket
            Files.copy(archivoFoto.toPath(), archivoDestino, StandardCopyOption.REPLACE_EXISTING);
            LoggerCentral.info(TAG, "✓ Archivo copiado a: " + archivoDestino.toAbsolutePath());

            // Retornar ID relativo (desde Bucket/)
            String fileId = USER_PHOTOS_DIR + nombreUnico;

            // Leer el archivo para calcular hash
            byte[] fileData = Files.readAllBytes(archivoDestino);
            String hash = calcularHashSHA256(fileData);

            LoggerCentral.info(TAG, "✓ Hash calculado: " + hash);

            // Determinar mime type
            String mimeType = "image/" + extension.replace(".", "");

            LoggerCentral.info(TAG, "✅ Archivo físico guardado: " + fileId + " (" + fileData.length + " bytes)");

            // Retornar info del archivo para que el controlador lo registre en BD
            return new ArchivoInfo(fileId, archivoFoto.getName(), mimeType, fileData.length, hash);

        } catch (Exception e) {
            LoggerCentral.error(TAG, "❌ Error al guardar foto: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Elimina una foto de usuario del Bucket (solo archivo físico)
     * @param fileId ID del archivo (ruta relativa desde Bucket/)
     * @return true si se eliminó correctamente
     */
    public static boolean eliminarFotoUsuario(String fileId) {
        try {
            if (fileId == null || fileId.isEmpty()) {
                return false;
            }

            LoggerCentral.info(TAG, "🗑️ Eliminando foto física: " + fileId);

            Path archivoAEliminar = Paths.get(BUCKET_PATH + fileId);
            
            boolean eliminadoFisico = false;
            if (Files.exists(archivoAEliminar)) {
                Files.delete(archivoAEliminar);
                eliminadoFisico = true;
                LoggerCentral.info(TAG, "✓ Archivo físico eliminado: " + fileId);
            } else {
                LoggerCentral.warn(TAG, "⚠️ Archivo físico no encontrado: " + fileId);
            }

            return eliminadoFisico;

        } catch (IOException e) {
            LoggerCentral.error(TAG, "❌ Error al eliminar foto: " + e.getMessage());
            return false;
        }
    }

    /**
     * Calcula el hash SHA-256 de un archivo
     */
    private static String calcularHashSHA256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hexString = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error calculando hash: " + e.getMessage());
            return "";
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

    /**
     * Clase interna para retornar información del archivo guardado
     */
    public static class ArchivoInfo {
        public final String fileId;
        public final String nombreOriginal;
        public final String mimeType;
        public final long tamanio;
        public final String hash;

        public ArchivoInfo(String fileId, String nombreOriginal, String mimeType, long tamanio, String hash) {
            this.fileId = fileId;
            this.nombreOriginal = nombreOriginal;
            this.mimeType = mimeType;
            this.tamanio = tamanio;
            this.hash = hash;
        }
    }
}

