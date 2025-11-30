package controlador.logs;

import logger.LoggerCentral;
import servicio.logs.ServicioLogs;

/**
 * Controlador para el API REST de Logs
 * Maneja el ciclo de vida del servidor Spring Boot que expone los logs
 * Respeta la arquitectura: Controlador → Servicio → Gestor
 */
public class ControladorLogsApi {

    private static final String TAG = "ControladorLogsApi";
    private final ServicioLogs servicioLogs;

    public ControladorLogsApi(ServicioLogs servicioLogs) {
        this.servicioLogs = servicioLogs;
        LoggerCentral.info(TAG, "ControladorLogsApi inicializado");
    }

    /**
     * Inicia el servidor REST API en el puerto especificado
     * @param puerto Puerto en el que se ejecutará el servidor
     * @return true si se inició correctamente, false si ya estaba activo
     */
    public boolean iniciarApiRest(int puerto) {
        try {
            LoggerCentral.info(TAG, "Solicitando inicio de API REST en puerto " + puerto);
            boolean resultado = servicioLogs.iniciarApiRest(puerto);

            if (resultado) {
                LoggerCentral.info(TAG, "API REST iniciado desde el controlador");
            } else {
                LoggerCentral.warn(TAG, "No se pudo iniciar API REST (posiblemente ya está activo)");
            }

            return resultado;
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al iniciar API REST desde controlador: " + e.getMessage());
            throw new RuntimeException("Error al iniciar API REST", e);
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
            LoggerCentral.info(TAG, "Solicitando detención de API REST");
            servicioLogs.detenerApiRest();
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al detener API REST desde controlador: " + e.getMessage());
            throw new RuntimeException("Error al detener API REST", e);
        }
    }

    /**
     * Verifica si el servidor REST API está activo
     */
    public boolean estaActivo() {
        try {
            return servicioLogs.apiRestEstaActivo();
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al verificar estado de API REST: " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene la URL base del API REST
     */
    public String getUrlBase(int puerto) {
        if (estaActivo()) {
            return "http://localhost:" + puerto + "/api/logs";
        }
        return null;
    }
}
