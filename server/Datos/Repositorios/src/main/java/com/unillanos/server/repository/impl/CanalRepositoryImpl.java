package com.unillanos.server.repository.impl;

import com.unillanos.server.exception.RepositoryException;
import com.unillanos.server.repository.interfaces.ICanalRepository;
import com.unillanos.server.repository.mappers.CanalMapper;
import com.unillanos.server.repository.models.CanalEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación del repositorio de canales usando JDBC puro.
 * Maneja todas las operaciones de persistencia de canales en la base de datos.
 */
@Repository
public class CanalRepositoryImpl implements ICanalRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(CanalRepositoryImpl.class);
    private final DataSource dataSource;

    public CanalRepositoryImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // --- MÉTODOS DE CONSULTA ---

    @Override
    public Optional<CanalEntity> findById(String id) {
        String sql = "SELECT * FROM canales WHERE id = ?";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, id);
            
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(CanalMapper.mapRow(rs));
            }
            return Optional.empty();
            
        } catch (SQLException e) {
            logger.error("Error al buscar canal por ID: {}", id, e);
            throw new RepositoryException("Error al buscar canal por ID", "findById", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    @Override
    public Optional<CanalEntity> findByNombre(String nombre) {
        String sql = "SELECT * FROM canales WHERE nombre = ?";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, nombre);
            
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(CanalMapper.mapRow(rs));
            }
            return Optional.empty();
            
        } catch (SQLException e) {
            logger.error("Error al buscar canal por nombre: {}", nombre, e);
            throw new RepositoryException("Error al buscar canal por nombre", "findByNombre", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    @Override
    public boolean existsByNombre(String nombre) {
        String sql = "SELECT COUNT(*) FROM canales WHERE nombre = ?";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, nombre);
            
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
            
        } catch (SQLException e) {
            logger.error("Error al verificar existencia de canal por nombre: {}", nombre, e);
            throw new RepositoryException("Error al verificar existencia de canal", "existsByNombre", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    @Override
    public List<CanalEntity> findAll(int limit, int offset) {
        String sql = "SELECT * FROM canales WHERE activo = true ORDER BY fecha_creacion DESC LIMIT ? OFFSET ?";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, limit);
            stmt.setInt(2, offset);
            
            rs = stmt.executeQuery();
            return mapResults(rs);
            
        } catch (SQLException e) {
            logger.error("Error al listar canales con paginación", e);
            throw new RepositoryException("Error al listar canales", "findAll", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    @Override
    public List<CanalEntity> findByUsuario(String usuarioId, int limit, int offset) {
        String sql = "SELECT c.* FROM canales c " +
                     "INNER JOIN canal_miembros cm ON c.id = cm.canal_id " +
                     "WHERE cm.usuario_id = ? AND c.activo = true " +
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
            return mapResults(rs);
            
        } catch (SQLException e) {
            logger.error("Error al listar canales del usuario: {}", usuarioId, e);
            throw new RepositoryException("Error al listar canales del usuario", "findByUsuario", e);
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
    public CanalEntity save(CanalEntity canal) {
        String sql = "INSERT INTO canales (id, nombre, descripcion, creador_id, fecha_creacion, activo) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(sql);
            
            stmt.setString(1, canal.getId());
            stmt.setString(2, canal.getNombre());
            stmt.setString(3, canal.getDescripcion());
            stmt.setString(4, canal.getCreadorId());
            stmt.setTimestamp(5, Timestamp.valueOf(canal.getFechaCreacion()));
            stmt.setBoolean(6, canal.isActivo());
            
            stmt.executeUpdate();
            logger.debug("Canal guardado: id={}, nombre={}", canal.getId(), canal.getNombre());
            
            return canal;
            
        } catch (SQLException e) {
            logger.error("Error al guardar canal: {}", canal.getNombre(), e);
            throw new RepositoryException("Error al guardar canal", "save", e);
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    @Override
    public void update(CanalEntity canal) {
        String sql = "UPDATE canales SET nombre = ?, descripcion = ? WHERE id = ?";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(sql);
            
            stmt.setString(1, canal.getNombre());
            stmt.setString(2, canal.getDescripcion());
            stmt.setString(3, canal.getId());
            
            int rowsAffected = stmt.executeUpdate();
            logger.debug("Canal actualizado: id={}, rows={}", canal.getId(), rowsAffected);
            
        } catch (SQLException e) {
            logger.error("Error al actualizar canal: {}", canal.getId(), e);
            throw new RepositoryException("Error al actualizar canal", "update", e);
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    @Override
    public void updateActivo(String id, boolean activo) {
        String sql = "UPDATE canales SET activo = ? WHERE id = ?";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setBoolean(1, activo);
            stmt.setString(2, id);
            
            int rowsAffected = stmt.executeUpdate();
            logger.debug("Estado activo actualizado: id={}, activo={}, rows={}", id, activo, rowsAffected);
            
        } catch (SQLException e) {
            logger.error("Error al actualizar estado activo del canal: {}", id, e);
            throw new RepositoryException("Error al actualizar estado activo", "updateActivo", e);
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    @Override
    public void deleteById(String id) {
        String sql = "DELETE FROM canales WHERE id = ?";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, id);
            
            int rowsAffected = stmt.executeUpdate();
            logger.debug("Canal eliminado: id={}, rows={}", id, rowsAffected);
            
        } catch (SQLException e) {
            logger.error("Error al eliminar canal: {}", id, e);
            throw new RepositoryException("Error al eliminar canal", "deleteById", e);
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    // --- MÉTODOS AUXILIARES ---

    /**
     * Mapea un ResultSet a una lista de CanalEntity.
     *
     * @param rs ResultSet a mapear
     * @return Lista de CanalEntity
     * @throws SQLException si hay error al leer el ResultSet
     */
    private List<CanalEntity> mapResults(ResultSet rs) throws SQLException {
        List<CanalEntity> canales = new ArrayList<>();
        while (rs.next()) {
            canales.add(CanalMapper.mapRow(rs));
        }
        return canales;
    }

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

