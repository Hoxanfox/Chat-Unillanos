package gestorDashboard;

import dto.dashboard.DTORecursosSistema;
import logger.LoggerCentral;
import observador.ISujeto;
import observador.IObservador;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Gestor del Dashboard del sistema
 * Recopila información de usuarios activos, conexiones y recursos del sistema
 * Utiliza el patrón Observer para notificar cambios a la interfaz
 */
public class GestorDashboard implements ISujeto {

    private static final String TAG = "GestorDashboard";

    private final List<IObservador> observadores;
    private ScheduledExecutorService scheduler;

    // Estadísticas del sistema
    private int usuariosActivos;
    private int conexionesP2P;
    private int conexionesClientes;
    private long memoriaUsada;
    private long memoriaTotal;
    private double cpuUsage;

    private boolean activo;

    public GestorDashboard() {
        this.observadores = new CopyOnWriteArrayList<>();
        this.activo = false;
        LoggerCentral.info(TAG, "GestorDashboard inicializado");
    }

    /**
     * Inicia el monitoreo periódico del sistema
     */
    public void iniciar() {
        if (activo) {
            LoggerCentral.warn(TAG, "GestorDashboard ya está activo");
            return;
        }

        activo = true;
        scheduler = Executors.newScheduledThreadPool(1);

        // Actualizar estadísticas cada 3 segundos
        scheduler.scheduleAtFixedRate(this::actualizarEstadisticas, 0, 3, TimeUnit.SECONDS);

        LoggerCentral.info(TAG, "✅ GestorDashboard iniciado - Monitoreo activo");
    }

    /**
     * Detiene el monitoreo del sistema
     */
    public void detener() {
        if (!activo) {
            return;
        }

        activo = false;
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        LoggerCentral.info(TAG, "GestorDashboard detenido");
    }

    /**
     * Actualiza las estadísticas del sistema y notifica a los observadores
     */
    private void actualizarEstadisticas() {
        try {
            // Obtener información de memoria
            Runtime runtime = Runtime.getRuntime();
            memoriaTotal = runtime.totalMemory();
            memoriaUsada = memoriaTotal - runtime.freeMemory();

            // Calcular uso de CPU (aproximado)
            cpuUsage = calcularUsoCPU();

            // Notificar cambios
            notificarObservadores("RECURSOS_ACTUALIZADOS", crearDatosRecursos());

            LoggerCentral.debug(TAG, String.format("Estadísticas actualizadas - Usuarios: %d, P2P: %d, Clientes: %d",
                    usuariosActivos, conexionesP2P, conexionesClientes));

        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error actualizando estadísticas: " + e.getMessage());
        }
    }

    /**
     * Calcula el uso aproximado de CPU
     */
    private double calcularUsoCPU() {
        // Implementación simplificada
        // En producción, usar OperatingSystemMXBean para datos precisos
        return Math.random() * 100; // Placeholder
    }

    /**
     * Crea un objeto con los datos de recursos del sistema
     */
    private DTORecursosSistema crearDatosRecursos() {
        return new DTORecursosSistema(memoriaUsada, memoriaTotal, cpuUsage);
    }

    /**
     * Actualiza el número de usuarios activos
     */
    public void actualizarUsuariosActivos(int cantidad) {
        this.usuariosActivos = cantidad;
        notificarObservadores("USUARIOS_ACTUALIZADOS", cantidad);
        LoggerCentral.debug(TAG, "Usuarios activos actualizados: " + cantidad);
    }

    /**
     * Actualiza el número de conexiones P2P
     */
    public void actualizarConexionesP2P(int cantidad) {
        this.conexionesP2P = cantidad;
        notificarObservadores("CONEXIONES_P2P_ACTUALIZADAS", cantidad);
        LoggerCentral.debug(TAG, "Conexiones P2P actualizadas: " + cantidad);
    }

    /**
     * Actualiza el número de conexiones de clientes
     */
    public void actualizarConexionesClientes(int cantidad) {
        this.conexionesClientes = cantidad;
        notificarObservadores("CONEXIONES_CLIENTES_ACTUALIZADAS", cantidad);
        LoggerCentral.debug(TAG, "Conexiones clientes actualizadas: " + cantidad);
    }

    /**
     * Registra una actividad reciente en el sistema
     */
    public void registrarActividad(String actividad) {
        notificarObservadores("ACTIVIDAD_REGISTRADA", actividad);
        LoggerCentral.info(TAG, "Actividad: " + actividad);
    }

    // === Getters ===

    public int getUsuariosActivos() {
        return usuariosActivos;
    }

    public int getConexionesP2P() {
        return conexionesP2P;
    }

    public int getConexionesClientes() {
        return conexionesClientes;
    }

    public DTORecursosSistema getRecursos() {
        return crearDatosRecursos();
    }

    public boolean estaActivo() {
        return activo;
    }

    // === Implementación ISujeto ===

    @Override
    public void registrarObservador(IObservador observador) {
        if (!observadores.contains(observador)) {
            observadores.add(observador);
            LoggerCentral.debug(TAG, "Observador registrado: " + observador.getClass().getSimpleName());
        }
    }

    @Override
    public void removerObservador(IObservador observador) {
        observadores.remove(observador);
        LoggerCentral.debug(TAG, "Observador eliminado: " + observador.getClass().getSimpleName());
    }

    @Override
    public void notificarObservadores(String tipoDeDato, Object datos) {
        for (IObservador observador : observadores) {
            try {
                observador.actualizar(tipoDeDato, datos);
            } catch (Exception e) {
                LoggerCentral.error(TAG, "Error notificando observador: " + e.getMessage());
            }
        }
    }
}
