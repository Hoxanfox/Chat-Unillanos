package controlador.logs;

import dto.logs.DTOLog;
import logger.LoggerCentral;
import observador.IObservador;
import servicio.logs.ServicioLogs;

import java.util.List;

/**
 * Controlador de Logs - Capa de Presentación
 * Maneja las peticiones de la interfaz de usuario relacionadas con logs
 * Actúa como intermediario entre la vista y el servicio de logs
 */
public class ControladorLogs {

    private static final String TAG = "ControladorLogs";
    private final ServicioLogs servicioLogs;

    public ControladorLogs(ServicioLogs servicioLogs) {
        this.servicioLogs = servicioLogs;
        LoggerCentral.info(TAG, "ControladorLogs inicializado");
    }

    /**
     * Registra un observador para recibir actualizaciones de logs en tiempo real
     */
    public void registrarObservadorLogs(IObservador observador) {
        try {
            servicioLogs.obtenerGestor().registrarObservador(observador);
            LoggerCentral.info(TAG, "Observador registrado para logs en tiempo real");
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al registrar observador: " + e.getMessage());
            throw new RuntimeException("Error al registrar observador de logs", e);
        }
    }

    /**
     * Remueve un observador
     */
    public void removerObservadorLogs(IObservador observador) {
        try {
            servicioLogs.obtenerGestor().removerObservador(observador);
            LoggerCentral.info(TAG, "Observador removido de logs");
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al remover observador: " + e.getMessage());
        }
    }

    /**
     * Obtiene los logs recientes
     */
    public List<DTOLog> obtenerLogsRecientes(int cantidad) {
        try {
            return servicioLogs.obtenerLogsRecientes(cantidad);
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error en obtenerLogsRecientes: " + e.getMessage());
            throw new RuntimeException("No se pudieron obtener los logs recientes", e);
        }
    }

    /**
     * Obtiene todos los logs en memoria
     */
    public List<DTOLog> obtenerTodosLosLogs() {
        try {
            return servicioLogs.obtenerTodosLosLogs();
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error en obtenerTodosLosLogs: " + e.getMessage());
            throw new RuntimeException("No se pudieron obtener los logs", e);
        }
    }

    /**
     * Lee logs desde el archivo físico
     */
    public List<DTOLog> leerLogsDesdeArchivo(int maxLineas) {
        try {
            return servicioLogs.leerLogsDesdeArchivo(maxLineas);
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error en leerLogsDesdeArchivo: " + e.getMessage());
            throw new RuntimeException("No se pudieron leer los logs desde el archivo", e);
        }
    }

    /**
     * Filtra logs por nivel
     */
    public List<DTOLog> filtrarPorNivel(String nivel) {
        try {
            return servicioLogs.filtrarPorNivel(nivel);
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error en filtrarPorNivel: " + e.getMessage());
            throw new RuntimeException("No se pudieron filtrar los logs por nivel", e);
        }
    }

    /**
     * Filtra logs por source
     */
    public List<DTOLog> filtrarPorSource(String source) {
        try {
            return servicioLogs.filtrarPorSource(source);
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error en filtrarPorSource: " + e.getMessage());
            throw new RuntimeException("No se pudieron filtrar los logs por source", e);
        }
    }

    /**
     * Busca logs que contengan un texto
     */
    public List<DTOLog> buscarPorTexto(String texto) {
        try {
            return servicioLogs.buscarPorTexto(texto);
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error en buscarPorTexto: " + e.getMessage());
            throw new RuntimeException("No se pudo realizar la búsqueda de logs", e);
        }
    }

    /**
     * Limpia los logs en memoria
     */
    public void limpiarLogs() {
        try {
            servicioLogs.limpiarLogs();
            LoggerCentral.info(TAG, "Logs limpiados por el usuario");
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error en limpiarLogs: " + e.getMessage());
            throw new RuntimeException("No se pudieron limpiar los logs", e);
        }
    }

    /**
     * Obtiene estadísticas de logs
     */
    public String obtenerEstadisticas() {
        try {
            return servicioLogs.obtenerEstadisticas().toString();
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error en obtenerEstadisticas: " + e.getMessage());
            throw new RuntimeException("No se pudieron obtener las estadísticas", e);
        }
    }
}

