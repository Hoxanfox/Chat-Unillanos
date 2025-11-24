package gestorClientes.servicios;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import conexion.clientes.interfaces.IGestorConexionesCliente;
import conexion.clientes.interfaces.IRouterMensajesCliente;
import dominio.clienteServidor.Archivo;
import dto.archivos.*;
import dto.comunicacion.DTOResponse;
import gestorClientes.interfaces.IServicioCliente;
import gestorP2P.servicios.ServicioSincronizacionDatos;
import logger.LoggerCentral;
import repositorio.clienteServidor.ArchivoRepositorio;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio del servidor que gestiona subida y descarga de archivos por chunks.
 * Los archivos se almacenan físicamente en Bucket/ y los metadatos en BD.
 * Notifica al ServicioSincronizacionDatos para sincronización P2P automática.
 */
public class ServicioArchivos implements IServicioCliente {

    private static final String TAG = "FileService";
    private static final int CHUNK_SIZE = 524288; // 512 KB
    private static final String BUCKET_PATH = "./Bucket/";

    private IGestorConexionesCliente gestor;
    private final ArchivoRepositorio repoArchivo;
    private final Gson gson;

    // Cache de sesiones de subida/descarga activas
    private final Map<String, UploadSession> uploadSessions;
    private final Map<String, DownloadSession> downloadSessions;

    // Referencia al servicio de sincronización P2P (inyectada externamente)
    private ServicioSincronizacionDatos servicioSync;

    public ServicioArchivos() {
        this.repoArchivo = new ArchivoRepositorio();
        this.gson = new Gson();
        this.uploadSessions = new ConcurrentHashMap<>();
        this.downloadSessions = new ConcurrentHashMap<>();

        // Crear directorio Bucket si no existe
        crearDirectorioBucket();
    }

    /**
     * Permite inyectar el servicio de sincronización P2P.
     * Esto permite que los archivos se sincronicen automáticamente entre peers.
     */
    public void setServicioSync(ServicioSincronizacionDatos sync) {
        this.servicioSync = sync;
        LoggerCentral.info(TAG, "Servicio de sincronización P2P configurado");
    }

    @Override
    public String getNombre() {
        return "ServicioArchivos";
    }

    @Override
    public void inicializar(IGestorConexionesCliente gestor, IRouterMensajesCliente router) {
        this.gestor = gestor;

        // ==================== RUTAS DE SUBIDA ====================

        // RUTA: Iniciar subida (autenticado)
        router.registrarAccion("startFileUpload", (datos, idSesion) -> {
            try {
                String userId = gestor.obtenerUsuarioDeSesion(idSesion);
                if (userId == null) {
                    return new DTOResponse("startFileUpload", "error", "Usuario no autenticado", null);
                }

                DTOStartUpload dto = gson.fromJson(datos, DTOStartUpload.class);

                // Validar entrada
                if (dto.getFileName() == null || dto.getFileName().trim().isEmpty()) {
                    return new DTOResponse("startFileUpload", "error", "Nombre de archivo requerido", null);
                }

                // Generar ID único de subida
                String uploadId = UUID.randomUUID().toString();

                // Crear sesión de subida
                UploadSession session = new UploadSession(
                    uploadId, userId, dto.getFileName(), dto.getMimeType(), dto.getTotalChunks()
                );
                uploadSessions.put(uploadId, session);

                LoggerCentral.info(TAG, "Upload iniciado: " + uploadId + " - Usuario: " + userId +
                                 " - Archivo: " + dto.getFileName());

                Map<String, String> response = new HashMap<>();
                response.put("uploadId", uploadId);
                return new DTOResponse("startFileUpload", "success", "Upload iniciado", gson.toJsonTree(response));

            } catch (Exception e) {
                LoggerCentral.error(TAG, "Error en startFileUpload: " + e.getMessage());
                return new DTOResponse("startFileUpload", "error", "Error interno: " + e.getMessage(), null);
            }
        });

        // RUTA: Iniciar subida para registro (sin autenticación)
        router.registrarAccion("uploadFileForRegistration", (datos, idSesion) -> {
            try {
                DTOStartUpload dto = gson.fromJson(datos, DTOStartUpload.class);

                if (dto.getFileName() == null || dto.getFileName().trim().isEmpty()) {
                    return new DTOResponse("uploadFileForRegistration", "error", "Nombre de archivo requerido", null);
                }

                String uploadId = UUID.randomUUID().toString();

                // Sesión de subida sin usuario (para registro)
                UploadSession session = new UploadSession(
                    uploadId, "REGISTRO_TEMP", dto.getFileName(), dto.getMimeType(), dto.getTotalChunks()
                );
                uploadSessions.put(uploadId, session);

                LoggerCentral.info(TAG, "Upload para registro iniciado: " + uploadId + " - Archivo: " + dto.getFileName());

                Map<String, String> response = new HashMap<>();
                response.put("uploadId", uploadId);
                return new DTOResponse("uploadFileForRegistration", "success", "Upload iniciado", gson.toJsonTree(response));

            } catch (Exception e) {
                LoggerCentral.error(TAG, "Error en uploadFileForRegistration: " + e.getMessage());
                return new DTOResponse("uploadFileForRegistration", "error", "Error interno", null);
            }
        });

        // RUTA: Subir chunk
        router.registrarAccion("uploadFileChunk", (datos, idSesion) -> {
            try {
                DTOUploadChunk dto = gson.fromJson(datos, DTOUploadChunk.class);

                UploadSession session = uploadSessions.get(dto.getUploadId());
                if (session == null) {
                    return new DTOResponse("uploadFileChunk_" + dto.getUploadId() + "_" + dto.getChunkNumber(),
                                         "error", "Sesión no encontrada", null);
                }

                // Decodificar y guardar chunk
                byte[] chunkData = Base64.getDecoder().decode(dto.getChunkDataBase64());
                session.addChunk(dto.getChunkNumber(), chunkData);

                LoggerCentral.debug(TAG, "Chunk recibido: " + dto.getChunkNumber() + "/" + session.totalChunks +
                                  " - Upload: " + dto.getUploadId());

                return new DTOResponse("uploadFileChunk_" + dto.getUploadId() + "_" + dto.getChunkNumber(),
                                     "success", "Chunk recibido", null);

            } catch (Exception e) {
                LoggerCentral.error(TAG, "Error en uploadFileChunk: " + e.getMessage());
                return new DTOResponse("uploadFileChunk", "error", "Error procesando chunk", null);
            }
        });

        // RUTA: Finalizar subida
        router.registrarAccion("endFileUpload", (datos, idSesion) -> {
            try {
                DTOEndUpload dto = gson.fromJson(datos, DTOEndUpload.class);

                UploadSession session = uploadSessions.get(dto.getUploadId());
                if (session == null) {
                    return new DTOResponse("endFileUpload", "error", "Sesión no encontrada", null);
                }

                // Validar que todos los chunks estén completos
                if (session.chunks.size() != session.totalChunks) {
                    return new DTOResponse("endFileUpload", "error",
                        "Chunks incompletos: " + session.chunks.size() + "/" + session.totalChunks, null);
                }

                // Ensamblar archivo
                byte[] fileData = ensamblarArchivo(session);

                // Verificar hash
                String hashCalculado = calcularHashSHA256(fileData);
                if (!hashCalculado.equals(dto.getFileHash())) {
                    LoggerCentral.warn(TAG, "Hash no coincide - Esperado: " + dto.getFileHash() +
                                     ", Calculado: " + hashCalculado);
                }

                // Determinar ruta según tipo de archivo
                String categoria = determinarCategoria(session.fileName, session.mimeType);
                String fileId = categoria + "/" + UUID.randomUUID() + "_" + sanitizarNombre(session.fileName);

                // Guardar archivo físicamente en Bucket
                File archivoFisico = guardarArchivoEnBucket(fileId, fileData);

                // Guardar metadatos en BD
                Archivo archivo = new Archivo(fileId, session.fileName, session.mimeType, fileData.length);
                archivo.setRutaRelativa(fileId);
                archivo.setHashSHA256(hashCalculado);
                archivo.setFechaUltimaActualizacion(Instant.now());
                boolean guardado = repoArchivo.guardar(archivo);

                // Limpiar sesión
                uploadSessions.remove(dto.getUploadId());

                LoggerCentral.info(TAG, "✅ Archivo guardado: " + fileId + " - Tamaño: " + fileData.length + " bytes");

                // ✅ ACTIVAR SINCRONIZACIÓN P2P (igual que ServicioChat)
                if (guardado && servicioSync != null) {
                    LoggerCentral.info(TAG, "🔄 Activando sincronización P2P para archivo: " + fileId);
                    servicioSync.onBaseDeDatosCambio(); // Reconstruir Merkle Tree
                    servicioSync.forzarSincronizacion(); // Sincronizar con peers
                }

                // Respuesta
                Map<String, Object> response = new HashMap<>();
                response.put("fileId", fileId);
                response.put("fileName", session.fileName);
                response.put("size", fileData.length);
                response.put("mimeType", session.mimeType);
                response.put("hash", hashCalculado);

                return new DTOResponse("endFileUpload", "success", "Archivo guardado", gson.toJsonTree(response));

            } catch (Exception e) {
                LoggerCentral.error(TAG, "Error en endFileUpload: " + e.getMessage());
                e.printStackTrace();
                return new DTOResponse("endFileUpload", "error", "Error al guardar archivo", null);
            }
        });

        // ==================== RUTAS DE DESCARGA ====================

        // RUTA: Iniciar descarga
        router.registrarAccion("startFileDownload", (datos, idSesion) -> {
            try {
                DTOStartDownload dto = gson.fromJson(datos, DTOStartDownload.class);

                // Buscar archivo en BD
                Archivo archivo = repoArchivo.buscarPorFileId(dto.getFileId());
                if (archivo == null) {
                    return new DTOResponse("startFileDownload", "error", "Archivo no encontrado", null);
                }

                // Leer archivo físico
                File archivoFisico = new File(BUCKET_PATH + archivo.getRutaRelativa());
                if (!archivoFisico.exists()) {
                    LoggerCentral.error(TAG, "Archivo físico no existe: " + archivoFisico.getAbsolutePath());
                    return new DTOResponse("startFileDownload", "error", "Archivo no disponible", null);
                }

                byte[] fileData = Files.readAllBytes(archivoFisico.toPath());
                int totalChunks = (int) Math.ceil((double) fileData.length / CHUNK_SIZE);

                // Crear sesión de descarga
                String downloadId = UUID.randomUUID().toString();
                DownloadSession session = new DownloadSession(downloadId, archivo, fileData, totalChunks);
                downloadSessions.put(downloadId, session);

                LoggerCentral.info(TAG, "Download iniciado: " + downloadId + " - FileId: " + dto.getFileId() +
                                 " - Chunks: " + totalChunks);

                // Respuesta con info de descarga
                DTODownloadInfo response = new DTODownloadInfo(
                    downloadId, archivo.getNombreArchivo(), archivo.getMimeType(),
                    archivo.getTamanio(), totalChunks
                );

                return new DTOResponse("startFileDownload", "success", "Download iniciado", gson.toJsonTree(response));

            } catch (Exception e) {
                LoggerCentral.error(TAG, "Error en startFileDownload: " + e.getMessage());
                return new DTOResponse("startFileDownload", "error", "Error al iniciar descarga", null);
            }
        });

        // RUTA: Solicitar chunk
        router.registrarAccion("requestFileChunk", (datos, idSesion) -> {
            try {
                DTORequestChunk dto = gson.fromJson(datos, DTORequestChunk.class);

                DownloadSession session = downloadSessions.get(dto.getDownloadId());
                if (session == null) {
                    return new DTOResponse("downloadFileChunk_" + dto.getDownloadId() + "_" + dto.getChunkNumber(),
                                         "error", "Sesión no encontrada", null);
                }

                // Extraer chunk solicitado
                int offset = (dto.getChunkNumber() - 1) * CHUNK_SIZE;
                int length = Math.min(CHUNK_SIZE, session.fileData.length - offset);

                byte[] chunkData = new byte[length];
                System.arraycopy(session.fileData, offset, chunkData, 0, length);

                String chunkBase64 = Base64.getEncoder().encodeToString(chunkData);

                LoggerCentral.debug(TAG, "Enviando chunk: " + dto.getChunkNumber() + "/" + session.totalChunks +
                                  " - Download: " + dto.getDownloadId());

                Map<String, Object> response = new HashMap<>();
                response.put("chunkDataBase64", chunkBase64);
                response.put("chunkNumber", dto.getChunkNumber());

                // Si es el último chunk, limpiar sesión
                if (dto.getChunkNumber() == session.totalChunks) {
                    downloadSessions.remove(dto.getDownloadId());
                    LoggerCentral.info(TAG, "Download completado: " + dto.getDownloadId());
                }

                return new DTOResponse("downloadFileChunk_" + dto.getDownloadId() + "_" + dto.getChunkNumber(),
                                     "success", "Chunk enviado", gson.toJsonTree(response));

            } catch (Exception e) {
                LoggerCentral.error(TAG, "Error en requestFileChunk: " + e.getMessage());
                return new DTOResponse("requestFileChunk", "error", "Error al enviar chunk", null);
            }
        });

        LoggerCentral.info(TAG, "Servicio de archivos inicializado - Bucket: " + BUCKET_PATH);
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private void crearDirectorioBucket() {
        try {
            Path bucketPath = Paths.get(BUCKET_PATH);
            if (!Files.exists(bucketPath)) {
                Files.createDirectories(bucketPath);
                LoggerCentral.info(TAG, "Directorio Bucket creado: " + BUCKET_PATH);
            }

            // Crear subdirectorios para categorías
            String[] categorias = {"user_photos", "images", "audio", "documents", "otros"};
            for (String cat : categorias) {
                Path catPath = Paths.get(BUCKET_PATH + cat);
                if (!Files.exists(catPath)) {
                    Files.createDirectories(catPath);
                }
            }
        } catch (IOException e) {
            LoggerCentral.error(TAG, "Error creando directorio Bucket: " + e.getMessage());
        }
    }

    private byte[] ensamblarArchivo(UploadSession session) {
        int totalSize = session.chunks.values().stream().mapToInt(chunk -> chunk.length).sum();
        byte[] fileData = new byte[totalSize];

        int offset = 0;
        for (int i = 1; i <= session.totalChunks; i++) {
            byte[] chunk = session.chunks.get(i);
            if (chunk != null) {
                System.arraycopy(chunk, 0, fileData, offset, chunk.length);
                offset += chunk.length;
            }
        }

        return fileData;
    }

    private File guardarArchivoEnBucket(String fileId, byte[] fileData) throws IOException {
        Path filePath = Paths.get(BUCKET_PATH + fileId);

        // Crear directorios padres si no existen
        Files.createDirectories(filePath.getParent());

        // Escribir archivo
        Files.write(filePath, fileData);

        LoggerCentral.info(TAG, "Archivo guardado en: " + filePath.toAbsolutePath());
        return filePath.toFile();
    }

    private String determinarCategoria(String fileName, String mimeType) {
        String fileNameLower = fileName.toLowerCase();
        String mimeTypeLower = mimeType != null ? mimeType.toLowerCase() : "";

        if (mimeTypeLower.startsWith("image/") || fileNameLower.matches(".*\\.(jpg|jpeg|png|gif|bmp|webp)$")) {
            return "images";
        } else if (mimeTypeLower.startsWith("audio/") || fileNameLower.matches(".*\\.(mp3|wav|ogg|m4a|flac)$")) {
            return "audio";
        } else if (mimeTypeLower.contains("pdf") || fileNameLower.matches(".*\\.(pdf|doc|docx|xls|xlsx|ppt|pptx)$")) {
            return "documents";
        } else {
            return "otros";
        }
    }

    private String sanitizarNombre(String fileName) {
        // Remover caracteres peligrosos y limitar longitud
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_")
                       .replaceAll("\\.\\.", "_")
                       .substring(0, Math.min(fileName.length(), 100));
    }

    private String calcularHashSHA256(byte[] data) {
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
            throw new RuntimeException("Error calculando hash", e);
        }
    }

    @Override
    public void iniciar() {
        LoggerCentral.info(TAG, "Servicio de archivos iniciado");
    }

    @Override
    public void detener() {
        uploadSessions.clear();
        downloadSessions.clear();
        LoggerCentral.info(TAG, "Servicio de archivos detenido");
    }

    // ==================== CLASES INTERNAS ====================

    private static class UploadSession {
        String uploadId;
        String userId;
        String fileName;
        String mimeType;
        int totalChunks;
        Map<Integer, byte[]> chunks;
        Instant createdAt;

        UploadSession(String uploadId, String userId, String fileName, String mimeType, int totalChunks) {
            this.uploadId = uploadId;
            this.userId = userId;
            this.fileName = fileName;
            this.mimeType = mimeType;
            this.totalChunks = totalChunks;
            this.chunks = new ConcurrentHashMap<>();
            this.createdAt = Instant.now();
        }

        void addChunk(int chunkNumber, byte[] data) {
            chunks.put(chunkNumber, data);
        }
    }

    private static class DownloadSession {
        String downloadId;
        Archivo archivo;
        byte[] fileData;
        int totalChunks;
        Instant createdAt;

        DownloadSession(String downloadId, Archivo archivo, byte[] fileData, int totalChunks) {
            this.downloadId = downloadId;
            this.archivo = archivo;
            this.fileData = fileData;
            this.totalChunks = totalChunks;
            this.createdAt = Instant.now();
        }
    }
}

