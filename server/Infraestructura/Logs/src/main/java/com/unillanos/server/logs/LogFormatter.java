package com.unillanos.server.logs;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Utilidades para formatear logs en diferentes formatos.
 * 
 * Proporciona métodos para convertir LogEntry a diferentes formatos
 * de salida (texto, HTML, JSON, CSV).
 * 
 * @author Chat-Unillanos Team
 * @version 1.0.0
 */
public class LogFormatter {
    
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Formatea una entrada de log como texto legible
     */
    public static String formatAsText(LogEntry entry) {
        StringBuilder sb = new StringBuilder();
        
        // Timestamp
        sb.append("[").append(entry.getTimestamp().format(TIMESTAMP_FORMATTER)).append("] ");
        
        // Tipo con color/ícono
        sb.append("[").append(entry.getType().getIcon()).append(" ")
          .append(entry.getType().getDescription()).append("] ");
        
        // Usuario
        if (entry.getUsuarioId() != null) {
            sb.append("Usuario: ").append(entry.getUsuarioId()).append(" ");
        }
        
        // IP
        if (entry.getIpAddress() != null) {
            sb.append("IP: ").append(entry.getIpAddress()).append(" ");
        }
        
        // Mensaje
        sb.append(entry.getMessage());
        
        return sb.toString();
    }
    
    /**
     * Formatea una entrada de log como HTML
     */
    public static String formatAsHtml(LogEntry entry) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("<div class=\"log-entry\" style=\"margin: 2px 0; padding: 5px; border-left: 3px solid ")
          .append(entry.getType().getColor()).append(";\">");
        
        // Header con timestamp y tipo
        sb.append("<div class=\"log-header\" style=\"font-weight: bold; color: #666;\">");
        sb.append("<span class=\"timestamp\">")
          .append(entry.getTimestamp().format(TIMESTAMP_FORMATTER))
          .append("</span> ");
        sb.append("<span class=\"type\" style=\"color: ")
          .append(entry.getType().getColor())
          .append(";\">")
          .append(entry.getType().getIcon())
          .append(" ")
          .append(entry.getType().getDescription())
          .append("</span>");
        
        // Usuario e IP
        if (entry.getUsuarioId() != null || entry.getIpAddress() != null) {
            sb.append(" <span class=\"context\">");
            if (entry.getUsuarioId() != null) {
                sb.append("Usuario: <strong>").append(entry.getUsuarioId()).append("</strong> ");
            }
            if (entry.getIpAddress() != null) {
                sb.append("IP: <code>").append(entry.getIpAddress()).append("</code> ");
            }
            sb.append("</span>");
        }
        
        sb.append("</div>");
        
        // Mensaje
        sb.append("<div class=\"log-message\" style=\"margin-top: 3px;\">")
          .append(escapeHtml(entry.getMessage()))
          .append("</div>");
        
        sb.append("</div>");
        
        return sb.toString();
    }
    
    /**
     * Formatea una entrada de log como JSON
     */
    public static String formatAsJson(LogEntry entry) {
        return entry.toJson();
    }
    
    /**
     * Formatea una entrada de log como CSV
     */
    public static String formatAsCsv(LogEntry entry) {
        StringBuilder sb = new StringBuilder();
        
        // Timestamp
        sb.append("\"").append(entry.getTimestamp().format(TIMESTAMP_FORMATTER)).append("\",");
        
        // Tipo
        sb.append("\"").append(entry.getType().getDescription()).append("\",");
        
        // Usuario
        sb.append("\"").append(entry.getUsuarioId() != null ? entry.getUsuarioId() : "").append("\",");
        
        // IP
        sb.append("\"").append(entry.getIpAddress() != null ? entry.getIpAddress() : "").append("\",");
        
        // Mensaje (escapar comillas)
        sb.append("\"").append(entry.getMessage().replace("\"", "\"\"")).append("\"");
        
        return sb.toString();
    }
    
    /**
     * Formatea una lista de logs como texto
     */
    public static String formatListAsText(List<LogEntry> logs) {
        StringBuilder sb = new StringBuilder();
        
        for (LogEntry log : logs) {
            sb.append(formatAsText(log)).append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Formatea una lista de logs como HTML
     */
    public static String formatListAsHtml(List<LogEntry> logs) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("<html><head><style>");
        sb.append("body { font-family: 'Courier New', monospace; font-size: 12px; }");
        sb.append(".log-entry { margin: 2px 0; padding: 5px; border-radius: 3px; }");
        sb.append(".log-header { font-weight: bold; margin-bottom: 3px; }");
        sb.append(".log-message { margin-top: 3px; }");
        sb.append("</style></head><body>");
        
        for (LogEntry log : logs) {
            sb.append(formatAsHtml(log));
        }
        
        sb.append("</body></html>");
        
        return sb.toString();
    }
    
    /**
     * Formatea una lista de logs como CSV con headers
     */
    public static String formatListAsCsv(List<LogEntry> logs) {
        StringBuilder sb = new StringBuilder();
        
        // Headers
        sb.append("Timestamp,Tipo,Usuario,IP,Mensaje\n");
        
        // Datos
        for (LogEntry log : logs) {
            sb.append(formatAsCsv(log)).append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Escapa caracteres HTML especiales
     */
    private static String escapeHtml(String text) {
        if (text == null) return "";
        
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }
    
    /**
     * Formatea un resumen estadístico de logs
     */
    public static String formatStats(List<LogEntry> logs) {
        if (logs.isEmpty()) {
            return "No hay logs disponibles";
        }
        
        StringBuilder sb = new StringBuilder();
        
        // Total de logs
        sb.append("Total de logs: ").append(logs.size()).append("\n");
        
        // Conteo por tipo
        sb.append("\nLogs por tipo:\n");
        for (LogType type : LogType.values()) {
            long count = logs.stream()
                .filter(log -> log.getType() == type)
                .count();
            if (count > 0) {
                sb.append("  ").append(type.getIcon())
                  .append(" ").append(type.getDescription())
                  .append(": ").append(count).append("\n");
            }
        }
        
        // Rango de fechas
        LocalDateTime oldest = logs.stream()
            .map(LogEntry::getTimestamp)
            .min(LocalDateTime::compareTo)
            .orElse(LocalDateTime.now());
            
        LocalDateTime newest = logs.stream()
            .map(LogEntry::getTimestamp)
            .max(LocalDateTime::compareTo)
            .orElse(LocalDateTime.now());
        
        sb.append("\nRango de fechas:\n");
        sb.append("  Desde: ").append(oldest.format(TIMESTAMP_FORMATTER)).append("\n");
        sb.append("  Hasta: ").append(newest.format(TIMESTAMP_FORMATTER)).append("\n");
        
        return sb.toString();
    }
}
