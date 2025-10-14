package com.unillanos.server.service.impl;
import com.unillanos.server.repository.interfaces.ILogRepository;
import com.unillanos.server.repository.models.LogEntity;
import org.springframework.beans.factory.annotation.Value;
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
    private final int nettyPort;
    private final ILogRepository logRepository;

    private final LocalDateTime startTime;

    public AdminMonitoringService(ConnectionManager connectionManager,
                                  ILogRepository logRepository,
                                  @Value("${server.netty.port:8080}") int nettyPort) {
        this.connectionManager = connectionManager;
        this.logRepository = logRepository;
        this.nettyPort = nettyPort;
        this.startTime = LocalDateTime.now();
    }

    /**
     * Retorna un mapa simple con métricas del servidor para el dashboard.
     */
    public Map<String, Object> getServerStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("active", true);
        status.put("port", nettyPort);
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
}


