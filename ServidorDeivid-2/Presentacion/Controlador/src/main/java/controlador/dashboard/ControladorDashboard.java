package controlador.dashboard;

import configuracion.Configuracion;
import dto.dashboard.DTORecursosSistema;
import logger.LoggerCentral;
import observador.IObservador;
import servicio.dashboard.IServicioDashboardControl;
import servicio.dashboard.ServicioDashboard;

/**
 * Controlador del Dashboard del sistema
 * Intermediario entre la vista y el servicio de dashboard
 * Gestiona las actualizaciones de estadísticas del sistema
 * ✅ NO DEPENDE DE GESTORES, solo de servicios
 */
public class ControladorDashboard implements IObservador {

    private static final String TAG = "ControladorDashboard";
    private final IServicioDashboardControl servicio;
    private final Configuracion config;

    // Callbacks para notificar a la vista
    private java.util.function.Consumer<String> onInfoServidorActualizada;
    private java.util.function.Consumer<String> onEstadoRedActualizado;
    private java.util.function.Consumer<String> onRecursosActualizados;
    private java.util.function.Consumer<String> onActividadRegistrada;

    public ControladorDashboard() {
        LoggerCentral.debug(TAG, "Creando instancia de ControladorDashboard...");
        this.servicio = new ServicioDashboard();
        this.config = Configuracion.getInstance();
        LoggerCentral.info(TAG, "ControladorDashboard inicializado correctamente.");
    }

    public ControladorDashboard(IServicioDashboardControl servicio) {
        LoggerCentral.debug(TAG, "Creando ControladorDashboard con servicio inyectado...");
        this.servicio = servicio;
        this.config = Configuracion.getInstance();
        LoggerCentral.info(TAG, "ControladorDashboard inicializado con inyección de dependencias.");
    }

    // === MÉTODOS DE CONTROL ===

    /**
     * Inicia el monitoreo del dashboard
     */
    public void iniciarMonitoreo() {
        LoggerCentral.info(TAG, "Iniciando monitoreo del dashboard...");
        servicio.iniciar();
        LoggerCentral.info(TAG, "Monitoreo del dashboard iniciado.");
    }

    /**
     * Detiene el monitoreo del dashboard
     */
    public void detenerMonitoreo() {
        LoggerCentral.info(TAG, "Deteniendo monitoreo del dashboard...");
        servicio.detener();
        LoggerCentral.info(TAG, "Monitoreo del dashboard detenido.");
    }

    /**
     * Actualiza el número de usuarios activos
     */
    public void actualizarUsuariosActivos(int cantidad) {
        LoggerCentral.debug(TAG, "Actualizando usuarios activos: " + cantidad);
        servicio.actualizarUsuariosActivos(cantidad);
    }

    /**
     * Actualiza el número de conexiones P2P
     */
    public void actualizarConexionesP2P(int cantidad) {
        LoggerCentral.debug(TAG, "Actualizando conexiones P2P: " + cantidad);
        servicio.actualizarConexionesP2P(cantidad);
    }

    /**
     * Actualiza el número de conexiones de clientes
     */
    public void actualizarConexionesClientes(int cantidad) {
        LoggerCentral.debug(TAG, "Actualizando conexiones de clientes: " + cantidad);
        servicio.actualizarConexionesClientes(cantidad);
    }

    /**
     * Registra una actividad en el sistema
     */
    public void registrarActividad(String actividad) {
        LoggerCentral.debug(TAG, "Registrando actividad: " + actividad);
        servicio.registrarActividad(actividad);
    }

    /**
     * Obtiene información del servidor formateada
     */
    public String obtenerInfoServidor() {
        int usuarios = servicio.obtenerUsuariosActivos();
        int puertoP2P = config.getPeerPuerto();
        int puertoCS = config.getClientePuerto();

        String info = "Estado: ACTIVO\n" +
                "Usuarios conectados: " + usuarios + "\n" +
                "Tiempo de actividad: Activo\n" +
                "Puerto P2P: " + puertoP2P + "\n" +
                "Puerto CS: " + puertoCS + "\n";

        return info;
    }

    /**
     * Obtiene el estado de la red formateado
     */
    public String obtenerEstadoRed() {
        int conexionesP2P = servicio.obtenerConexionesP2P();
        int conexionesClientes = servicio.obtenerConexionesClientes();

        String estado = "Conexiones P2P: " + conexionesP2P + "\n" +
                "Conexiones Clientes: " + conexionesClientes + "\n" +
                "Total conexiones: " + (conexionesP2P + conexionesClientes) + "\n" +
                "Estado: " + ((conexionesP2P > 0 || conexionesClientes > 0) ? "ACTIVO" : "INACTIVO") + "\n";

        return estado;
    }

    /**
     * Obtiene los recursos del sistema formateados
     */
    public String obtenerRecursos() {
        DTORecursosSistema recursos = servicio.obtenerRecursos();

        long memoriaMB = recursos.getMemoriaUsada() / (1024 * 1024);
        long memoriaTotal = recursos.getMemoriaTotal() / (1024 * 1024);
        double porcentajeMemoria = recursos.getPorcentajeMemoria();

        String info = String.format("Memoria: %d MB / %d MB (%.1f%%)\n", memoriaMB, memoriaTotal, porcentajeMemoria) +
                String.format("CPU: %.1f%%\n", recursos.getCpuUsage()) +
                "Threads: " + Thread.activeCount() + "\n";

        return info;
    }

    /**
     * Verifica si el monitoreo está activo
     */
    public boolean estaActivo() {
        return servicio.estaActivo();
    }

    // === SUSCRIPCIÓN DE OBSERVADORES ===

    /**
     * Registra el controlador como observador del servicio
     */
    public void suscribirseAEventos() {
        LoggerCentral.info(TAG, "Suscribiendo ControladorDashboard a eventos del servicio...");
        servicio.registrarObservador(this);
        LoggerCentral.info(TAG, "✅ ControladorDashboard suscrito a eventos del servicio.");
    }

    /**
     * Permite que la vista se suscriba para recibir actualizaciones de información del servidor
     */
    public void suscribirInfoServidor(java.util.function.Consumer<String> callback) {
        LoggerCentral.debug(TAG, "Vista suscrita a actualizaciones de información del servidor.");
        this.onInfoServidorActualizada = callback;
    }

    /**
     * Permite que la vista se suscriba para recibir actualizaciones de estado de red
     */
    public void suscribirEstadoRed(java.util.function.Consumer<String> callback) {
        LoggerCentral.debug(TAG, "Vista suscrita a actualizaciones de estado de red.");
        this.onEstadoRedActualizado = callback;
    }

    /**
     * Permite que la vista se suscriba para recibir actualizaciones de recursos
     */
    public void suscribirRecursos(java.util.function.Consumer<String> callback) {
        LoggerCentral.debug(TAG, "Vista suscrita a actualizaciones de recursos.");
        this.onRecursosActualizados = callback;
    }

    /**
     * Permite que la vista se suscriba para recibir notificaciones de actividades
     */
    public void suscribirActividades(java.util.function.Consumer<String> callback) {
        LoggerCentral.debug(TAG, "Vista suscrita a notificaciones de actividades.");
        this.onActividadRegistrada = callback;
    }

    // === IMPLEMENTACIÓN DEL PATRÓN OBSERVER ===

    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        LoggerCentral.debug(TAG, "Evento recibido del patrón Observer: [" + tipoDeDato + "]");

        switch (tipoDeDato) {
            case "USUARIOS_ACTUALIZADOS":
            case "CONEXIONES_P2P_ACTUALIZADAS":
            case "CONEXIONES_CLIENTES_ACTUALIZADAS":
                // Actualizar información del servidor y red
                if (onInfoServidorActualizada != null) {
                    onInfoServidorActualizada.accept(obtenerInfoServidor());
                }
                if (onEstadoRedActualizado != null) {
                    onEstadoRedActualizado.accept(obtenerEstadoRed());
                }
                break;

            case "RECURSOS_ACTUALIZADOS":
                // Actualizar recursos del sistema
                if (onRecursosActualizados != null && datos instanceof DTORecursosSistema) {
                    onRecursosActualizados.accept(obtenerRecursos());
                }
                break;

            case "ACTIVIDAD_REGISTRADA":
                // Notificar nueva actividad
                if (onActividadRegistrada != null && datos instanceof String) {
                    String actividad = (String) datos;
                    onActividadRegistrada.accept(actividad);
                }
                break;

            default:
                LoggerCentral.debug(TAG, "Evento no manejado: " + tipoDeDato);
        }
    }
}
