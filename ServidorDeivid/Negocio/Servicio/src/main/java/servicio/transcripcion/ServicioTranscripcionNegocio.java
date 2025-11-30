package servicio.transcripcion;

import dto.transcripcion.DTOAudioTranscripcion;
import gestorTranscripcion.FachadaTranscripcion;
import logger.LoggerCentral;

import java.util.List;

/**
 * Servicio de negocio para gestión de transcripciones
 * Actúa como intermediario entre el Controlador (Presentación) y la Fachada (Gestor)
 * Capa: Negocio/Servicio
 */
public class ServicioTranscripcionNegocio {

    private static final String TAG = "ServicioTranscripcionNegocio";
    private static ServicioTranscripcionNegocio instancia;
    private final FachadaTranscripcion fachadaTranscripcion;

    private ServicioTranscripcionNegocio() {
        this.fachadaTranscripcion = FachadaTranscripcion.getInstance();
        LoggerCentral.info(TAG, "✓ ServicioTranscripcionNegocio inicializado");
    }

    public static synchronized ServicioTranscripcionNegocio getInstance() {
        if (instancia == null) {
            instancia = new ServicioTranscripcionNegocio();
        }
        return instancia;
    }

    /**
     * Inicializa el sistema de transcripción
     */
    public void inicializar() {
        try {
            LoggerCentral.info(TAG, "🚀 Inicializando sistema de transcripción...");

            // 1. Cargar audios existentes desde la BD
            fachadaTranscripcion.cargarAudiosDesdeBaseDatos();

            // 2. Iniciar actualización automática cada 60 segundos
            fachadaTranscripcion.iniciarActualizacionAutomatica(60);

            LoggerCentral.info(TAG, "✓ Sistema de transcripción inicializado correctamente");
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al inicializar sistema de transcripción: " + e.getMessage());
        }
    }

    /**
     * Obtiene todos los audios disponibles
     */
    public List<DTOAudioTranscripcion> obtenerAudios() {
        try {
            return fachadaTranscripcion.obtenerAudios();
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al obtener audios: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Filtra audios por tipo (CANAL o CONTACTO)
     */
    public List<DTOAudioTranscripcion> filtrarPorTipo(String tipo) {
        try {
            return fachadaTranscripcion.filtrarPorTipo(tipo);
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al filtrar por tipo: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Filtra audios por estado de transcripción
     */
    public List<DTOAudioTranscripcion> filtrarPorEstado(boolean transcritos) {
        try {
            return fachadaTranscripcion.filtrarPorEstado(transcritos);
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al filtrar por estado: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Busca audios por texto
     */
    public List<DTOAudioTranscripcion> buscarAudios(String textoBusqueda) {
        try {
            if (textoBusqueda == null || textoBusqueda.trim().isEmpty()) {
                return obtenerAudios();
            }
            return fachadaTranscripcion.buscarAudios(textoBusqueda);
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al buscar audios: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Transcribe un audio manualmente
     */
    public boolean transcribirAudio(String audioId, String transcripcion) {
        try {
            return fachadaTranscripcion.transcribirAudio(audioId, transcripcion);
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al transcribir audio: " + e.getMessage());
            return false;
        }
    }

    /**
     * Inicia transcripción automática de un audio
     */
    public boolean iniciarTranscripcionAutomatica(String audioId) {
        try {
            return fachadaTranscripcion.iniciarTranscripcionAutomatica(audioId);
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al iniciar transcripción automática: " + e.getMessage());
            return false;
        }
    }

    /**
     * Actualiza la tabla de audios manualmente (forzado)
     */
    public void actualizarTablaAudios() {
        try {
            LoggerCentral.info(TAG, "🔄 Actualizando tabla de audios...");
            fachadaTranscripcion.actualizarTablaAudios();

            // Recargar audios después de actualizar
            fachadaTranscripcion.cargarAudiosDesdeBaseDatos();
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al actualizar tabla: " + e.getMessage());
        }
    }

    /**
     * Recarga los audios desde la base de datos
     */
    public void recargarAudios() {
        try {
            LoggerCentral.info(TAG, "🔄 Recargando audios desde base de datos...");
            fachadaTranscripcion.cargarAudiosDesdeBaseDatos();
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al recargar audios: " + e.getMessage());
        }
    }

    /**
     * Obtiene estadísticas de transcripción
     */
    public FachadaTranscripcion.EstadisticasTranscripcion obtenerEstadisticas() {
        try {
            return fachadaTranscripcion.obtenerEstadisticas();
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al obtener estadísticas: " + e.getMessage());
            return new FachadaTranscripcion.EstadisticasTranscripcion(0, 0, 0, 0);
        }
    }

    /**
     * Verifica si el servicio de transcripción está disponible
     */
    public boolean isTranscripcionDisponible() {
        try {
            return fachadaTranscripcion.isTranscripcionDisponible();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Busca un audio específico por su ID
     */
    public DTOAudioTranscripcion buscarAudioPorId(String audioId) {
        try {
            return fachadaTranscripcion.buscarAudioPorId(audioId);
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al buscar audio por ID: " + e.getMessage());
            return null;
        }
    }

    /**
     * Detiene el servicio de transcripción
     */
    public void detener() {
        try {
            LoggerCentral.info(TAG, "Deteniendo servicio de transcripción...");
            fachadaTranscripcion.detenerActualizacionAutomatica();
            fachadaTranscripcion.detener();
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al detener servicio: " + e.getMessage());
        }
    }
}

