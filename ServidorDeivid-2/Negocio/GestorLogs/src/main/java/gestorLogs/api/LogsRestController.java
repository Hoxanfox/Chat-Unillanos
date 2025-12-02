package gestorLogs.api;

import dto.logs.DTOLog;
import gestorLogs.GestorLogs;
import logger.LoggerCentral;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.io.IOException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import observador.IObservador;

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

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public LogsRestController(GestorLogs gestorLogs) {
        this.gestorLogs = gestorLogs;
        LoggerCentral.info(TAG, "LogsRestController inicializado");

        // Registrar observador para SSE
        this.gestorLogs.registrarObservador(new IObservador() {
            @Override
            public void actualizar(String tipoDeDato, Object datos) {
                if ("NUEVO_LOG".equals(tipoDeDato) && datos instanceof DTOLog) {
                    DTOLog log = (DTOLog) datos;
                    // Transmit all logs, filtering is done in Frontend
                    notificarEmitters(log);
                }
            }
        });
    }

    private void notificarEmitters(DTOLog log) {
        List<SseEmitter> deadEmitters = new ArrayList<>();
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name("log").data(log));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        });
        emitters.removeAll(deadEmitters);
    }

    /**
     * GET /api/logs/stream
     * Stream de logs en tiempo real (SSE)
     */
    @GetMapping("/stream")
    public SseEmitter streamLogs() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // Timeout infinito (o muy largo)

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((e) -> emitters.remove(emitter));

        emitters.add(emitter);

        // Enviar un evento inicial de conexión
        try {
            emitter.send(SseEmitter.event().name("connected").data("Conexión SSE establecida"));
        } catch (IOException e) {
            emitters.remove(emitter);
        }

        LoggerCentral.debug(TAG, "Nueva conexión SSE establecida. Total clientes: " + emitters.size());
        return emitter;
    }

    /**
     * GET /api/logs
     * Obtiene todos los logs en memoria (Excluyendo DEBUG)
     */
    @GetMapping
    public ResponseEntity<List<DTOLog>> obtenerTodosLosLogs() {
        try {
            List<DTOLog> logs = gestorLogs.obtenerTodosLosLogsEnMemoria().stream()
                    .filter(log -> !"DEBUG".equalsIgnoreCase(log.getLevel()))
                    .collect(Collectors.toList());

            LoggerCentral.debug(TAG, "GET /api/logs - " + logs.size() + " logs retornados");
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al obtener todos los logs: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/logs/recent?cantidad=100
     * Obtiene los logs más recientes (Excluyendo DEBUG)
     */
    @GetMapping("/recent")
    public ResponseEntity<List<DTOLog>> obtenerLogsRecientes(
            @RequestParam(defaultValue = "100") int cantidad) {
        try {
            List<DTOLog> logs = gestorLogs.obtenerLogsRecientes(cantidad).stream()
                    .filter(log -> !"DEBUG".equalsIgnoreCase(log.getLevel()))
                    .collect(Collectors.toList());

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
     * Filtra logs por source/tag (Excluyendo DEBUG)
     */
    @GetMapping("/filter/source")
    public ResponseEntity<List<DTOLog>> filtrarPorSource(
            @RequestParam String source) {
        try {
            if (source == null || source.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            List<DTOLog> logs = gestorLogs.filtrarPorSource(source).stream()
                    .filter(log -> !"DEBUG".equalsIgnoreCase(log.getLevel()))
                    .collect(Collectors.toList());

            LoggerCentral.debug(TAG, "GET /api/logs/filter/source?source=" + source + " - " + logs.size() + " logs");
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al filtrar por source: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/logs/search?texto=error
     * Busca logs que contengan un texto específico (Excluyendo DEBUG)
     */
    @GetMapping("/search")
    public ResponseEntity<List<DTOLog>> buscarPorTexto(
            @RequestParam String texto) {
        try {
            if (texto == null || texto.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            List<DTOLog> logs = gestorLogs.buscarPorTexto(texto).stream()
                    .filter(log -> !"DEBUG".equalsIgnoreCase(log.getLevel()))
                    .collect(Collectors.toList());

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
