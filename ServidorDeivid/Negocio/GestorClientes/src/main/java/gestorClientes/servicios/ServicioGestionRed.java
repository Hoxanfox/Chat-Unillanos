package gestorClientes.servicios;

import configuracion.Configuracion;
import conexion.clientes.interfaces.IGestorConexionesCliente;
import conexion.clientes.interfaces.IRouterMensajesCliente;
import dto.cliente.DTOSesionCliente;
import gestorClientes.interfaces.IServicioCliente;
import logger.LoggerCentral;
import observador.IObservador;
import observador.ISujeto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class ServicioGestionRed implements IServicioCliente, ISujeto {

    private static final String TAG = "GestionRedCS";

    // --- COLORES ANSI ---
    public static final String RESET = "\u001B[0m";
    public static final String ROJO = "\u001B[31m";
    public static final String VERDE = "\u001B[32m";
    public static final String AMARILLO = "\u001B[33m";
    public static final String AZUL = "\u001B[34m";
    public static final String MAGENTA = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";

    private IGestorConexionesCliente gestorConexiones;
    private final Configuracion config;
    private Timer timerMantenimiento;
    private final List<IObservador> observadores;

    // Pool de sesiones activas (redundante con GestorConexiones pero útil para lógica de negocio)
    private final ConcurrentHashMap<String, DTOSesionCliente> sesionesActivas = new ConcurrentHashMap<>();

    public ServicioGestionRed() {
        this.config = Configuracion.getInstance();
        this.observadores = new ArrayList<>();
    }

    @Override
    public String getNombre() {
        return "ServicioGestionRedCS";
    }

    /**
     * Callback para cuando un cliente se desconecta.
     * Puede ser llamado por el GestorConexiones o detectado internamente.
     */
    public void onClienteDesconectado(String idSesion) {
        LoggerCentral.warn(TAG, ROJO + "🔴 Cliente desconectado: " + idSesion + RESET);

        DTOSesionCliente sesion = sesionesActivas.remove(idSesion);
        if (sesion != null && sesion.getIdUsuario() != null) {
            LoggerCentral.info(TAG, "Usuario " + sesion.getIdUsuario() + " desvinculado de sesión");
            notificarObservadores("CLIENTE_OFFLINE", sesion.getIdUsuario());
        }

        // ✅ NUEVO: Notificar cambio en estadísticas
        notificarObservadores("CLIENTE_DESCONECTADO", idSesion);
    }

    /**
     * Callback para cuando un cliente se conecta.
     */
    public void onClienteConectado(String idSesion, DTOSesionCliente sesion) {
        LoggerCentral.info(TAG, VERDE + "✓ Nuevo cliente conectado: " + idSesion + RESET);
        sesionesActivas.put(idSesion, sesion);
        notificarObservadores("CLIENTE_CONECTADO", idSesion);
    }

    @Override
    public void inicializar(IGestorConexionesCliente gestor, IRouterMensajesCliente router) {
        this.gestorConexiones = gestor;

        LoggerCentral.info(TAG, CYAN + "Inicializando ServicioGestionRed para Cliente-Servidor..." + RESET);

        // ✅ NUEVO: Configurar callbacks para recibir notificaciones
        if (gestor instanceof conexion.clientes.impl.GestorConexionesClienteImpl) {
            conexion.clientes.impl.GestorConexionesClienteImpl gestorImpl =
                (conexion.clientes.impl.GestorConexionesClienteImpl) gestor;

            gestorImpl.setOnClienteConectado(idSesion -> {
                LoggerCentral.info(TAG, VERDE + "✓ Nuevo cliente conectado: " + idSesion + RESET);
                onClienteConectado(idSesion, gestor.obtenerSesion(idSesion));
            });

            gestorImpl.setOnClienteDesconectado(idSesion -> {
                LoggerCentral.warn(TAG, ROJO + "🔴 Cliente desconectado: " + idSesion + RESET);
                onClienteDesconectado(idSesion);
            });

            LoggerCentral.info(TAG, VERDE + "✓ Callbacks de conexión configurados" + RESET);
        }

        // =========================================================================
        // RUTA 1: HEARTBEAT (Verificación de conexión)
        // =========================================================================
        router.registrarAccion("heartbeat", (datosJson, origenId) -> {
            // Cliente envía heartbeat para mantener conexión viva
            LoggerCentral.debug(TAG, "Heartbeat recibido de: " + origenId);

            // Actualizar timestamp de última actividad
            DTOSesionCliente sesion = sesionesActivas.get(origenId);
            if (sesion != null) {
                // Solo verificar que existe, el estado no se modifica aquí
                LoggerCentral.debug(TAG, "Sesión activa confirmada: " + origenId);
            }

            return null; // No necesitamos responder
        });

        // =========================================================================
        // RUTA 2: PING (Para monitoreo de latencia)
        // =========================================================================
        router.registrarAccion("ping", (datosJson, origenId) -> {
            LoggerCentral.debug(TAG, "PING recibido de: " + origenId);
            return null; // El router responderá automáticamente con "pong"
        });

        LoggerCentral.info(TAG, VERDE + "Rutas de gestión de red registradas correctamente" + RESET);
    }

    @Override
    public void iniciar() {
        LoggerCentral.info(TAG, MAGENTA + "═══════════════════════════════════════════" + RESET);
        LoggerCentral.info(TAG, MAGENTA + "    INICIANDO RED CLIENTE-SERVIDOR" + RESET);
        LoggerCentral.info(TAG, MAGENTA + "═══════════════════════════════════════════" + RESET);

        String host = config.getClienteHost();
        int puerto = config.getClientePuerto();

        LoggerCentral.info(TAG, "Configuración de Red:");
        LoggerCentral.info(TAG, "  • Host: " + CYAN + host + RESET);
        LoggerCentral.info(TAG, "  • Puerto: " + CYAN + puerto + RESET);

        // Iniciar servidor para escuchar clientes
        new Thread(() -> {
            try {
                LoggerCentral.info(TAG, AMARILLO + "⚡ Levantando servidor de clientes..." + RESET);
                gestorConexiones.iniciarServidor(puerto);
                LoggerCentral.info(TAG, VERDE + "✓ Servidor de clientes ACTIVO en " + host + ":" + puerto + RESET);
                notificarObservadores("RED_INICIADA", "Puerto " + puerto);
            } catch (Exception e) {
                LoggerCentral.error(TAG, ROJO + "✗ Error al iniciar servidor: " + e.getMessage() + RESET);
                notificarObservadores("RED_ERROR", e.getMessage());
            }
        }, "Thread-ServidorClientes").start();

        // Iniciar tarea de mantenimiento
        iniciarMantenimiento();

        LoggerCentral.info(TAG, MAGENTA + "═══════════════════════════════════════════" + RESET);
        LoggerCentral.info(TAG, VERDE + "    Red Cliente-Servidor LISTA" + RESET);
        LoggerCentral.info(TAG, MAGENTA + "═══════════════════════════════════════════" + RESET);
    }

    /**
     * Inicia tareas periódicas de mantenimiento:
     * - Verificar sesiones inactivas
     * - Limpiar sesiones muertas
     * - Reportar estadísticas
     */
    private void iniciarMantenimiento() {
        if (timerMantenimiento == null) {
            timerMantenimiento = new Timer("Timer-MantenimientoCS", true);

            // Tarea 1: Verificar sesiones cada 30 segundos
            timerMantenimiento.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    verificarSesiones();
                }
            }, 30000, 30000); // 30 segundos

            // Tarea 2: Reportar estadísticas cada 60 segundos
            timerMantenimiento.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    reportarEstadisticas();
                }
            }, 60000, 60000); // 60 segundos

            LoggerCentral.info(TAG, "Tareas de mantenimiento programadas");
        }
    }

    /**
     * Verifica el estado de las sesiones activas.
     * Elimina sesiones muertas y sincroniza con el pool del GestorConexiones.
     */
    private void verificarSesiones() {
        try {
            LoggerCentral.debug(TAG, "Ejecutando verificación de sesiones...");

            // Obtener lista actual del gestor de conexiones
            List<DTOSesionCliente> sesionesReales = gestorConexiones.obtenerClientesConectados();

            // Sincronizar con nuestra cache local
            sesionesActivas.clear();
            for (DTOSesionCliente sesion : sesionesReales) {
                sesionesActivas.put(sesion.getIdSesion(), sesion);
            }

            LoggerCentral.debug(TAG, VERDE + "Sesiones activas: " + sesionesActivas.size() + RESET);

        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error en verificación de sesiones: " + e.getMessage());
        }
    }

    /**
     * Reporta estadísticas del servidor.
     */
    private void reportarEstadisticas() {
        try {
            int totalSesiones = sesionesActivas.size();
            long sesionesAutenticadas = sesionesActivas.values().stream()
                    .filter(s -> s.getIdUsuario() != null)
                    .count();

            LoggerCentral.info(TAG, CYAN + "─────────────────────────────────────" + RESET);
            LoggerCentral.info(TAG, CYAN + "  ESTADÍSTICAS DEL SERVIDOR" + RESET);
            LoggerCentral.info(TAG, CYAN + "─────────────────────────────────────" + RESET);
            LoggerCentral.info(TAG, "  • Sesiones totales: " + AMARILLO + totalSesiones + RESET);
            LoggerCentral.info(TAG, "  • Usuarios autenticados: " + VERDE + sesionesAutenticadas + RESET);
            LoggerCentral.info(TAG, "  • Sesiones anónimas: " + AMARILLO + (totalSesiones - sesionesAutenticadas) + RESET);
            LoggerCentral.info(TAG, CYAN + "─────────────────────────────────────" + RESET);

            notificarObservadores("ESTADISTICAS", Map.of(
                    "totalSesiones", totalSesiones,
                    "usuariosAutenticados", sesionesAutenticadas
            ));

        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error reportando estadísticas: " + e.getMessage());
        }
    }

    /**
     * Obtiene el número de clientes conectados actualmente.
     */
    public int getNumeroClientesConectados() {
        return sesionesActivas.size();
    }

    /**
     * Obtiene la lista de sesiones activas.
     */
    public List<DTOSesionCliente> getSesionesActivas() {
        return new ArrayList<>(sesionesActivas.values());
    }

    @Override
    public void detener() {
        LoggerCentral.info(TAG, AMARILLO + "Deteniendo ServicioGestionRed..." + RESET);

        if (timerMantenimiento != null) {
            timerMantenimiento.cancel();
            timerMantenimiento = null;
        }

        sesionesActivas.clear();

        LoggerCentral.info(TAG, VERDE + "ServicioGestionRed detenido correctamente" + RESET);
        notificarObservadores("RED_DETENIDA", null);
    }

    // --- PATRÓN OBSERVER ---

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
        LoggerCentral.debug(TAG, "Observador removido: " + observador.getClass().getSimpleName());
    }

    @Override
    public void notificarObservadores(String tipo, Object datos) {
        for (IObservador observador : observadores) {
            try {
                observador.actualizar(tipo, datos);
            } catch (Exception e) {
                LoggerCentral.error(TAG, "Error notificando observador: " + e.getMessage());
            }
        }
    }
}
