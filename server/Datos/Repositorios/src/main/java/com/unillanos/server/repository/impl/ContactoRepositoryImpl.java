package com.unillanos.server.repository.impl;

import com.unillanos.server.entity.ContactoEntity;
import com.unillanos.server.exception.RepositoryException;
import com.unillanos.server.repository.interfaces.IContactoRepository;
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
 * Implementación del repositorio de contactos.
 */
@Repository
public class ContactoRepositoryImpl implements IContactoRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(ContactoRepositoryImpl.class);
    
    private final DataSource dataSource;

    public ContactoRepositoryImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public ContactoEntity save(ContactoEntity contacto) {
        String sql = "INSERT INTO contactos (id, usuario_id, contacto_id, estado, fecha_solicitud, fecha_respuesta, solicitado_por) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, contacto.getId());
            stmt.setString(2, contacto.getUsuarioId());
            stmt.setString(3, contacto.getContactoId());
            stmt.setString(4, contacto.getEstado());
            stmt.setTimestamp(5, Timestamp.valueOf(contacto.getFechaSolicitud()));
            stmt.setTimestamp(6, contacto.getFechaRespuesta() != null ? Timestamp.valueOf(contacto.getFechaRespuesta()) : null);
            stmt.setString(7, contacto.getSolicitadoPor());
            
            stmt.executeUpdate();
            logger.debug("Contacto guardado: {}", contacto.getId());
            
            return contacto;
            
        } catch (SQLException e) {
            logger.error("Error al guardar contacto: {}", e.getMessage(), e);
            throw new RepositoryException("Error al guardar contacto", "save", e);
        }
    }

    @Override
    public Optional<ContactoEntity> findById(String id) {
        String sql = "SELECT * FROM contactos WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error al buscar contacto por ID: {}", e.getMessage(), e);
            throw new RepositoryException("Error al buscar contacto", "findById", e);
        }
        
        return Optional.empty();
    }

    @Override
    public Optional<ContactoEntity> findByUsuarios(String usuarioId, String contactoId) {
        String sql = "SELECT * FROM contactos WHERE (usuario_id = ? AND contacto_id = ?) OR (usuario_id = ? AND contacto_id = ?)";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, usuarioId);
            stmt.setString(2, contactoId);
            stmt.setString(3, contactoId);
            stmt.setString(4, usuarioId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error al buscar contacto por usuarios: {}", e.getMessage(), e);
            throw new RepositoryException("Error al buscar contacto", "findByUsuarios", e);
        }
        
        return Optional.empty();
    }

    @Override
    public List<ContactoEntity> findContactosActivos(String usuarioId) {
        String sql = "SELECT * FROM contactos WHERE (usuario_id = ? OR contacto_id = ?) AND estado = 'ACEPTADO' ORDER BY fecha_solicitud DESC";
        return findContactos(sql, usuarioId, usuarioId);
    }

    @Override
    public List<ContactoEntity> findSolicitudesEnviadas(String usuarioId) {
        String sql = "SELECT * FROM contactos WHERE usuario_id = ? AND estado = 'PENDIENTE' ORDER BY fecha_solicitud DESC";
        return findContactos(sql, usuarioId);
    }

    @Override
    public List<ContactoEntity> findSolicitudesRecibidas(String usuarioId) {
        String sql = "SELECT * FROM contactos WHERE contacto_id = ? AND estado = 'PENDIENTE' ORDER BY fecha_solicitud DESC";
        return findContactos(sql, usuarioId);
    }

    @Override
    public boolean sonContactos(String usuarioId, String contactoId) {
        String sql = "SELECT COUNT(*) FROM contactos WHERE (usuario_id = ? AND contacto_id = ?) OR (usuario_id = ? AND contacto_id = ?) AND estado = 'ACEPTADO'";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, usuarioId);
            stmt.setString(2, contactoId);
            stmt.setString(3, contactoId);
            stmt.setString(4, usuarioId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            logger.error("Error al verificar si son contactos: {}", e.getMessage(), e);
            throw new RepositoryException("Error al verificar contactos", "sonContactos", e);
        }
        
        return false;
    }

    @Override
    public boolean tieneSolicitudPendiente(String usuarioId, String contactoId) {
        String sql = "SELECT COUNT(*) FROM contactos WHERE ((usuario_id = ? AND contacto_id = ?) OR (usuario_id = ? AND contacto_id = ?)) AND estado = 'PENDIENTE'";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, usuarioId);
            stmt.setString(2, contactoId);
            stmt.setString(3, contactoId);
            stmt.setString(4, usuarioId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            logger.error("Error al verificar solicitud pendiente: {}", e.getMessage(), e);
            throw new RepositoryException("Error al verificar solicitud", "tieneSolicitudPendiente", e);
        }
        
        return false;
    }

    @Override
    public void actualizarEstado(String id, String nuevoEstado) {
        String sql = "UPDATE contactos SET estado = ?, fecha_respuesta = ? WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, nuevoEstado);
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(3, id);
            
            stmt.executeUpdate();
            logger.debug("Estado de contacto actualizado: {} -> {}", id, nuevoEstado);
            
        } catch (SQLException e) {
            logger.error("Error al actualizar estado de contacto: {}", e.getMessage(), e);
            throw new RepositoryException("Error al actualizar estado", "actualizarEstado", e);
        }
    }

    @Override
    public void delete(String id) {
        String sql = "DELETE FROM contactos WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, id);
            stmt.executeUpdate();
            logger.debug("Contacto eliminado: {}", id);
            
        } catch (SQLException e) {
            logger.error("Error al eliminar contacto: {}", e.getMessage(), e);
            throw new RepositoryException("Error al eliminar contacto", "delete", e);
        }
    }

    private List<ContactoEntity> findContactos(String sql, String... params) {
        List<ContactoEntity> contactos = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            for (int i = 0; i < params.length; i++) {
                stmt.setString(i + 1, params[i]);
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    contactos.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error al buscar contactos: {}", e.getMessage(), e);
            throw new RepositoryException("Error al buscar contactos", "findContactos", e);
        }
        
        return contactos;
    }

    private ContactoEntity mapRow(ResultSet rs) throws SQLException {
        ContactoEntity contacto = new ContactoEntity();
        contacto.setId(rs.getString("id"));
        contacto.setUsuarioId(rs.getString("usuario_id"));
        contacto.setContactoId(rs.getString("contacto_id"));
        contacto.setEstado(rs.getString("estado"));
        contacto.setFechaSolicitud(rs.getTimestamp("fecha_solicitud").toLocalDateTime());
        
        Timestamp fechaRespuesta = rs.getTimestamp("fecha_respuesta");
        if (fechaRespuesta != null) {
            contacto.setFechaRespuesta(fechaRespuesta.toLocalDateTime());
        }
        
        contacto.setSolicitadoPor(rs.getString("solicitado_por"));
        return contacto;
    }
}
