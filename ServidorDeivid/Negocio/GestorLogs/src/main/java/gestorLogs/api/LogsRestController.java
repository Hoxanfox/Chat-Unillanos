package gestorLogs.api;

import dto.logs.DTOLog;
import gestorLogs.GestorLogs;
import logger.LoggerCentral;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para exponer los logs del sistema
 * Endpoints HTTP para acceder a los logs desde cualquier cliente
 */
@RestController
@RequestMapping("/api/logs")
@CrossOrigin(origins = "*") // Permitir CORS para acceso desde diferentes orígenes
public class LogsRestController {

    private static final String TAG = "LogsRestController";
    private final GestorLogs gestorLogs;

    public LogsRestController(GestorLogs gestorLogs) {
        this.gestorLogs = gestorLogs;
        LoggerCentral.info(TAG, "LogsRestController inicializado");
    }

    /**
     * GET /api/logs
     * Obtiene todos los logs en memoria
     */
    @GetMapping
    public ResponseEntity<List<DTOLog>> obtenerTodosLosLogs() {
        try {
            List<DTOLog> logs = gestorLogs.obtenerTodosLosLogsEnMemoria();
            LoggerCentral.debug(TAG, "GET /api/logs - " + logs.size() + " logs retornados");
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al obtener todos los logs: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/logs/recent?cantidad=100
     * Obtiene los logs más recientes
     */
    @GetMapping("/recent")
    public ResponseEntity<List<DTOLog>> obtenerLogsRecientes(
            @RequestParam(defaultValue = "100") int cantidad) {
        try {
            List<DTOLog> logs = gestorLogs.obtenerLogsRecientes(cantidad);
            LoggerCentral.debug(TAG, "GET /api/logs/recent - " + logs.size() + " logs retornados");
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al obtener logs recientes: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/logs/filter/level?nivel=ERROR
     * Filtra logs por nivel (INFO, WARNING, ERROR, DEBUG)
     */
    @GetMapping("/filter/level")
    public ResponseEntity<List<DTOLog>> filtrarPorNivel(
            @RequestParam String nivel) {
        try {
            if (nivel == null || nivel.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            List<DTOLog> logs = gestorLogs.filtrarPorNivel(nivel);
            LoggerCentral.debug(TAG, "GET /api/logs/filter/level?nivel=" + nivel + " - " + logs.size() + " logs");
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al filtrar por nivel: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/logs/filter/source?source=MiClase
     * Filtra logs por source/tag
     */
    @GetMapping("/filter/source")
    public ResponseEntity<List<DTOLog>> filtrarPorSource(
            @RequestParam String source) {
        try {
            if (source == null || source.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            List<DTOLog> logs = gestorLogs.filtrarPorSource(source);
            LoggerCentral.debug(TAG, "GET /api/logs/filter/source?source=" + source + " - " + logs.size() + " logs");
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al filtrar por source: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/logs/search?texto=error
     * Busca logs que contengan un texto específico
     */
    @GetMapping("/search")
    public ResponseEntity<List<DTOLog>> buscarPorTexto(
            @RequestParam String texto) {
        try {
            if (texto == null || texto.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            List<DTOLog> logs = gestorLogs.buscarPorTexto(texto);
            LoggerCentral.debug(TAG, "GET /api/logs/search?texto=" + texto + " - " + logs.size() + " logs");
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al buscar por texto: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/logs/stats
     * Obtiene estadísticas de los logs
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticas() {
        try {
            GestorLogs.EstadisticasLogs stats = gestorLogs.obtenerEstadisticas();
            Map<String, Object> response = new HashMap<>();
            response.put("total", stats.getTotal());
            response.put("info", stats.getInfo());
            response.put("warning", stats.getWarning());
            response.put("error", stats.getError());
            response.put("debug", stats.getDebug());

            LoggerCentral.debug(TAG, "GET /api/logs/stats - Estadísticas retornadas");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al obtener estadísticas: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * DELETE /api/logs
     * Limpia todos los logs en memoria
     */
    @DeleteMapping
    public ResponseEntity<Map<String, String>> limpiarLogs() {
        try {
            gestorLogs.limpiarLogsEnMemoria();
            Map<String, String> response = new HashMap<>();
            response.put("message", "Logs limpiados exitosamente");
            LoggerCentral.info(TAG, "DELETE /api/logs - Logs limpiados");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al limpiar logs: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/logs/health
     * Health check del servicio de logs
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Logs API");
        response.put("logsEnMemoria", String.valueOf(gestorLogs.obtenerTodosLosLogsEnMemoria().size()));
        return ResponseEntity.ok(response);
    }
}

