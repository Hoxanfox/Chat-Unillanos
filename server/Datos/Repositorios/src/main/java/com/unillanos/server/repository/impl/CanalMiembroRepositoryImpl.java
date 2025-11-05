package com.unillanos.server.repository.impl;

import com.unillanos.server.exception.RepositoryException;
import com.unillanos.server.repository.interfaces.ICanalMiembroRepository;
import com.unillanos.server.repository.mappers.CanalMapper;
import com.unillanos.server.repository.mappers.CanalMiembroMapper;
import com.unillanos.server.entity.CanalEntity;
import com.unillanos.server.entity.CanalMiembroEntity;
import com.unillanos.server.entity.RolCanal;
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
 * Implementación del repositorio de miembros de canales usando JDBC puro.
 * Maneja la relación N:M entre usuarios y canales.
 */
@Repository
public class CanalMiembroRepositoryImpl implements ICanalMiembroRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(CanalMiembroRepositoryImpl.class);
    private final DataSource dataSource;

    public CanalMiembroRepositoryImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // --- MÉTODOS DE CONSULTA ---

    @Override
    public Optional<CanalMiembroEntity> findByUsuarioAndCanal(String usuarioId, String canalId) {
        String sql = "SELECT * FROM canal_miembros WHERE usuario_id = ? AND canal_id = ?";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, usuarioId);
            stmt.setString(2, canalId);
            
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(CanalMiembroMapper.mapRow(rs));
            }
            return Optional.empty();
            
        } catch (SQLException e) {
            logger.error("Error al buscar miembro: usuario={}, canal={}", usuarioId, canalId, e);
            throw new RepositoryException("Error al buscar miembro", "findByUsuarioAndCanal", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    @Override
    public List<CanalMiembroEntity> findMiembrosByCanal(String canalId) {
        String sql = "SELECT * FROM canal_miembros WHERE canal_id = ? ORDER BY fecha_union ASC";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, canalId);
            
            rs = stmt.executeQuery();
            
            List<CanalMiembroEntity> miembros = new ArrayList<>();
            while (rs.next()) {
                miembros.add(CanalMiembroMapper.mapRow(rs));
            }
            return miembros;
            
        } catch (SQLException e) {
            logger.error("Error al listar miembros del canal: {}", canalId, e);
            throw new RepositoryException("Error al listar miembros", "findMiembrosByCanal", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    @Override
    public List<CanalEntity> findCanalesByUsuario(String usuarioId, int limit, int offset) {
        String sql = "SELECT c.* FROM canales c " +
                     "INNER JOIN canal_miembros cm ON c.id = cm.canal_id " +
                     "WHERE cm.usuario_id = ? " +
                     "ORDER BY cm.fecha_union DESC LIMIT ? OFFSET ?";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, usuarioId);
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);
            
            rs = stmt.executeQuery();
            
            List<CanalEntity> canales = new ArrayList<>();
            while (rs.next()) {
                canales.add(CanalMapper.mapRow(rs));
            }
            return canales;
            
        } catch (SQLException e) {
            logger.error("Error al listar canales del usuario: {}", usuarioId, e);
            throw new RepositoryException("Error al listar canales del usuario", "findCanalesByUsuario", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    @Override
    public boolean esAdministrador(String usuarioId, String canalId) {
        String sql = "SELECT COUNT(*) FROM canal_miembros " +
                     "WHERE usuario_id = ? AND canal_id = ? AND rol = 'ADMIN'";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, usuarioId);
            stmt.setString(2, canalId);
            
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
            
        } catch (SQLException e) {
            logger.error("Error al verificar si es administrador: usuario={}, canal={}", usuarioId, canalId, e);
            throw new RepositoryException("Error al verificar rol de administrador", "esAdministrador", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    @Override
    public boolean esMiembro(String usuarioId, String canalId) {
        String sql = "SELECT COUNT(*) FROM canal_miembros WHERE usuario_id = ? AND canal_id = ?";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, usuarioId);
            stmt.setString(2, canalId);
            
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
            
        } catch (SQLException e) {
            logger.error("Error al verificar si es miembro: usuario={}, canal={}", usuarioId, canalId, e);
            throw new RepositoryException("Error al verificar membresía", "esMiembro", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    @Override
    public int countMiembros(String canalId) {
        String sql = "SELECT COUNT(*) FROM canal_miembros WHERE canal_id = ?";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, canalId);
            
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
            
        } catch (SQLException e) {
            logger.error("Error al contar miembros del canal: {}", canalId, e);
            throw new RepositoryException("Error al contar miembros", "countMiembros", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    // --- MÉTODOS DE ESCRITURA ---

    @Override
    public void agregarMiembro(String canalId, String usuarioId, RolCanal rol) {
        String sql = "INSERT INTO canal_miembros (canal_id, usuario_id, fecha_union, rol) " +
                     "VALUES (?, ?, ?, ?)";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(sql);
            
            stmt.setString(1, canalId);
            stmt.setString(2, usuarioId);
            stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(4, rol.name());
            
            stmt.executeUpdate();
            logger.debug("Miembro agregado: canal={}, usuario={}, rol={}", canalId, usuarioId, rol);
            
        } catch (SQLException e) {
            logger.error("Error al agregar miembro: canal={}, usuario={}", canalId, usuarioId, e);
            throw new RepositoryException("Error al agregar miembro", "agregarMiembro", e);
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    @Override
    public void removerMiembro(String canalId, String usuarioId) {
        String sql = "DELETE FROM canal_miembros WHERE canal_id = ? AND usuario_id = ?";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, canalId);
            stmt.setString(2, usuarioId);
            
            int rowsAffected = stmt.executeUpdate();
            logger.debug("Miembro removido: canal={}, usuario={}, rows={}", canalId, usuarioId, rowsAffected);
            
        } catch (SQLException e) {
            logger.error("Error al remover miembro: canal={}, usuario={}", canalId, usuarioId, e);
            throw new RepositoryException("Error al remover miembro", "removerMiembro", e);
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    @Override
    public void actualizarRol(String canalId, String usuarioId, RolCanal nuevoRol) {
        String sql = "UPDATE canal_miembros SET rol = ? WHERE canal_id = ? AND usuario_id = ?";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, nuevoRol.name());
            stmt.setString(2, canalId);
            stmt.setString(3, usuarioId);
            
            int rowsAffected = stmt.executeUpdate();
            logger.debug("Rol actualizado: canal={}, usuario={}, rol={}, rows={}", 
                        canalId, usuarioId, nuevoRol, rowsAffected);
            
        } catch (SQLException e) {
            logger.error("Error al actualizar rol: canal={}, usuario={}", canalId, usuarioId, e);
            throw new RepositoryException("Error al actualizar rol", "actualizarRol", e);
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    // --- MÉTODOS AUXILIARES ---

    /**
     * Cierra los recursos JDBC de forma segura.
     *
     * @param conn Connection a cerrar
     * @param stmt Statement a cerrar
     * @param rs ResultSet a cerrar
     */
    private void closeResources(Connection conn, Statement stmt, ResultSet rs) {
        try {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        } catch (SQLException e) {
            logger.warn("Error al cerrar recursos JDBC", e);
        }
    }
}

