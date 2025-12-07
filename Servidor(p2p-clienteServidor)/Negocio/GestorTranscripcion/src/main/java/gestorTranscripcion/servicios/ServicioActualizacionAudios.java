package gestorTranscripcion.servicios;

import dominio.clienteServidor.Archivo;
import dominio.clienteServidor.Transcripcion;
import dominio.clienteServidor.Transcripcion.EstadoTranscripcion;
import logger.LoggerCentral;
import repositorio.clienteServidor.ArchivoRepositorio;
import repositorio.clienteServidor.TranscripcionRepositorio;

import java.io.File;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Servicio que actualiza automáticamente la tabla de audios
 * Detecta nuevos archivos de audio en Bucket/audio/ y crea registros de transcripción
 */
public class ServicioActualizacionAudios {

    private static final String TAG = "ServicioActualizacionAudios";
    private static ServicioActualizacionAudios instancia;

    private final ArchivoRepositorio archivoRepo;
    private final TranscripcionRepositorio transcripcionRepo;
    private final ScheduledExecutorService scheduler;
    private final String rutaBucket;

    private boolean activo = false;

    private ServicioActualizacionAudios() {
        this.archivoRepo = new ArchivoRepositorio();
        this.transcripcionRepo = new TranscripcionRepositorio();
        this.scheduler = Executors.newScheduledThreadPool(1);

        // Ruta al Bucket (ajustar según configuración)
        this.rutaBucket = System.getProperty("bucket.path", "./Bucket");

        LoggerCentral.info(TAG, "Servicio de actualización de audios inicializado");
    }

    public static synchronized ServicioActualizacionAudios getInstance() {
        if (instancia == null) {
            instancia = new ServicioActualizacionAudios();
        }
        return instancia;
    }

    /**
     * Inicia el servicio de actualización automática
     * @param intervaloSegundos Intervalo entre actualizaciones
     */
    public void iniciar(int intervaloSegundos) {
        if (activo) {
            LoggerCentral.warn(TAG, "El servicio ya está activo");
            return;
        }

        activo = true;
        LoggerCentral.info(TAG, "🔄 Iniciando servicio de actualización automática cada " + intervaloSegundos + " segundos");

        // Ejecutar inmediatamente y luego periódicamente
        scheduler.scheduleAtFixedRate(
            this::actualizarTablaAudios,
            0,
            intervaloSegundos,
            TimeUnit.SECONDS
        );
    }

    /**
     * Detiene el servicio de actualización automática
     */
    public void detener() {
        if (!activo) {
            return;
        }

        activo = false;
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            LoggerCentral.info(TAG, "Servicio de actualización detenido");
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Actualiza la tabla de audios detectando nuevos archivos
     */
    public void actualizarTablaAudios() {
        try {
            LoggerCentral.debug(TAG, "🔍 Buscando nuevos audios...");

            // 1. Sincronizar archivos físicos con la tabla de archivos
            sincronizarArchivosDesdeFileSystem();

            // 2. Detectar audios sin transcripción
            List<UUID> audiosSinTranscripcion = transcripcionRepo.obtenerAudiosSinTranscripcion();

            if (audiosSinTranscripcion.isEmpty()) {
                LoggerCentral.debug(TAG, "No hay audios nuevos para procesar");
                return;
            }

            LoggerCentral.info(TAG, "📁 Encontrados " + audiosSinTranscripcion.size() + " audios sin transcripción");

            // 3. Crear registros de transcripción pendientes
            int creados = 0;
            for (UUID archivoId : audiosSinTranscripcion) {
                if (crearTranscripcionPendiente(archivoId)) {
                    creados++;
                }
            }

            LoggerCentral.info(TAG, "✓ Creados " + creados + " registros de transcripción pendientes");

        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al actualizar tabla de audios: " + e.getMessage());
        }
    }

    /**
     * Sincroniza archivos físicos del Bucket con la tabla de archivos
     */
    private void sincronizarArchivosDesdeFileSystem() {
        File carpetaAudio = new File(rutaBucket + "/audio");

        if (!carpetaAudio.exists() || !carpetaAudio.isDirectory()) {
            LoggerCentral.warn(TAG, "Carpeta de audio no existe: " + carpetaAudio.getAbsolutePath());
            return;
        }

        File[] archivos = carpetaAudio.listFiles((dir, name) ->
            name.toLowerCase().endsWith(".wav") ||
            name.toLowerCase().endsWith(".mp3") ||
            name.toLowerCase().endsWith(".ogg") ||
            name.toLowerCase().endsWith(".flac")
        );

        if (archivos == null || archivos.length == 0) {
            return;
        }

        LoggerCentral.debug(TAG, "Archivos de audio encontrados en filesystem: " + archivos.length);

        for (File archivo : archivos) {
            sincronizarArchivo(archivo);
        }
    }

    /**
     * Sincroniza un archivo individual con la base de datos
     */
    private void sincronizarArchivo(File archivo) {
        try {
            String nombreArchivo = archivo.getName();
            String fileId = "audio/" + nombreArchivo;

            // Verificar si ya existe en la BD
            if (archivoRepo.existe(fileId)) {
                return;
            }

            // Crear nuevo registro de archivo
            Archivo nuevoArchivo = new Archivo();
            nuevoArchivo.setFileId(fileId);
            nuevoArchivo.setNombreArchivo(nombreArchivo);
            nuevoArchivo.setRutaRelativa(fileId);
            nuevoArchivo.setMimeType(detectarMimeType(nombreArchivo));
            nuevoArchivo.setTamanio(archivo.length());

            if (archivoRepo.guardar(nuevoArchivo)) {
                LoggerCentral.info(TAG, "✓ Archivo registrado en BD: " + fileId);
            }

        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error sincronizando archivo: " + e.getMessage());
        }
    }

    /**
     * Crea un registro de transcripción pendiente para un audio
     */
    private boolean crearTranscripcionPendiente(UUID archivoId) {
        try {
            Archivo archivo = archivoRepo.buscarPorId(archivoId);
            if (archivo == null) {
                LoggerCentral.warn(TAG, "Archivo no encontrado: " + archivoId);
                return false;
            }

            Transcripcion transcripcion = new Transcripcion(archivoId);
            transcripcion.setEstado(EstadoTranscripcion.PENDIENTE);
            transcripcion.setIdioma("es");

            // Calcular duración si es posible
            BigDecimal duracion = calcularDuracionAudio(archivo);
            if (duracion != null) {
                transcripcion.setDuracionSegundos(duracion);
            }

            if (transcripcionRepo.guardar(transcripcion)) {
                LoggerCentral.info(TAG, "✓ Transcripción pendiente creada para: " + archivo.getFileId());
                return true;
            }

        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error creando transcripción pendiente: " + e.getMessage());
        }
        return false;
    }

    /**
     * Detecta el MIME type basado en la extensión
     */
    private String detectarMimeType(String nombreArchivo) {
        String ext = nombreArchivo.substring(nombreArchivo.lastIndexOf(".") + 1).toLowerCase();
        switch (ext) {
            case "wav":
                return "audio/wav";
            case "mp3":
                return "audio/mpeg";
            case "ogg":
                return "audio/ogg";
            case "flac":
                return "audio/flac";
            case "m4a":
                return "audio/mp4";
            default:
                return "audio/unknown";
        }
    }

    /**
     * Calcula la duración del audio (simplificado)
     * TODO: Implementar cálculo real usando bibliotecas de audio
     */
    private BigDecimal calcularDuracionAudio(Archivo archivo) {
        try {
            // Por ahora, estimación básica basada en el tamaño
            // WAV: típicamente 16 bits, 44100 Hz, mono = ~88.2 KB/s
            long tamanio = archivo.getTamanio();
            double duracionEstimada = tamanio / 88200.0; // Aproximación
            return BigDecimal.valueOf(Math.max(0, duracionEstimada));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Obtiene todos los audios pendientes de transcripción
     */
    public List<Transcripcion> obtenerAudiosPendientes() {
        return transcripcionRepo.buscarPorEstado(EstadoTranscripcion.PENDIENTE);
    }

    /**
     * Actualiza el estado de una transcripción
     */
    public boolean actualizarEstadoTranscripcion(UUID transcripcionId, EstadoTranscripcion nuevoEstado, String transcripcion) {
        try {
            Transcripcion t = transcripcionRepo.buscarPorId(transcripcionId);
            if (t == null) {
                LoggerCentral.warn(TAG, "Transcripción no encontrada: " + transcripcionId);
                return false;
            }

            t.setEstado(nuevoEstado);
            if (transcripcion != null) {
                t.setTranscripcion(transcripcion);
            }

            return transcripcionRepo.actualizar(t);
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error actualizando estado: " + e.getMessage());
            return false;
        }
    }

    /**
     * Fuerza una actualización manual inmediata
     */
    public void actualizarAhora() {
        LoggerCentral.info(TAG, "Forzando actualización manual...");
        actualizarTablaAudios();
    }

    public boolean estaActivo() {
        return activo;
    }
}

