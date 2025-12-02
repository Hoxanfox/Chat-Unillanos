package servicio.transcripcion;

import configuracion.Configuracion;
import dto.transcripcion.DTOAudioTranscripcion;
import gestorTranscripcion.FachadaTranscripcion;
import logger.LoggerCentral;

import java.io.File;
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

            // Obtener configuración
            Configuracion config = Configuracion.getInstance();
            
            // 1. Verificar si Vosk está habilitado
            if (config.isVoskHabilitado()) {
                String rutaModelo = config.getVoskModeloRuta();
                LoggerCentral.info(TAG, "📂 Ruta del modelo Vosk configurada: " + rutaModelo);
                
                // Verificar si el modelo existe
                File modeloDir = new File(rutaModelo);
                if (modeloDir.exists() && modeloDir.isDirectory()) {
                    LoggerCentral.info(TAG, "✓ Directorio del modelo encontrado");
                    
                    // Inicializar el modelo de Vosk
                    boolean modeloCargado = fachadaTranscripcion.inicializarModeloTranscripcion(rutaModelo);
                    
                    if (modeloCargado) {
                        LoggerCentral.info(TAG, "✅ Modelo Vosk cargado exitosamente");
                    } else {
                        LoggerCentral.warn(TAG, "⚠️ No se pudo cargar el modelo Vosk");
                        LoggerCentral.warn(TAG, "   La transcripción automática no estará disponible");
                    }
                } else {
                    LoggerCentral.warn(TAG, "⚠️ Modelo Vosk no encontrado en: " + rutaModelo);
                    LoggerCentral.warn(TAG, "   Descarga el modelo desde: https://alphacephei.com/vosk/models");
                    LoggerCentral.warn(TAG, "   Recomendado: vosk-model-small-es-0.42 (50MB)");
                    LoggerCentral.warn(TAG, "   La transcripción automática no estará disponible");
                }
            } else {
                LoggerCentral.info(TAG, "ℹ️ Transcripción Vosk deshabilitada en configuración");
            }

            // 2. Cargar audios existentes desde la BD
            fachadaTranscripcion.cargarAudiosDesdeBaseDatos();

            // 3. Iniciar actualización automática
            int intervalo = config.getVoskActualizacionIntervalo();
            fachadaTranscripcion.iniciarActualizacionAutomatica(intervalo);
            LoggerCentral.info(TAG, "✓ Actualización automática iniciada (cada " + intervalo + " segundos)");

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
     * ✅ NUEVO: Obtiene estadísticas de transcripción
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
     * ✅ NUEVO: Busca un audio por su ID
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
     * Verifica si el servicio de transcripción está disponible
     */
    public boolean isTranscripcionDisponible() {
        return fachadaTranscripcion.isTranscripcionDisponible();
    }
    
    /**
     * ✅ NUEVO: Obtiene el número de transcripciones en cola
     */
    public int getNumeroTranscripcionesPendientes() {
        return fachadaTranscripcion.getNumeroTranscripcionesPendientes();
    }
    
    /**
     * ✅ NUEVO: Transcribe todos los audios pendientes
     */
    public int transcribirTodosPendientes() {
        try {
            LoggerCentral.info(TAG, "⚡ Iniciando transcripción masiva de audios pendientes...");
            return fachadaTranscripcion.transcribirTodosPendientes();
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error en transcripción masiva: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Detiene el servicio de transcripción
     */
    public void detener() {
        try {
            LoggerCentral.info(TAG, "Deteniendo servicio de transcripción...");
            fachadaTranscripcion.detenerActualizacionAutomatica();
            LoggerCentral.info(TAG, "✓ Servicio detenido");
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al detener servicio: " + e.getMessage());
        }
    }
}
