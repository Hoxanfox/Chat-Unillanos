package servicio.logs;

import dto.logs.DTOLog;
import gestorLogs.GestorLogs;
import gestorLogs.api.LogsApiApplication;
import logger.LoggerCentral;

import java.io.IOException;
import java.util.List;

/**
 * Servicio de Logs - Capa de Aplicación
 * Maneja la lógica de aplicación y orquestación para logs
 * Actúa como intermediario entre el controlador y el gestor de logs
 */
public class ServicioLogs {

    private static final String TAG = "ServicioLogs";
    private final GestorLogs gestor;

    public ServicioLogs(GestorLogs gestor) {
        this.gestor = gestor;
        LoggerCentral.info(TAG, "ServicioLogs inicializado");
    }

    /**
     * Obtiene los logs recientes en memoria
     */
    public List<DTOLog> obtenerLogsRecientes(int cantidad) {
        try {
            List<DTOLog> logs = gestor.obtenerLogsRecientes(cantidad);
            LoggerCentral.debug(TAG, "Logs recientes obtenidos: " + logs.size());
            return logs;
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al obtener logs recientes: " + e.getMessage());
            throw new RuntimeException("Error al obtener logs recientes", e);
        }
    }

    /**
     * Obtiene todos los logs en memoria
     */
    public List<DTOLog> obtenerTodosLosLogs() {
        try {
            List<DTOLog> logs = gestor.obtenerTodosLosLogsEnMemoria();
            LoggerCentral.debug(TAG, "Todos los logs obtenidos: " + logs.size());
            return logs;
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al obtener todos los logs: " + e.getMessage());
            throw new RuntimeException("Error al obtener todos los logs", e);
        }
    }

    /**
     * Lee logs desde el archivo físico
     */
    public List<DTOLog> leerLogsDesdeArchivo(int maxLineas) {
        try {
            List<DTOLog> logs = gestor.leerLogsDesdeArchivo(maxLineas);
            LoggerCentral.info(TAG, "Logs leídos desde archivo: " + logs.size());
            return logs;
        } catch (IOException e) {
            LoggerCentral.error(TAG, "Error al leer logs desde archivo: " + e.getMessage());
            throw new RuntimeException("Error al leer logs desde archivo", e);
        }
    }

    /**
     * Filtra logs por nivel (INFO, WARNING, ERROR, DEBUG)
     */
    public List<DTOLog> filtrarPorNivel(String nivel) {
        try {
            if (nivel == null || nivel.trim().isEmpty()) {
                throw new IllegalArgumentException("El nivel no puede estar vacío");
            }

            List<DTOLog> logs = gestor.filtrarPorNivel(nivel.toUpperCase());
            LoggerCentral.debug(TAG, "Logs filtrados por nivel " + nivel + ": " + logs.size());
            return logs;
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al filtrar logs por nivel: " + e.getMessage());
            throw new RuntimeException("Error al filtrar logs por nivel", e);
        }
    }

    /**
     * Filtra logs por source/tag
     */
    public List<DTOLog> filtrarPorSource(String source) {
        try {
            if (source == null || source.trim().isEmpty()) {
                throw new IllegalArgumentException("El source no puede estar vacío");
            }

            List<DTOLog> logs = gestor.filtrarPorSource(source);
            LoggerCentral.debug(TAG, "Logs filtrados por source " + source + ": " + logs.size());
            return logs;
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al filtrar logs por source: " + e.getMessage());
            throw new RuntimeException("Error al filtrar logs por source", e);
        }
    }

    /**
     * Busca logs que contengan un texto específico
     */
    public List<DTOLog> buscarPorTexto(String texto) {
        try {
            if (texto == null || texto.trim().isEmpty()) {
                throw new IllegalArgumentException("El texto de búsqueda no puede estar vacío");
            }

            List<DTOLog> logs = gestor.buscarPorTexto(texto);
            LoggerCentral.debug(TAG, "Logs encontrados con texto '" + texto + "': " + logs.size());
            return logs;
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al buscar logs por texto: " + e.getMessage());
            throw new RuntimeException("Error al buscar logs por texto", e);
        }
    }

    /**
     * Limpia los logs en memoria
     */
    public void limpiarLogs() {
        try {
            gestor.limpiarLogsEnMemoria();
            LoggerCentral.info(TAG, "Logs limpiados exitosamente");
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al limpiar logs: " + e.getMessage());
            throw new RuntimeException("Error al limpiar logs", e);
        }
    }

    /**
     * Obtiene estadísticas de los logs
     */
    public GestorLogs.EstadisticasLogs obtenerEstadisticas() {
        try {
            GestorLogs.EstadisticasLogs stats = gestor.obtenerEstadisticas();
            LoggerCentral.debug(TAG, "Estadísticas obtenidas: " + stats.toString());
            return stats;
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al obtener estadísticas: " + e.getMessage());
            throw new RuntimeException("Error al obtener estadísticas", e);
        }
    }

    /**
     * Obtiene el gestor de logs para operaciones avanzadas
     */
    public GestorLogs obtenerGestor() {
        return gestor;
    }

    // --- MÉTODOS PARA GESTIONAR EL API REST ---

    /**
     * Inicia el servidor REST API en el puerto especificado
     */
    public boolean iniciarApiRest(int puerto) {
        try {
            if (LogsApiApplication.estaActivo()) {
                LoggerCentral.warn(TAG, "API REST ya está en ejecución");
                return false;
            }

            LoggerCentral.info(TAG, "========================================");
            LoggerCentral.info(TAG, "Preparando inicio de Logs REST API...");
            LoggerCentral.info(TAG, "Puerto configurado: " + puerto);
            LoggerCentral.info(TAG, "GestorLogs actual: " + (gestor != null ? "OK" : "NULL"));

            if (gestor != null) {
                LoggerCentral.info(TAG, "Logs en memoria: " + gestor.obtenerTodosLosLogsEnMemoria().size());
            }

            // 🔧 IMPORTANTE: Configurar el GestorLogs compartido ANTES de iniciar Spring Boot
            LoggerCentral.info(TAG, "Configurando GestorLogs compartido...");
            gestorLogs.api.LogsApiConfig.setGestorLogs(gestor);
            LoggerCentral.info(TAG, "✓ GestorLogs compartido configurado");

            LoggerCentral.info(TAG, "Iniciando Spring Boot...");
            LogsApiApplication.iniciar(puerto);

            // Esperar un momento para que el servidor inicie
            LoggerCentral.info(TAG, "Esperando a que Spring Boot arranque...");
            Thread.sleep(3000); // Aumentado a 3 segundos

            boolean activo = LogsApiApplication.estaActivo();
            LoggerCentral.info(TAG, "Estado del servidor después de espera: " + (activo ? "ACTIVO" : "INACTIVO"));

            if (activo) {
                LoggerCentral.info(TAG, "✓ Logs REST API iniciado exitosamente en puerto " + puerto);
                LoggerCentral.info(TAG, "✓ Endpoints disponibles en: http://localhost:" + puerto + "/api/logs");
                LoggerCentral.info(TAG, "========================================");
                return true;
            } else {
                LoggerCentral.error(TAG, "✗ Error: API REST no se inició correctamente");
                LoggerCentral.error(TAG, "Revisa los logs de Spring Boot arriba para más detalles");
                LoggerCentral.info(TAG, "========================================");
                return false;
            }

        } catch (Exception e) {
            LoggerCentral.error(TAG, "✗ ERROR al iniciar API REST: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Inicia el servidor REST API en el puerto por defecto (8080)
     */
    public boolean iniciarApiRest() {
        return iniciarApiRest(8080);
    }

    /**
     * Detiene el servidor REST API
     */
    public void detenerApiRest() {
        try {
            if (LogsApiApplication.estaActivo()) {
                LoggerCentral.info(TAG, "Deteniendo Logs REST API...");
                LogsApiApplication.detener();
                LoggerCentral.info(TAG, "✓ Logs REST API detenido");
            } else {
                LoggerCentral.warn(TAG, "API REST no está en ejecución");
            }
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al detener API REST: " + e.getMessage());
            throw new RuntimeException("Error al detener API REST", e);
        }
    }

    /**
     * Verifica si el servidor REST API está activo
     */
    public boolean apiRestEstaActivo() {
        return LogsApiApplication.estaActivo();
    }
}
