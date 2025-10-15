package com.unillanos.server.repository.mappers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.unillanos.server.repository.models.ChunkSessionEntity;
import com.unillanos.server.repository.models.EstadoSesion;

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Mapper para convertir ResultSet a ChunkSessionEntity.
 */
public class ChunkSessionMapper {
    
    private static final Type SET_INTEGER_TYPE = new TypeToken<Set<Integer>>(){}.getType();
    
    /**
     * Convierte un ResultSet a ChunkSessionEntity.
     *
     * @param rs ResultSet con los datos de la sesión
     * @param gson Instancia de Gson para deserializar JSON
     * @return ChunkSessionEntity mapeada
     * @throws SQLException si hay error al acceder a los datos
     */
    public static ChunkSessionEntity mapRow(ResultSet rs, Gson gson) throws SQLException {
        ChunkSessionEntity session = new ChunkSessionEntity();
        
        session.setSessionId(rs.getString("session_id"));
        session.setUsuarioId(rs.getString("usuario_id"));
        session.setNombreArchivo(rs.getString("nombre_archivo"));
        session.setTipoMime(rs.getString("tipo_mime"));
        session.setTamanoTotal(rs.getLong("tamano_total"));
        session.setTotalChunks(rs.getInt("total_chunks"));
        
        // Deserializar chunks recibidos desde JSON
        String chunksJson = rs.getString("chunks_recibidos");
        if (chunksJson != null && !chunksJson.trim().isEmpty()) {
            try {
                Set<Integer> chunksRecibidos = gson.fromJson(chunksJson, SET_INTEGER_TYPE);
                session.setChunksRecibidos(chunksRecibidos != null ? chunksRecibidos : new HashSet<>());
            } catch (Exception e) {
                // Si hay error en la deserialización, usar conjunto vacío
                session.setChunksRecibidos(new HashSet<>());
            }
        } else {
            session.setChunksRecibidos(new HashSet<>());
        }
        
        // Mapear timestamps
        Timestamp fechaInicio = rs.getTimestamp("fecha_inicio");
        if (fechaInicio != null) {
            session.setFechaInicio(fechaInicio.toLocalDateTime());
        }
        
        Timestamp ultimaActividad = rs.getTimestamp("ultima_actividad");
        if (ultimaActividad != null) {
            session.setUltimaActividad(ultimaActividad.toLocalDateTime());
        }
        
        // Mapear estado
        String estadoStr = rs.getString("estado");
        if (estadoStr != null) {
            try {
                session.setEstadoSesion(EstadoSesion.fromString(estadoStr));
            } catch (IllegalArgumentException e) {
                // Si el estado no es válido, usar ACTIVA por defecto
                session.setEstadoSesion(EstadoSesion.ACTIVA);
            }
        } else {
            session.setEstadoSesion(EstadoSesion.ACTIVA);
        }
        
        return session;
    }
}
