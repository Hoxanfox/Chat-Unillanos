package gestorP2P.servicios;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import conexion.p2p.interfaces.IGestorConexiones;
import conexion.p2p.interfaces.IRouterMensajes;
import dominio.clienteServidor.Archivo;
import dto.comunicacion.DTORequest;
import dto.comunicacion.DTOResponse;
import gestorP2P.interfaces.IServicioP2P;
import gestorP2P.utils.GsonUtil;
import logger.LoggerCentral;
import repositorio.clienteServidor.ArchivoRepositorio;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

/**
 * Servicio P2P para transferir archivos físicos del Bucket/ entre peers.
 * Se activa automáticamente después de sincronizar metadatos de archivos.
 *
 * Flujo:
 * 1. Detecta archivos con metadatos en BD pero sin archivo físico en Bucket/
 * 2. Solicita el archivo a peers disponibles
 * 3. Descarga por chunks con verificación de integridad
 * 4. Guarda en Bucket/ local
 */
public class ServicioTransferenciaArchivos implements IServicioP2P {

    private static final String TAG = "TransferenciaArchivos";
    private static final String BUCKET_PATH = "./Bucket/";
    private static final int CHUNK_SIZE = 524288; // 512 KB (igual que ServicioArchivos)

    // Colores ANSI
    private static final String RESET = "\u001B[0m";
    private static final String VERDE = "\u001B[32m";
    private static final String AMARILLO = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";
    private static final String ROJO = "\u001B[31m";
    private static final String AZUL = "\u001B[34m";

    private IGestorConexiones gestor;
    private ServicioNotificacionCambios notificador; // ✅ NUEVO: Para notificar progreso de descargas
    private final Gson gson;
    private final ArchivoRepositorio repoArchivo;

    // Executor para descargas en paralelo
    private final ExecutorService executorDescargas;

    // Cache de descargas en progreso (evita descargar el mismo archivo 2 veces)
    private final Set<String> descargasEnProgreso;

    // Cache temporal de chunks recibidos durante una descarga
    private final Map<String, DescargaEnProgreso> cacheDescargas;

    public ServicioTransferenciaArchivos() {
        this.gson = GsonUtil.crearGson();
        this.repoArchivo = new ArchivoRepositorio();
        this.executorDescargas = Executors.newFixedThreadPool(3); // Max 3 descargas simultáneas
        this.descargasEnProgreso = ConcurrentHashMap.newKeySet();
        this.cacheDescargas = new ConcurrentHashMap<>();
        crearDirectorioBucket();
    }

    /**
     * ✅ NUEVO: Permite inyectar el notificador para enviar progreso de descargas a clientes.
     */
    public void setNotificador(ServicioNotificacionCambios notificador) {
        this.notificador = notificador;
        LoggerCentral.info(TAG, "Notificador configurado para tracking de progreso");
    }

    @Override
    public String getNombre() {
        return "ServicioTransferenciaArchivos";
    }

    @Override
    public void inicializar(IGestorConexiones gestor, IRouterMensajes router) {
        this.gestor = gestor;
        LoggerCentral.info(TAG, AZUL + "Inicializando ServicioTransferenciaArchivos..." + RESET);

        // ==================== RUTAS P2P ====================

        // RUTA: Solicitar metadatos de archivo (para verificar disponibilidad)
        router.registrarAccion("p2p_file_metadata_request", (datos, origen) -> {
            try {
                String fileId = datos.getAsString();
                LoggerCentral.debug(TAG, "Solicitud de metadata para archivo: " + fileId + " desde: " + origen);

                Archivo archivo = repoArchivo.buscarPorFileId(fileId);
                if (archivo == null) {
                    return new DTOResponse("p2p_file_metadata_request", "not_found", "Archivo no encontrado", null);
                }

                // Verificar que el archivo físico exista
                File archivoFisico = new File(BUCKET_PATH + archivo.getRutaRelativa());
                if (!archivoFisico.exists()) {
                    LoggerCentral.warn(TAG, "Metadato existe pero archivo físico NO: " + fileId);
                    return new DTOResponse("p2p_file_metadata_request", "not_available", "Archivo físico no disponible", null);
                }

                // Responder con metadatos
                JsonObject metadata = new JsonObject();
                metadata.addProperty("fileId", archivo.getFileId());
                metadata.addProperty("nombreArchivo", archivo.getNombreArchivo());
                metadata.addProperty("tamanio", archivo.getTamanio());
                metadata.addProperty("mimeType", archivo.getMimeType());
                metadata.addProperty("hashSHA256", archivo.getHashSHA256());
                metadata.addProperty("totalChunks", calcularTotalChunks(archivo.getTamanio()));

                LoggerCentral.debug(TAG, VERDE + "✓ Archivo disponible: " + fileId + RESET);
                return new DTOResponse("p2p_file_metadata_request", "success", "Archivo disponible", metadata);

            } catch (Exception e) {
                LoggerCentral.error(TAG, "Error en p2p_file_metadata_request: " + e.getMessage());
                return new DTOResponse("p2p_file_metadata_request", "error", "Error interno", null);
            }
        });

        // RUTA: Solicitar chunk de archivo
        router.registrarAccion("p2p_file_chunk_request", (datos, origen) -> {
            try {
                JsonObject req = datos.getAsJsonObject();
                String fileId = req.get("fileId").getAsString();
                int chunkNumber = req.get("chunkNumber").getAsInt();

                LoggerCentral.debug(TAG, "Solicitud de chunk " + chunkNumber + " para archivo: " + fileId);

                Archivo archivo = repoArchivo.buscarPorFileId(fileId);
                if (archivo == null) {
                    return new DTOResponse("p2p_file_chunk_request", "not_found", "Archivo no encontrado", null);
                }

                // Leer archivo físico
                File archivoFisico = new File(BUCKET_PATH + archivo.getRutaRelativa());
                if (!archivoFisico.exists()) {
                    return new DTOResponse("p2p_file_chunk_request", "not_available", "Archivo físico no disponible", null);
                }

                byte[] fileData = Files.readAllBytes(archivoFisico.toPath());

                // Extraer chunk solicitado
                int offset = (chunkNumber - 1) * CHUNK_SIZE;
                int length = Math.min(CHUNK_SIZE, fileData.length - offset);

                if (offset >= fileData.length || length <= 0) {
                    return new DTOResponse("p2p_file_chunk_request", "invalid_chunk", "Chunk fuera de rango", null);
                }

                byte[] chunkData = new byte[length];
                System.arraycopy(fileData, offset, chunkData, 0, length);

                String chunkBase64 = Base64.getEncoder().encodeToString(chunkData);

                JsonObject response = new JsonObject();
                response.addProperty("fileId", fileId);
                response.addProperty("chunkNumber", chunkNumber);
                response.addProperty("chunkDataBase64", chunkBase64);

                LoggerCentral.debug(TAG, VERDE + "✓ Enviando chunk " + chunkNumber + " (" + length + " bytes)" + RESET);
                return new DTOResponse("p2p_file_chunk_request", "success", "Chunk enviado", response);

            } catch (Exception e) {
                LoggerCentral.error(TAG, "Error en p2p_file_chunk_request: " + e.getMessage());
                return new DTOResponse("p2p_file_chunk_request", "error", "Error interno", null);
            }
        });

        // MANEJADOR DE RESPUESTA: Metadatos recibidos
        router.registrarManejadorRespuesta("p2p_file_metadata_request", (resp) -> {
            if (resp.fueExitoso() && resp.getData() != null) {
                JsonObject metadata = resp.getData().getAsJsonObject();
                String fileId = metadata.get("fileId").getAsString();
                int totalChunks = metadata.get("totalChunks").getAsInt();

                LoggerCentral.info(TAG, CYAN + "✓ Metadata recibida para: " + fileId + " (" + totalChunks + " chunks)" + RESET);

                // Iniciar descarga de chunks
                iniciarDescargaChunks(fileId, metadata);
            } else {
                LoggerCentral.warn(TAG, AMARILLO + "Archivo no disponible en peer: " + resp.getStatus() + RESET);
            }
        });

        // MANEJADOR DE RESPUESTA: Chunk recibido
        router.registrarManejadorRespuesta("p2p_file_chunk_request", (resp) -> {
            if (resp.fueExitoso() && resp.getData() != null) {
                JsonObject chunkData = resp.getData().getAsJsonObject();
                procesarChunkRecibido(chunkData);
            } else {
                LoggerCentral.error(TAG, ROJO + "Error al recibir chunk: " + resp.getStatus() + RESET);
            }
        });

        LoggerCentral.info(TAG, VERDE + "ServicioTransferenciaArchivos inicializado" + RESET);
    }

    /**
     * Método público para verificar y descargar archivos faltantes.
     * Llamado desde ServicioSincronizacionDatos después de sincronizar metadatos.
     */
    public void verificarYDescargarArchivosFaltantes() {
        LoggerCentral.info(TAG, AZUL + "🔍 Verificando archivos faltantes en Bucket/..." + RESET);

        // Obtener todos los archivos de la BD
        List<Archivo> todosLosArchivos = repoArchivo.obtenerTodosParaSync();
        List<Archivo> archivosFaltantes = new ArrayList<>();

        for (Archivo archivo : todosLosArchivos) {
            File archivoFisico = new File(BUCKET_PATH + archivo.getRutaRelativa());
            if (!archivoFisico.exists()) {
                archivosFaltantes.add(archivo);
                LoggerCentral.warn(TAG, AMARILLO + "⚠ Archivo faltante: " + archivo.getFileId() + RESET);
            }
        }

        if (archivosFaltantes.isEmpty()) {
            LoggerCentral.info(TAG, VERDE + "✓ Todos los archivos están sincronizados" + RESET);
            return;
        }

        LoggerCentral.info(TAG, CYAN + "📥 Descargando " + archivosFaltantes.size() + " archivos faltantes..." + RESET);

        // Descargar archivos en paralelo
        for (Archivo archivo : archivosFaltantes) {
            if (!descargasEnProgreso.contains(archivo.getFileId())) {
                descargasEnProgreso.add(archivo.getFileId());
                executorDescargas.submit(() -> descargarArchivo(archivo));
            }
        }
    }

    /**
     * Descarga un archivo completo de un peer.
     */
    private void descargarArchivo(Archivo archivo) {
        try {
            LoggerCentral.info(TAG, CYAN + "📥 Iniciando descarga: " + archivo.getNombreArchivo() + RESET);

            // Solicitar metadatos para confirmar disponibilidad
            DTORequest req = new DTORequest("p2p_file_metadata_request", gson.toJsonTree(archivo.getFileId()));
            String jsonReq = gson.toJson(req);
            gestor.broadcast(jsonReq);

            // La descarga continuará en el manejador de respuesta

        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al descargar archivo: " + e.getMessage());
            descargasEnProgreso.remove(archivo.getFileId());
        }
    }

    /**
     * Inicia la descarga secuencial de chunks.
     */
    private void iniciarDescargaChunks(String fileId, JsonObject metadata) {
        try {
            int totalChunks = metadata.get("totalChunks").getAsInt();
            String nombreArchivo = metadata.get("nombreArchivo").getAsString();
            String hashEsperado = metadata.get("hashSHA256").getAsString();

            // Crear sesión de descarga
            DescargaEnProgreso descarga = new DescargaEnProgreso(fileId, nombreArchivo, totalChunks, hashEsperado, metadata);
            cacheDescargas.put(fileId, descarga);

            LoggerCentral.info(TAG, CYAN + "⬇ Descargando " + nombreArchivo + " (" + totalChunks + " chunks)" + RESET);

            // Solicitar chunks secuencialmente
            for (int i = 1; i <= totalChunks; i++) {
                solicitarChunk(fileId, i);
            }

        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error iniciando descarga de chunks: " + e.getMessage());
            cacheDescargas.remove(fileId);
            descargasEnProgreso.remove(fileId);
        }
    }

    /**
     * Solicita un chunk específico de un archivo.
     */
    private void solicitarChunk(String fileId, int chunkNumber) {
        try {
            JsonObject req = new JsonObject();
            req.addProperty("fileId", fileId);
            req.addProperty("chunkNumber", chunkNumber);

            DTORequest dtoReq = new DTORequest("p2p_file_chunk_request", req);
            String jsonReq = gson.toJson(dtoReq);
            gestor.broadcast(jsonReq);

            LoggerCentral.debug(TAG, "Solicitando chunk " + chunkNumber + " de " + fileId);

        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error solicitando chunk: " + e.getMessage());
        }
    }

    /**
     * Procesa un chunk recibido y ensambla el archivo si está completo.
     */
    private void procesarChunkRecibido(JsonObject chunkData) {
        try {
            String fileId = chunkData.get("fileId").getAsString();
            int chunkNumber = chunkData.get("chunkNumber").getAsInt();
            String chunkBase64 = chunkData.get("chunkDataBase64").getAsString();

            DescargaEnProgreso descarga = cacheDescargas.get(fileId);
            if (descarga == null) {
                LoggerCentral.warn(TAG, "Chunk recibido para descarga no existente: " + fileId);
                return;
            }

            // Decodificar y guardar chunk
            byte[] chunkBytes = Base64.getDecoder().decode(chunkBase64);
            descarga.chunks.put(chunkNumber, chunkBytes);

            LoggerCentral.debug(TAG, VERDE + "✓ Chunk " + chunkNumber + "/" + descarga.totalChunks + " recibido" + RESET);

            // Verificar si la descarga está completa
            if (descarga.chunks.size() == descarga.totalChunks) {
                LoggerCentral.info(TAG, CYAN + "✓ Todos los chunks recibidos. Ensamblando archivo..." + RESET);
                ensamblarYGuardarArchivo(descarga);
            }

            // ==================== NUEVO: Mostrar barra de progreso ====================
            double progreso = (double) descarga.chunks.size() / descarga.totalChunks * 100;
            mostrarBarraProgreso(descarga.nombreArchivo, descarga.chunks.size(), descarga.totalChunks, progreso);
            notificarProgreso(descarga, descarga.chunks.size(), progreso);

        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error procesando chunk: " + e.getMessage());
        }
    }

    /**
     * Ensambla todos los chunks y guarda el archivo en Bucket/.
     */
    private void ensamblarYGuardarArchivo(DescargaEnProgreso descarga) {
        try {
            // Calcular tamaño total
            int totalSize = descarga.chunks.values().stream().mapToInt(chunk -> chunk.length).sum();
            byte[] fileData = new byte[totalSize];

            // Ensamblar chunks en orden
            int offset = 0;
            for (int i = 1; i <= descarga.totalChunks; i++) {
                byte[] chunk = descarga.chunks.get(i);
                if (chunk != null) {
                    System.arraycopy(chunk, 0, fileData, offset, chunk.length);
                    offset += chunk.length;
                }
            }

            // Verificar hash
            String hashCalculado = calcularHashSHA256(fileData);
            if (!hashCalculado.equals(descarga.hashEsperado)) {
                LoggerCentral.error(TAG, ROJO + "❌ Hash no coincide para: " + descarga.nombreArchivo + RESET);
                LoggerCentral.error(TAG, "Esperado: " + descarga.hashEsperado);
                LoggerCentral.error(TAG, "Calculado: " + hashCalculado);
                return;
            }

            // Guardar archivo en Bucket/
            Archivo archivo = repoArchivo.buscarPorFileId(descarga.fileId);
            if (archivo == null) {
                LoggerCentral.error(TAG, "Metadato no encontrado para: " + descarga.fileId);
                return;
            }

            Path filePath = Paths.get(BUCKET_PATH + archivo.getRutaRelativa());
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, fileData);

            LoggerCentral.info(TAG, VERDE + "✅ Archivo descargado y guardado: " + descarga.nombreArchivo + RESET);
            LoggerCentral.info(TAG, "   Ruta: " + filePath.toAbsolutePath());
            LoggerCentral.info(TAG, "   Tamaño: " + fileData.length + " bytes");

            // Limpiar cache
            cacheDescargas.remove(descarga.fileId);
            descargasEnProgreso.remove(descarga.fileId);

        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error ensamblando archivo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== MÉTODOS AUXILIARES ====================

    /**
     * ✅ NUEVO: Muestra una barra de progreso visual en los logs.
     */
    private void mostrarBarraProgreso(String nombreArchivo, int chunksRecibidos, int totalChunks, double progreso) {
        int barraLongitud = 30;
        int bloqueCompletos = (int) (barraLongitud * progreso / 100);

        StringBuilder barra = new StringBuilder("[");
        for (int i = 0; i < barraLongitud; i++) {
            if (i < bloqueCompletos) {
                barra.append("█");
            } else {
                barra.append("░");
            }
        }
        barra.append("]");

        // Formatear tamaño descargado
        long bytesDescargados = (long) chunksRecibidos * CHUNK_SIZE;
        long totalBytes = (long) totalChunks * CHUNK_SIZE;
        String tamanioDescargado = formatearTamanio(bytesDescargados);
        String tamanioTotal = formatearTamanio(totalBytes);

        LoggerCentral.info(TAG, String.format(
                CYAN + "📥 %s " + RESET + "%s " + VERDE + "%.1f%%" + RESET + " (%d/%d chunks) [%s / %s]",
                nombreArchivo,
                barra.toString(),
                progreso,
                chunksRecibidos,
                totalChunks,
                tamanioDescargado,
                tamanioTotal
        ));
    }

    /**
     * ✅ NUEVO: Notifica el progreso de descarga a los clientes conectados.
     */
    private void notificarProgreso(DescargaEnProgreso descarga, int chunksRecibidos, double progreso) {
        if (notificador == null) {
            return;
        }

        try {
            // Crear objeto de progreso para enviar a clientes
            JsonObject progresoData = new JsonObject();
            progresoData.addProperty("fileId", descarga.fileId);
            progresoData.addProperty("nombreArchivo", descarga.nombreArchivo);
            progresoData.addProperty("chunksRecibidos", chunksRecibidos);
            progresoData.addProperty("totalChunks", descarga.totalChunks);
            progresoData.addProperty("progreso", Math.round(progreso * 10) / 10.0); // 1 decimal
            progresoData.addProperty("estado", "descargando");

            // Calcular velocidad y tiempo estimado
            long tiempoTranscurrido = System.currentTimeMillis() - descarga.inicioDescarga;
            if (tiempoTranscurrido > 0) {
                long bytesDescargados = (long) chunksRecibidos * CHUNK_SIZE;
                double velocidadBps = (bytesDescargados * 1000.0) / tiempoTranscurrido;
                progresoData.addProperty("velocidad", formatearVelocidad(velocidadBps));

                // Tiempo estimado restante
                if (progreso > 0) {
                    long tiempoEstimado = (long) ((tiempoTranscurrido / progreso) * (100 - progreso));
                    progresoData.addProperty("tiempoRestante", formatearTiempo(tiempoEstimado));
                }
            }

            // Notificar solo cada 10% de progreso para evitar spam
            int progresoInt = (int) progreso;
            if (progresoInt % 10 == 0 && progresoInt != descarga.ultimoProgresoNotificado) {
                descarga.ultimoProgresoNotificado = progresoInt;
                notificador.notificarCambio(
                        ServicioNotificacionCambios.TipoEvento.PROGRESO_DESCARGA,
                        progresoData
                );
            }

        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error notificando progreso: " + e.getMessage());
        }
    }

    /**
     * ✅ NUEVO: Formatea el tamaño en bytes a formato legible (KB, MB, GB).
     */
    private String formatearTamanio(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /**
     * ✅ NUEVO: Formatea la velocidad en bytes/segundo a formato legible.
     */
    private String formatearVelocidad(double bytesPerSegundo) {
        if (bytesPerSegundo < 1024) return String.format("%.2f B/s", bytesPerSegundo);
        if (bytesPerSegundo < 1024 * 1024) return String.format("%.2f KB/s", bytesPerSegundo / 1024);
        return String.format("%.2f MB/s", bytesPerSegundo / (1024 * 1024));
    }

    /**
     * ✅ NUEVO: Formatea tiempo en milisegundos a formato legible.
     */
    private String formatearTiempo(long milisegundos) {
        long segundos = milisegundos / 1000;
        if (segundos < 60) return segundos + "s";
        long minutos = segundos / 60;
        segundos = segundos % 60;
        if (minutos < 60) return minutos + "m " + segundos + "s";
        long horas = minutos / 60;
        minutos = minutos % 60;
        return horas + "h " + minutos + "m";
    }

    private void crearDirectorioBucket() {
        try {
            Path bucketPath = Paths.get(BUCKET_PATH);
            if (!Files.exists(bucketPath)) {
                Files.createDirectories(bucketPath);
            }

            String[] categorias = {"user_photos", "images", "audio", "documents", "otros"};
            for (String cat : categorias) {
                Path catPath = Paths.get(BUCKET_PATH + cat);
                if (!Files.exists(catPath)) {
                    Files.createDirectories(catPath);
                }
            }
        } catch (IOException e) {
            LoggerCentral.error(TAG, "Error creando directorios: " + e.getMessage());
        }
    }

    private int calcularTotalChunks(long tamanio) {
        return (int) Math.ceil((double) tamanio / CHUNK_SIZE);
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
        LoggerCentral.info(TAG, "Servicio de transferencia de archivos P2P iniciado");
    }

    @Override
    public void detener() {
        executorDescargas.shutdown();
        try {
            if (!executorDescargas.awaitTermination(10, TimeUnit.SECONDS)) {
                executorDescargas.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorDescargas.shutdownNow();
        }
        LoggerCentral.info(TAG, "Servicio de transferencia de archivos P2P detenido");
    }

    // ==================== CLASE INTERNA ====================

    private static class DescargaEnProgreso {
        String fileId;
        String nombreArchivo;
        int totalChunks;
        String hashEsperado;
        JsonObject metadata;
        Map<Integer, byte[]> chunks;
        long inicioDescarga;
        int ultimoProgresoNotificado; // ✅ NUEVO: Para evitar spam de notificaciones

        DescargaEnProgreso(String fileId, String nombreArchivo, int totalChunks, String hashEsperado, JsonObject metadata) {
            this.fileId = fileId;
            this.nombreArchivo = nombreArchivo;
            this.totalChunks = totalChunks;
            this.hashEsperado = hashEsperado;
            this.metadata = metadata;
            this.chunks = new ConcurrentHashMap<>();
            this.inicioDescarga = System.currentTimeMillis();
            this.ultimoProgresoNotificado = -1; // ✅ NUEVO: Inicializar en -1
        }
    }
}

