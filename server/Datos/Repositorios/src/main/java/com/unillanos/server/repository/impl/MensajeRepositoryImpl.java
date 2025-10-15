package com.unillanos.server.repository.impl;

import com.unillanos.server.exception.RepositoryException;
import com.unillanos.server.repository.interfaces.IMensajeRepository;
import com.unillanos.server.repository.mappers.MensajeMapper;
import com.unillanos.server.repository.models.EstadoMensaje;
import com.unillanos.server.repository.models.MensajeEntity;
import com.unillanos.server.repository.models.TipoMensaje;
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
 * Implementación JDBC del IMensajeRepository para interactuar con la base de datos MySQL.
 */
@Repository
public class MensajeRepositoryImpl implements IMensajeRepository {

    private static final Logger logger = LoggerFactory.getLogger(MensajeRepositoryImpl.class);
    private final DataSource dataSource;

    public MensajeRepositoryImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<MensajeEntity> findById(Long id) {
        String sql = "SELECT * FROM mensajes WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(MensajeMapper.mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error al buscar mensaje por ID: {}", e.getMessage(), e);
            throw new RepositoryException("Error al buscar mensaje por ID", "MESSAGE_FIND_BY_ID_ERROR", e);
        }
        return Optional.empty();
    }

    @Override
    public List<MensajeEntity> findMensajesDirectos(String usuarioId1, String usuarioId2, int limit, int offset) {
        List<MensajeEntity> mensajes = new ArrayList<>();
        String sql = "SELECT * FROM mensajes " +
                     "WHERE tipo = 'DIRECT' " +
                     "  AND ((remitente_id = ? AND destinatario_id = ?) " +
                     "    OR (remitente_id = ? AND destinatario_id = ?)) " +
                     "ORDER BY fecha_envio DESC " +
                     "LIMIT ? OFFSET ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, usuarioId1);
            stmt.setString(2, usuarioId2);
            stmt.setString(3, usuarioId2);
            stmt.setString(4, usuarioId1);
            stmt.setInt(5, limit);
            stmt.setInt(6, offset);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    mensajes.add(MensajeMapper.mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error al buscar mensajes directos: {}", e.getMessage(), e);
            throw new RepositoryException("Error al buscar mensajes directos", "MESSAGE_FIND_DIRECT_ERROR", e);
        }
        return mensajes;
    }

    @Override
    public List<MensajeEntity> findMensajesCanal(String canalId, int limit, int offset) {
        List<MensajeEntity> mensajes = new ArrayList<>();
        String sql = "SELECT * FROM mensajes " +
                     "WHERE tipo = 'CHANNEL' AND canal_id = ? " +
                     "ORDER BY fecha_envio DESC " +
                     "LIMIT ? OFFSET ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, canalId);
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    mensajes.add(MensajeMapper.mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error al buscar mensajes de canal: {}", e.getMessage(), e);
            throw new RepositoryException("Error al buscar mensajes de canal", "MESSAGE_FIND_CHANNEL_ERROR", e);
        }
        return mensajes;
    }

    @Override
    public Optional<MensajeEntity> findUltimoMensajeDirecto(String usuarioId1, String usuarioId2) {
        String sql = "SELECT * FROM mensajes " +
                     "WHERE tipo = 'DIRECT' " +
                     "  AND ((remitente_id = ? AND destinatario_id = ?) " +
                     "    OR (remitente_id = ? AND destinatario_id = ?)) " +
                     "ORDER BY fecha_envio DESC " +
                     "LIMIT 1";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, usuarioId1);
            stmt.setString(2, usuarioId2);
            stmt.setString(3, usuarioId2);
            stmt.setString(4, usuarioId1);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(MensajeMapper.mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error al buscar último mensaje directo: {}", e.getMessage(), e);
            throw new RepositoryException("Error al buscar último mensaje directo", "MESSAGE_FIND_LAST_DIRECT_ERROR", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<MensajeEntity> findUltimoMensajeCanal(String canalId) {
        String sql = "SELECT * FROM mensajes " +
                     "WHERE tipo = 'CHANNEL' AND canal_id = ? " +
                     "ORDER BY fecha_envio DESC " +
                     "LIMIT 1";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, canalId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(MensajeMapper.mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error al buscar último mensaje de canal: {}", e.getMessage(), e);
            throw new RepositoryException("Error al buscar último mensaje de canal", "MESSAGE_FIND_LAST_CHANNEL_ERROR", e);
        }
        return Optional.empty();
    }

    @Override
    public int countMensajesDirectos(String usuarioId1, String usuarioId2) {
        String sql = "SELECT COUNT(*) FROM mensajes " +
                     "WHERE tipo = 'DIRECT' " +
                     "  AND ((remitente_id = ? AND destinatario_id = ?) " +
                     "    OR (remitente_id = ? AND destinatario_id = ?))";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, usuarioId1);
            stmt.setString(2, usuarioId2);
            stmt.setString(3, usuarioId2);
            stmt.setString(4, usuarioId1);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al contar mensajes directos: {}", e.getMessage(), e);
            throw new RepositoryException("Error al contar mensajes directos", "MESSAGE_COUNT_DIRECT_ERROR", e);
        }
        return 0;
    }

    @Override
    public int countMensajesCanal(String canalId) {
        String sql = "SELECT COUNT(*) FROM mensajes WHERE tipo = 'CHANNEL' AND canal_id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, canalId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al contar mensajes de canal: {}", e.getMessage(), e);
            throw new RepositoryException("Error al contar mensajes de canal", "MESSAGE_COUNT_CHANNEL_ERROR", e);
        }
        return 0;
    }

    @Override
    public MensajeEntity save(MensajeEntity mensaje) {
        String sql = "INSERT INTO mensajes (remitente_id, destinatario_id, canal_id, tipo, contenido, file_id, fecha_envio) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // Establecer fecha de envío si no está definida
            if (mensaje.getFechaEnvio() == null) {
                mensaje.setFechaEnvio(LocalDateTime.now());
            }

            stmt.setString(1, mensaje.getRemitenteId());
            stmt.setString(2, mensaje.getDestinatarioId());
            stmt.setString(3, mensaje.getCanalId());
            stmt.setString(4, mensaje.getTipo() != null ? mensaje.getTipo().name() : "DIRECT");
            stmt.setString(5, mensaje.getContenido());
            stmt.setString(6, mensaje.getFileId());
            stmt.setTimestamp(7, Timestamp.valueOf(mensaje.getFechaEnvio()));

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creación de mensaje fallida, no se afectaron filas.");
            }

            // Recuperar el ID generado automáticamente
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    mensaje.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Creación de mensaje fallida, no se obtuvo ID.");
                }
            }
            
            logger.debug("Mensaje guardado exitosamente con ID: {}", mensaje.getId());
            return mensaje;
        } catch (SQLException e) {
            logger.error("Error al guardar mensaje en la base de datos: {}", e.getMessage(), e);
            throw new RepositoryException("Error al guardar mensaje", "MESSAGE_SAVE_ERROR", e);
        }
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM mensajes WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                logger.warn("No se encontró mensaje para eliminar con ID: {}", id);
                throw new RepositoryException("No se encontró mensaje para eliminar", "MESSAGE_NOT_FOUND_FOR_DELETE");
            }
            logger.debug("Mensaje eliminado exitosamente con ID: {}", id);
        } catch (SQLException e) {
            logger.error("Error al eliminar mensaje de la base de datos: {}", e.getMessage(), e);
            throw new RepositoryException("Error al eliminar mensaje", "MESSAGE_DELETE_ERROR", e);
        }
    }

    @Override
    public void actualizarEstado(Long mensajeId, EstadoMensaje nuevoEstado) {
        String sql = "UPDATE mensajes SET estado = ? WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, nuevoEstado.toString());
            stmt.setLong(2, mensajeId);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                logger.warn("No se encontró mensaje con ID: {} para actualizar estado", mensajeId);
            } else {
                logger.debug("Estado de mensaje {} actualizado a {}", mensajeId, nuevoEstado);
            }
            
        } catch (SQLException e) {
            logger.error("Error al actualizar estado del mensaje {}: {}", mensajeId, e.getMessage(), e);
            throw new RepositoryException("Error al actualizar estado del mensaje", "MESSAGE_STATE_UPDATE_ERROR", e);
        }
    }

    @Override
    public void marcarComoEntregado(Long mensajeId) {
        String sql = "UPDATE mensajes SET estado = ?, fecha_entrega = ? WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, EstadoMensaje.ENTREGADO.toString());
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setLong(3, mensajeId);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                logger.warn("No se encontró mensaje con ID: {} para marcar como entregado", mensajeId);
            } else {
                logger.debug("Mensaje {} marcado como entregado", mensajeId);
            }
            
        } catch (SQLException e) {
            logger.error("Error al marcar mensaje {} como entregado: {}", mensajeId, e.getMessage(), e);
            throw new RepositoryException("Error al marcar mensaje como entregado", "MESSAGE_DELIVERY_ERROR", e);
        }
    }

    @Override
    public void marcarComoLeido(Long mensajeId, String usuarioId) {
        String sql = "UPDATE mensajes SET estado = ?, fecha_lectura = ? WHERE id = ? AND destinatario_id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, EstadoMensaje.LEIDO.toString());
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setLong(3, mensajeId);
            stmt.setString(4, usuarioId);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                logger.warn("No se encontró mensaje {} para usuario {} o no es el destinatario", mensajeId, usuarioId);
            } else {
                logger.debug("Mensaje {} marcado como leído por usuario {}", mensajeId, usuarioId);
            }
            
        } catch (SQLException e) {
            logger.error("Error al marcar mensaje {} como leído por usuario {}: {}", mensajeId, usuarioId, e.getMessage(), e);
            throw new RepositoryException("Error al marcar mensaje como leído", "MESSAGE_READ_ERROR", e);
        }
    }

    @Override
    public List<Long> obtenerMensajesNoLeidos(String usuarioId) {
        String sql = "SELECT id FROM mensajes WHERE destinatario_id = ? AND estado IN ('ENVIADO', 'ENTREGADO') ORDER BY fecha_envio DESC";
        List<Long> mensajesNoLeidos = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, usuarioId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    mensajesNoLeidos.add(rs.getLong("id"));
                }
            }
            
            logger.debug("Encontrados {} mensajes no leídos para usuario {}", mensajesNoLeidos.size(), usuarioId);
            
        } catch (SQLException e) {
            logger.error("Error al obtener mensajes no leídos para usuario {}: {}", usuarioId, e.getMessage(), e);
            throw new RepositoryException("Error al obtener mensajes no leídos", "MESSAGE_UNREAD_QUERY_ERROR", e);
        }
        
        return mensajesNoLeidos;
    }

    @Override
    public int contarMensajesNoLeidos(String usuarioId) {
        String sql = "SELECT COUNT(*) FROM mensajes WHERE destinatario_id = ? AND estado IN ('ENVIADO', 'ENTREGADO')";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, usuarioId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    logger.debug("Usuario {} tiene {} mensajes no leídos", usuarioId, count);
                    return count;
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error al contar mensajes no leídos para usuario {}: {}", usuarioId, e.getMessage(), e);
            throw new RepositoryException("Error al contar mensajes no leídos", "MESSAGE_UNREAD_COUNT_ERROR", e);
        }
        
        return 0;
    }

    @Override
    public int countByTipoHoy(TipoMensaje tipo) {
        String sql = "SELECT COUNT(*) FROM mensajes WHERE tipo = ? AND DATE(fecha_envio) = CURDATE()";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, tipo.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    logger.debug("Mensajes de tipo {} enviados hoy: {}", tipo, count);
                    return count;
                }
            }

        } catch (SQLException e) {
            logger.error("Error al contar mensajes por tipo hoy: {}", e.getMessage(), e);
            throw new RepositoryException("Error al contar mensajes por tipo", "MESSAGE_COUNT_BY_TYPE_ERROR", e);
        }

        return 0;
    }
}

