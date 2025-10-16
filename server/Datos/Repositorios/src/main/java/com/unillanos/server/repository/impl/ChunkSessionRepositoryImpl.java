package com.unillanos.server.repository.impl;

import com.unillanos.server.repository.interfaces.IChunkSessionRepository;
import com.unillanos.server.entity.ChunkSessionEntity;
import com.unillanos.server.entity.EstadoSesion;
import com.unillanos.server.repository.mappers.ChunkSessionMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.lang.reflect.Type;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Implementación del repositorio para gestionar sesiones de subida de archivos por chunks.
 */
@Repository
public class ChunkSessionRepositoryImpl implements IChunkSessionRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(ChunkSessionRepositoryImpl.class);
    
    private final DataSource dataSource;
    private final Gson gson;

    @Autowired
    public ChunkSessionRepositoryImpl(DataSource dataSource) {
        this.dataSource = dataSource;
        this.gson = new Gson();
    }

    @Override
    public ChunkSessionEntity iniciarSesion(ChunkSessionEntity session) {
        String sql = "INSERT INTO chunk_sessions (session_id, usuario_id, nombre_archivo, tipo_mime, " +
                    "tamano_total, total_chunks, chunks_recibidos, fecha_inicio, ultima_actividad, estado) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, session.getSessionId());
            stmt.setString(2, session.getUsuarioId());
            stmt.setString(3, session.getNombreArchivo());
            stmt.setString(4, session.getTipoMime());
            stmt.setLong(5, session.getTamanoTotal());
            stmt.setInt(6, session.getTotalChunks());
            stmt.setString(7, gson.toJson(session.getChunksRecibidos()));
            stmt.setTimestamp(8, Timestamp.valueOf(session.getFechaInicio()));
            stmt.setTimestamp(9, Timestamp.valueOf(session.getUltimaActividad()));
            stmt.setString(10, session.getEstadoSesion().toString());
            
            stmt.executeUpdate();
            logger.debug("Sesión de chunk iniciada: {}", session.getSessionId());
            return session;
            
        } catch (SQLException e) {
            logger.error("Error al iniciar sesión de chunk: {}", e.getMessage(), e);
            throw new RuntimeException("Error al iniciar sesión de chunk", e);
        }
    }

    @Override
    public void registrarChunk(String sessionId, int numeroChunk) {
        String sql = "UPDATE chunk_sessions SET chunks_recibidos = ?, ultima_actividad = ? WHERE session_id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Obtener chunks actuales
            ChunkSessionEntity session = obtenerSesion(sessionId).orElseThrow(
                () -> new RuntimeException("Sesión no encontrada: " + sessionId)
            );
            
            // Agregar nuevo chunk
            Set<Integer> chunksRecibidos = new HashSet<>(session.getChunksRecibidos());
            chunksRecibidos.add(numeroChunk);
            
            stmt.setString(1, gson.toJson(chunksRecibidos));
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(3, sessionId);
            
            stmt.executeUpdate();
            logger.debug("Chunk {} registrado para sesión {}", numeroChunk, sessionId);
            
        } catch (SQLException e) {
            logger.error("Error al registrar chunk: {}", e.getMessage(), e);
            throw new RuntimeException("Error al registrar chunk", e);
        }
    }

    @Override
    public Optional<ChunkSessionEntity> obtenerSesion(String sessionId) {
        String sql = "SELECT * FROM chunk_sessions WHERE session_id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, sessionId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(ChunkSessionMapper.mapRow(rs, gson));
                }
                return Optional.empty();
            }
            
        } catch (SQLException e) {
            logger.error("Error al obtener sesión: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener sesión", e);
        }
    }

    @Override
    public void actualizarEstado(String sessionId, EstadoSesion nuevoEstado) {
        String sql = "UPDATE chunk_sessions SET estado = ?, ultima_actividad = ? WHERE session_id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, nuevoEstado.toString());
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(3, sessionId);
            
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated == 0) {
                logger.warn("No se encontró sesión para actualizar estado: {}", sessionId);
            } else {
                logger.debug("Estado actualizado para sesión {}: {}", sessionId, nuevoEstado);
            }
            
        } catch (SQLException e) {
            logger.error("Error al actualizar estado de sesión: {}", e.getMessage(), e);
            throw new RuntimeException("Error al actualizar estado de sesión", e);
        }
    }

    @Override
    public void actualizarUltimaActividad(String sessionId) {
        String sql = "UPDATE chunk_sessions SET ultima_actividad = ? WHERE session_id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(2, sessionId);
            
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            logger.error("Error al actualizar última actividad: {}", e.getMessage(), e);
            throw new RuntimeException("Error al actualizar última actividad", e);
        }
    }
    
    @Override
    public void actualizarSesion(ChunkSessionEntity session) {
        String sql = "UPDATE chunk_sessions SET chunks_recibidos = ?, ultima_actividad = ?, estado = ? WHERE session_id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, gson.toJson(session.getChunksRecibidos()));
            stmt.setTimestamp(2, Timestamp.valueOf(session.getUltimaActividad()));
            stmt.setString(3, session.getEstadoSesion().toString());
            stmt.setString(4, session.getSessionId());
            
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated == 0) {
                logger.warn("No se encontró sesión para actualizar: {}", session.getSessionId());
            } else {
                logger.debug("Sesión actualizada: {}", session.getSessionId());
            }
            
        } catch (SQLException e) {
            logger.error("Error al actualizar sesión: {}", e.getMessage(), e);
            throw new RuntimeException("Error al actualizar sesión", e);
        }
    }

    @Override
    public void eliminarSesion(String sessionId) {
        String sql = "DELETE FROM chunk_sessions WHERE session_id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, sessionId);
            
            int rowsDeleted = stmt.executeUpdate();
            if (rowsDeleted > 0) {
                logger.debug("Sesión eliminada: {}", sessionId);
            } else {
                logger.warn("No se encontró sesión para eliminar: {}", sessionId);
            }
            
        } catch (SQLException e) {
            logger.error("Error al eliminar sesión: {}", e.getMessage(), e);
            throw new RuntimeException("Error al eliminar sesión", e);
        }
    }

    @Override
    public List<ChunkSessionEntity> obtenerSesionesPorUsuario(String usuarioId) {
        String sql = "SELECT * FROM chunk_sessions WHERE usuario_id = ? ORDER BY fecha_inicio DESC";
        List<ChunkSessionEntity> sesiones = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, usuarioId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    sesiones.add(ChunkSessionMapper.mapRow(rs, gson));
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error al obtener sesiones por usuario: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener sesiones por usuario", e);
        }
        
        return sesiones;
    }

    @Override
    public int limpiarSesionesExpiradas(int horasExpiracion) {
        String sql = "DELETE FROM chunk_sessions WHERE estado = 'EXPIRADA' OR " +
                    "(estado = 'ACTIVA' AND ultima_actividad < ?)";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            LocalDateTime fechaLimite = LocalDateTime.now().minusHours(horasExpiracion);
            stmt.setTimestamp(1, Timestamp.valueOf(fechaLimite));
            
            int rowsDeleted = stmt.executeUpdate();
            logger.info("Sesiones expiradas eliminadas: {}", rowsDeleted);
            return rowsDeleted;
            
        } catch (SQLException e) {
            logger.error("Error al limpiar sesiones expiradas: {}", e.getMessage(), e);
            throw new RuntimeException("Error al limpiar sesiones expiradas", e);
        }
    }

    @Override
    public List<ChunkSessionEntity> obtenerSesionesActivas() {
        String sql = "SELECT * FROM chunk_sessions WHERE estado = 'ACTIVA' ORDER BY fecha_inicio DESC";
        List<ChunkSessionEntity> sesiones = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    sesiones.add(ChunkSessionMapper.mapRow(rs, gson));
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error al obtener sesiones activas: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener sesiones activas", e);
        }
        
        return sesiones;
    }
}
