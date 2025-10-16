package com.unillanos.server.repository.impl;

import com.unillanos.server.entity.NotificacionEntity;
import com.unillanos.server.exception.RepositoryException;
import com.unillanos.server.repository.interfaces.INotificacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación del repositorio de notificaciones.
 */
@Repository
public class NotificacionRepositoryImpl implements INotificacionRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificacionRepositoryImpl.class);
    
    private final DataSource dataSource;

    public NotificacionRepositoryImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public NotificacionEntity save(NotificacionEntity notificacion) {
        String sql = "INSERT INTO notificaciones (id, usuario_id, tipo, titulo, mensaje, remitente_id, " +
                    "canal_id, leida, timestamp, accion, metadata) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, notificacion.getId());
            stmt.setString(2, notificacion.getUsuarioId());
            stmt.setString(3, notificacion.getTipo());
            stmt.setString(4, notificacion.getTitulo());
            stmt.setString(5, notificacion.getMensaje());
            stmt.setString(6, notificacion.getRemitenteId());
            stmt.setString(7, notificacion.getCanalId());
            stmt.setBoolean(8, notificacion.isLeida());
            stmt.setTimestamp(9, Timestamp.valueOf(notificacion.getTimestamp()));
            stmt.setString(10, notificacion.getAccion());
            stmt.setString(11, notificacion.getMetadata());
            
            stmt.executeUpdate();
            logger.debug("Notificación guardada: {}", notificacion.getId());
            
            return notificacion;
            
        } catch (SQLException e) {
            logger.error("Error al guardar notificación: {}", e.getMessage(), e);
            throw new RepositoryException("Error al guardar notificación", "save", e);
        }
    }

    @Override
    public Optional<NotificacionEntity> findById(String id) {
        String sql = "SELECT * FROM notificaciones WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error al buscar notificación por ID: {}", e.getMessage(), e);
            throw new RepositoryException("Error al buscar notificación", "findById", e);
        }
        
        return Optional.empty();
    }

    @Override
    public List<NotificacionEntity> findByUsuarioId(String usuarioId) {
        String sql = "SELECT * FROM notificaciones WHERE usuario_id = ? ORDER BY timestamp DESC";
        return findNotificaciones(sql, usuarioId);
    }

    @Override
    public List<NotificacionEntity> findNoLeidasByUsuarioId(String usuarioId) {
        String sql = "SELECT * FROM notificaciones WHERE usuario_id = ? AND leida = false ORDER BY timestamp DESC";
        return findNotificaciones(sql, usuarioId);
    }

    @Override
    public int countNoLeidasByUsuarioId(String usuarioId) {
        String sql = "SELECT COUNT(*) FROM notificaciones WHERE usuario_id = ? AND leida = false";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, usuarioId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al contar notificaciones no leídas: {}", e.getMessage(), e);
            throw new RepositoryException("Error al contar notificaciones", "countNoLeidasByUsuarioId", e);
        }
        
        return 0;
    }

    @Override
    public void marcarComoLeida(String notificacionId) {
        String sql = "UPDATE notificaciones SET leida = true WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, notificacionId);
            stmt.executeUpdate();
            logger.debug("Notificación marcada como leída: {}", notificacionId);
            
        } catch (SQLException e) {
            logger.error("Error al marcar notificación como leída: {}", e.getMessage(), e);
            throw new RepositoryException("Error al marcar notificación", "marcarComoLeida", e);
        }
    }

    @Override
    public void marcarTodasComoLeidas(String usuarioId) {
        String sql = "UPDATE notificaciones SET leida = true WHERE usuario_id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, usuarioId);
            stmt.executeUpdate();
            logger.debug("Todas las notificaciones marcadas como leídas para usuario: {}", usuarioId);
            
        } catch (SQLException e) {
            logger.error("Error al marcar todas las notificaciones como leídas: {}", e.getMessage(), e);
            throw new RepositoryException("Error al marcar notificaciones", "marcarTodasComoLeidas", e);
        }
    }

    @Override
    public void delete(String notificacionId) {
        String sql = "DELETE FROM notificaciones WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, notificacionId);
            stmt.executeUpdate();
            logger.debug("Notificación eliminada: {}", notificacionId);
            
        } catch (SQLException e) {
            logger.error("Error al eliminar notificación: {}", e.getMessage(), e);
            throw new RepositoryException("Error al eliminar notificación", "delete", e);
        }
    }

    @Override
    public void deleteAntiguas(int dias) {
        String sql = "DELETE FROM notificaciones WHERE timestamp < DATE_SUB(NOW(), INTERVAL ? DAY)";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, dias);
            int eliminadas = stmt.executeUpdate();
            logger.debug("Notificaciones antiguas eliminadas: {}", eliminadas);
            
        } catch (SQLException e) {
            logger.error("Error al eliminar notificaciones antiguas: {}", e.getMessage(), e);
            throw new RepositoryException("Error al eliminar notificaciones antiguas", "deleteAntiguas", e);
        }
    }

    private List<NotificacionEntity> findNotificaciones(String sql, String usuarioId) {
        List<NotificacionEntity> notificaciones = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, usuarioId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    notificaciones.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error al buscar notificaciones: {}", e.getMessage(), e);
            throw new RepositoryException("Error al buscar notificaciones", "findNotificaciones", e);
        }
        
        return notificaciones;
    }

    private NotificacionEntity mapRow(ResultSet rs) throws SQLException {
        NotificacionEntity notificacion = new NotificacionEntity();
        notificacion.setId(rs.getString("id"));
        notificacion.setUsuarioId(rs.getString("usuario_id"));
        notificacion.setTipo(rs.getString("tipo"));
        notificacion.setTitulo(rs.getString("titulo"));
        notificacion.setMensaje(rs.getString("mensaje"));
        notificacion.setRemitenteId(rs.getString("remitente_id"));
        notificacion.setCanalId(rs.getString("canal_id"));
        notificacion.setLeida(rs.getBoolean("leida"));
        notificacion.setTimestamp(rs.getTimestamp("timestamp").toLocalDateTime());
        notificacion.setAccion(rs.getString("accion"));
        notificacion.setMetadata(rs.getString("metadata"));
        return notificacion;
    }
}
