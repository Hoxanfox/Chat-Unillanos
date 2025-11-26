package controlador.clienteServidor;

import dto.cliente.DTOSesionCliente;
import logger.LoggerCentral;
import observador.IObservador;
import servicio.clienteServidor.IServicioClienteControl;
import servicio.clienteServidor.ServicioCliente;

import java.util.List;
import java.util.function.Consumer;

/**
 * Controlador para la red Cliente-Servidor.
 * Expone métodos simples para que la vista/interfaz gráfica pueda controlar el servidor.
 *
 * PUEDE usar componentes transversales:
 * - Servicio (IServicioClienteControl)
 * - Observer (patrón de diseño)
 * - Logger (infraestructura de logging)
 * - DTOs (objetos de transferencia de datos)
 */
public class ControladorClienteServidor implements IObservador {

    private static final String TAG = "ControladorCS";
    private final IServicioClienteControl servicio;

    // Callbacks para notificar a la vista
    private Consumer<String> onRedIniciada;
    private Consumer<String> onRedDetenida;
    private Consumer<String> onClienteConectado;
    private Consumer<String> onClienteDesconectado;
    private Consumer<EstadisticasServidor> onEstadisticasActualizadas;

    public ControladorClienteServidor() {
        LoggerCentral.debug(TAG, "Creando instancia de ControladorClienteServidor...");
        this.servicio = new ServicioCliente();
        LoggerCentral.info(TAG, "ControladorClienteServidor inicializado correctamente.");
    }

    public ControladorClienteServidor(IServicioClienteControl servicio) {
        LoggerCentral.debug(TAG, "Creando ControladorCS con servicio inyectado...");
        this.servicio = servicio;
        LoggerCentral.info(TAG, "ControladorCS inicializado con inyección de dependencias.");
    }

    // =========================================================================
    // MÉTODOS DE CONTROL PRINCIPAL
    // =========================================================================

    /**
     * ✅ NUEVO: Expone el servicio Cliente-Servidor interno para configuración avanzada.
     * Útil para conectar con otros servicios (ej: P2P para topología).
     */
    public IServicioClienteControl getServicioClienteInterno() {
        return servicio;
    }

    /**
     * Inicia el servidor de clientes en el puerto especificado.
     * @param puerto Puerto de escucha (ej. 8000).
     */
    public void iniciarServidor(int puerto) {
        LoggerCentral.info(TAG, "Iniciando servidor de clientes en puerto " + puerto + "...");
        try {
            servicio.iniciarServidor(puerto);

            // Suscribirse a eventos usando el patrón Observer
            servicio.registrarObservador(this);

            LoggerCentral.info(TAG, "✓ Servidor de clientes iniciado correctamente");

            if (onRedIniciada != null) {
                onRedIniciada.accept("Puerto " + puerto);
            }
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al iniciar servidor: " + e.getMessage());
            throw new RuntimeException("No se pudo iniciar el servidor: " + e.getMessage());
        }
    }

    /**
     * Inicia el servidor usando el puerto configurado en configuracion.txt
     */
    public void iniciarServidorAutomatico() {
        LoggerCentral.info(TAG, "Iniciando servidor con configuración automática...");
        iniciarServidor(8000); // Puerto por defecto
    }

    /**
     * Detiene el servidor y desconecta todos los clientes.
     */
    public void detenerServidor() {
        LoggerCentral.info(TAG, "Deteniendo servidor de clientes...");
        servicio.detenerServidor();
        LoggerCentral.info(TAG, "✓ Servidor de clientes detenido");

        if (onRedDetenida != null) {
            onRedDetenida.accept("Servidor detenido");
        }
    }

    /**
     * Verifica si el servidor está corriendo.
     */
    public boolean isServidorActivo() {
        return servicio.estaCorriendo();
    }

    // =========================================================================
    // MÉTODOS DE CONSULTA CON DTOs TRANSVERSALES
    // =========================================================================

    /**
     * Obtiene el número de clientes conectados actualmente.
     */
    public int getNumeroClientesConectados() {
        int total = servicio.getNumeroClientesConectados();
        LoggerCentral.debug(TAG, "Clientes conectados: " + total);
        return total;
    }

    /**
     * Obtiene la lista de sesiones activas (DTOs transversales).
     */
    public List<DTOSesionCliente> getSesionesActivas() {
        List<DTOSesionCliente> sesiones = servicio.getSesionesActivas();
        LoggerCentral.debug(TAG, "Sesiones activas obtenidas: " + sesiones.size());
        return sesiones;
    }

    /**
     * Obtiene estadísticas completas del servidor.
     */
    public EstadisticasServidor getEstadisticas() {
        List<DTOSesionCliente> sesiones = getSesionesActivas();
        long usuariosAutenticados = sesiones.stream()
                .filter(DTOSesionCliente::estaAutenticado)
                .count();

        EstadisticasServidor stats = new EstadisticasServidor(
                sesiones.size(),
                (int) usuariosAutenticados,
                sesiones.size() - (int) usuariosAutenticados,
                isServidorActivo()
        );

        LoggerCentral.debug(TAG, "Estadísticas generadas: " + stats);
        return stats;
    }

    /**
     * Obtiene información resumida del servidor en formato texto.
     */
    public String getEstadoServidor() {
        if (!servicio.estaCorriendo()) {
            return "Estado: DETENIDO | Clientes: 0";
        }

        int clientes = getNumeroClientesConectados();
        return String.format("Estado: ACTIVO | Clientes: %d", clientes);
    }

    // =========================================================================
    // PATRÓN OBSERVER (componente transversal)
    // =========================================================================

    @Override
    public void actualizar(String tipo, Object datos) {
        LoggerCentral.debug(TAG, "Evento recibido: " + tipo + " | Datos: " + datos);

        switch (tipo) {
            case "RED_INICIADA":
                LoggerCentral.info(TAG, "✓ Red CS iniciada: " + datos);
                if (onRedIniciada != null && datos != null) {
                    onRedIniciada.accept(datos.toString());
                }
                break;

            case "RED_DETENIDA":
                LoggerCentral.info(TAG, "✓ Red CS detenida");
                if (onRedDetenida != null) {
                    onRedDetenida.accept("Red detenida");
                }
                break;

            case "CLIENTE_CONECTADO":
                LoggerCentral.info(TAG, "✓ Nuevo cliente conectado: " + datos);
                if (onClienteConectado != null && datos != null) {
                    onClienteConectado.accept(datos.toString());
                }
                break;

            case "CLIENTE_OFFLINE":
                LoggerCentral.info(TAG, "✓ Cliente desconectado: " + datos);
                if (onClienteDesconectado != null && datos != null) {
                    onClienteDesconectado.accept(datos.toString());
                }
                break;

            case "ESTADISTICAS":
                LoggerCentral.debug(TAG, "Estadísticas actualizadas");
                if (onEstadisticasActualizadas != null) {
                    onEstadisticasActualizadas.accept(getEstadisticas());
                }
                break;

            case "RED_ERROR":
                LoggerCentral.error(TAG, "✗ Error en la red: " + datos);
                break;

            default:
                LoggerCentral.debug(TAG, "Evento no manejado: " + tipo);
        }
    }

    // =========================================================================
    // SETTERS PARA CALLBACKS (Para la vista)
    // =========================================================================

    public void setOnRedIniciada(Consumer<String> callback) {
        this.onRedIniciada = callback;
    }

    public void setOnRedDetenida(Consumer<String> callback) {
        this.onRedDetenida = callback;
    }

    public void setOnClienteConectado(Consumer<String> callback) {
        this.onClienteConectado = callback;
    }

    public void setOnClienteDesconectado(Consumer<String> callback) {
        this.onClienteDesconectado = callback;
    }

    public void setOnEstadisticasActualizadas(Consumer<EstadisticasServidor> callback) {
        this.onEstadisticasActualizadas = callback;
    }

    // =========================================================================
    // CLASE INTERNA: ESTADÍSTICAS DEL SERVIDOR (DTO transversal)
    // =========================================================================

    public static class EstadisticasServidor {
        private final int totalSesiones;
        private final int usuariosAutenticados;
        private final int sesionesAnonimas;
        private final boolean servidorActivo;

        public EstadisticasServidor(int totalSesiones, int usuariosAutenticados,
                                    int sesionesAnonimas, boolean servidorActivo) {
            this.totalSesiones = totalSesiones;
            this.usuariosAutenticados = usuariosAutenticados;
            this.sesionesAnonimas = sesionesAnonimas;
            this.servidorActivo = servidorActivo;
        }

        public int getTotalSesiones() {
            return totalSesiones;
        }

        public int getUsuariosAutenticados() {
            return usuariosAutenticados;
        }

        public int getSesionesAnonimas() {
            return sesionesAnonimas;
        }

        public boolean isServidorActivo() {
            return servidorActivo;
        }

        @Override
        public String toString() {
            return String.format("Estadísticas[Total: %d, Autenticados: %d, Anónimos: %d, Activo: %s]",
                    totalSesiones, usuariosAutenticados, sesionesAnonimas, servidorActivo);
        }
    }
}
