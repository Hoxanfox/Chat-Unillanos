package com.unillanos.server.service.impl;

import com.unillanos.server.config.ServerConfigProperties;
import com.unillanos.server.repository.interfaces.ILogRepository;
import com.unillanos.server.repository.interfaces.IMensajeRepository;
import com.unillanos.server.repository.interfaces.IUsuarioRepository;
import com.unillanos.server.entity.LogEntity;
import com.unillanos.server.entity.TipoMensaje;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio de monitoreo para la GUI de administración.
 * Expone estado del servidor y logs recientes de forma segura.
 */
@Service
public class AdminMonitoringService {

    private final ConnectionManager connectionManager;
    private final ServerConfigProperties config;
    private final ILogRepository logRepository;
    private final IMensajeRepository mensajeRepository;
    private final IUsuarioRepository usuarioRepository;

    private final LocalDateTime startTime;

    public AdminMonitoringService(ConnectionManager connectionManager,
                                  ServerConfigProperties config,
                                  ILogRepository logRepository,
                                  IMensajeRepository mensajeRepository,
                                  IUsuarioRepository usuarioRepository) {
        this.connectionManager = connectionManager;
        this.config = config;
        this.logRepository = logRepository;
        this.mensajeRepository = mensajeRepository;
        this.usuarioRepository = usuarioRepository;
        this.startTime = LocalDateTime.now();
    }

    /**
     * Retorna un mapa simple con métricas del servidor para el dashboard.
     */
    public Map<String, Object> getServerStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("active", true);
        status.put("port", config.getNetty().getPort());
        status.put("connections", connectionManager.getAllConnections().size());
        status.put("uptime", formatUptime(Duration.between(startTime, LocalDateTime.now())));
        return status;
    }

    /**
     * Retorna una lista de logs recientes formateados para visualización.
     */
    public List<String> getRecentLogs(int limit) {
        List<LogEntity> logs = logRepository.findRecent(limit);
        return logs.stream()
                .map(l -> String.format("%s [%s] %s - %s",
                        l.getTimestamp() != null ? l.getTimestamp().toString() : "",
                        l.getTipo(),
                        l.getAccion(),
                        truncate(l.getDetalles(), 140)))
                .collect(Collectors.toList());
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 3) + "..." : s;
    }

    private String formatUptime(Duration d) {
        long h = d.toHours();
        long m = d.toMinutesPart();
        long s = d.toSecondsPart();
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    /**
     * Retorna estadísticas detalladas para los gráficos del dashboard.
     */
    public Map<String, Object> getEstadisticas() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Conexiones actuales
            stats.put("connections", connectionManager.getAllConnections().size());
            
            // Contar mensajes por tipo hoy
            int mensajesDirectos = mensajeRepository.countByTipoHoy(TipoMensaje.DIRECT);
            int mensajesCanal = mensajeRepository.countByTipoHoy(TipoMensaje.CHANNEL);
            stats.put("mensajesDirectos", mensajesDirectos);
            stats.put("mensajesCanal", mensajesCanal);
            
            // Top usuarios activos
            List<Map<String, Object>> topUsuarios = obtenerTopUsuariosActivos(5);
            stats.put("topUsuarios", topUsuarios);
            
        } catch (Exception e) {
            // En caso de error, retornar valores por defecto
            stats.put("connections", 0);
            stats.put("mensajesDirectos", 0);
            stats.put("mensajesCanal", 0);
            stats.put("topUsuarios", List.of());
        }
        
        return stats;
    }

    /**
     * Obtiene los usuarios más activos del día.
     */
    private List<Map<String, Object>> obtenerTopUsuariosActivos(int limit) {
        try {
            return usuarioRepository.getTopActivos(limit);
        } catch (Exception e) {
            return List.of();
        }
    }
}


