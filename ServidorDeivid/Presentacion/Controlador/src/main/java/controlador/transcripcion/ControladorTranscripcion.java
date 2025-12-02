package controlador.transcripcion;

import dto.transcripcion.DTOAudioTranscripcion;
import gestorTranscripcion.FachadaTranscripcion;
import servicio.transcripcion.ServicioTranscripcionNegocio;
import logger.LoggerCentral;
import observador.IObservador;

import java.util.ArrayList;
import java.util.List;

/**
 * Controlador para la gestión de transcripciones desde la interfaz gráfica
 * Coordina las acciones entre la vista y el servicio de negocio
 * Pertenece a la capa de Presentación
 */
public class ControladorTranscripcion {

    private static final String TAG = "ControladorTranscripcion";
    private final List<IObservador> observadores;
    private final ServicioTranscripcionNegocio servicioTranscripcion;

    public ControladorTranscripcion() {
        this.observadores = new ArrayList<>();
        this.servicioTranscripcion = ServicioTranscripcionNegocio.getInstance();

        // Inicializar el sistema de transcripción
        servicioTranscripcion.inicializar();

        LoggerCentral.info(TAG, "✓ ControladorTranscripcion inicializado");
    }

    /**
     * Suscribe un observador para notificaciones
     */
    public void suscribirObservador(IObservador observador) {
        if (observador != null && !observadores.contains(observador)) {
            observadores.add(observador);
            LoggerCentral.debug(TAG, "Observador suscrito");
        }
    }

    /**
     * Desuscribe un observador
     */
    public void desuscribirObservador(IObservador observador) {
        observadores.remove(observador);
        LoggerCentral.debug(TAG, "Observador desuscrito");
    }

    /**
     * Notifica a todos los observadores
     */
    private void notificarObservadores(String tipoDeDato, Object datos) {
        for (IObservador observador : observadores) {
            observador.actualizar(tipoDeDato, datos);
        }
    }

    /**
     * ✅ NUEVO: Suscribe un observador directamente a la FachadaTranscripcion
     * para recibir eventos en tiempo real
     */
    public void suscribirAFachadaTranscripcion(IObservador observador) {
        try {
            FachadaTranscripcion fachada = FachadaTranscripcion.getInstance();
            fachada.registrarObservador(observador);
            LoggerCentral.info(TAG, "✓ Observador suscrito a FachadaTranscripcion");
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al suscribir a FachadaTranscripcion: " + e.getMessage());
        }
    }

    /**
     * Obtiene todos los audios disponibles
     */
    public List<DTOAudioTranscripcion> obtenerAudios() {
        try {
            LoggerCentral.debug(TAG, "Obteniendo lista de audios");
            return servicioTranscripcion.obtenerAudios();
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
            LoggerCentral.debug(TAG, "Filtrando audios por tipo: " + tipo);
            return servicioTranscripcion.filtrarPorTipo(tipo);
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al filtrar por tipo: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Busca audios por texto
     */
    public List<DTOAudioTranscripcion> buscarAudios(String textoBusqueda) {
        try {
            LoggerCentral.debug(TAG, "Buscando audios: " + textoBusqueda);
            return servicioTranscripcion.buscarAudios(textoBusqueda);
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al buscar audios: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Transcribe un audio
     */
    public boolean transcribirAudio(String audioId, String transcripcion) {
        try {
            LoggerCentral.info(TAG, "Transcribiendo audio: " + audioId);
            return servicioTranscripcion.transcribirAudio(audioId, transcripcion);
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al transcribir: " + e.getMessage());
            return false;
        }
    }

    /**
     * ✅ NUEVO: Inicia la transcripción automática de un audio usando Vosk
     */
    public boolean iniciarTranscripcionAutomatica(String audioId) {
        try {
            LoggerCentral.info(TAG, "🎤 Iniciando transcripción automática para: " + audioId);
            return servicioTranscripcion.iniciarTranscripcionAutomatica(audioId);
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al iniciar transcripción automática: " + e.getMessage());
            return false;
        }
    }

    /**
     * ✅ NUEVO: Carga los audios desde la base de datos
     */
    public void cargarAudiosDesdeBaseDatos() {
        try {
            LoggerCentral.info(TAG, "🔄 Cargando audios desde la base de datos...");
            servicioTranscripcion.recargarAudios();
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al cargar audios: " + e.getMessage());
        }
    }

    /**
     * ✅ NUEVO: Actualiza la tabla de audios manualmente
     */
    public void actualizarTablaAudios() {
        try {
            LoggerCentral.info(TAG, "🔄 Actualizando tabla de audios...");
            servicioTranscripcion.actualizarTablaAudios();
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al actualizar tabla: " + e.getMessage());
        }
    }

    /**
     * ✅ NUEVO: Obtiene estadísticas de transcripción
     */
    public FachadaTranscripcion.EstadisticasTranscripcion obtenerEstadisticas() {
        return servicioTranscripcion.obtenerEstadisticas();
    }
    
    /**
     * ✅ NUEVO: Verifica si el servicio de transcripción está disponible (modelo Vosk cargado)
     */
    public boolean isTranscripcionDisponible() {
        try {
            return servicioTranscripcion.isTranscripcionDisponible();
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al verificar disponibilidad: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * ✅ NUEVO: Obtiene el número de transcripciones en cola
     */
    public int getNumeroTranscripcionesPendientes() {
        try {
            return servicioTranscripcion.getNumeroTranscripcionesPendientes();
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al obtener pendientes: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * ✅ NUEVO: Transcribe todos los audios pendientes
     */
    public int transcribirTodosPendientes() {
        try {
            LoggerCentral.info(TAG, "⚡ Iniciando transcripción de todos los audios pendientes...");
            return servicioTranscripcion.transcribirTodosPendientes();
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error en transcripción masiva: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Obtiene un audio por su ID
     */
    public DTOAudioTranscripcion obtenerAudioPorId(String audioId) {
        try {
            LoggerCentral.debug(TAG, "Obteniendo audio por ID: " + audioId);
            return servicioTranscripcion.buscarAudioPorId(audioId);
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al obtener audio: " + e.getMessage());
            return null;
        }
    }

    /**
     * Detiene el servicio de transcripción y libera recursos.
     */
    public void detenerServicio() {
        try {
            LoggerCentral.info(TAG, "Deteniendo servicio de transcripción...");
            servicioTranscripcion.detener();
            LoggerCentral.info(TAG, "✓ Servicio de transcripción detenido");
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al detener servicio: " + e.getMessage());
        }
    }
}
