package com.unillanos.server.repository.impl;

import com.unillanos.server.exception.RepositoryException;
import com.unillanos.server.repository.interfaces.ILogRepository;
import com.unillanos.server.repository.mappers.LogMapper;
import com.unillanos.server.entity.LogEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación del repositorio de logs usando JDBC puro.
 * Persiste y consulta logs del sistema en la tabla logs_sistema.
 */
@Repository
public class LogRepositoryImpl implements ILogRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(LogRepositoryImpl.class);
    private final DataSource dataSource;

    public LogRepositoryImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void save(LogEntity log) {
        String sql = "INSERT INTO logs_sistema (timestamp, tipo, usuario_id, ip_address, accion, detalles) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(sql);
            
            stmt.setTimestamp(1, Timestamp.valueOf(log.getTimestamp()));
            stmt.setString(2, log.getTipo());
            stmt.setString(3, log.getUsuarioId());
            stmt.setString(4, log.getIpAddress());
            stmt.setString(5, log.getAccion());
            stmt.setString(6, log.getDetalles());
            
            stmt.executeUpdate();
            logger.debug("Log guardado: tipo={}, accion={}", log.getTipo(), log.getAccion());
            
        } catch (SQLException e) {
            logger.error("Error al guardar log", e);
            throw new RepositoryException("Error al guardar log en la base de datos", "save", e);
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    @Override
    public List<LogEntity> findByTipo(String tipo, int limit) {
        String sql = "SELECT * FROM logs_sistema WHERE tipo = ? ORDER BY timestamp DESC LIMIT ?";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, tipo);
            stmt.setInt(2, limit);
            
            rs = stmt.executeQuery();
            return mapResults(rs);
            
        } catch (SQLException e) {
            logger.error("Error al buscar logs por tipo", e);
            throw new RepositoryException("Error al buscar logs por tipo", "findByTipo", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    @Override
    public List<LogEntity> findByUsuarioId(String usuarioId, int limit) {
        String sql = "SELECT * FROM logs_sistema WHERE usuario_id = ? ORDER BY timestamp DESC LIMIT ?";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, usuarioId);
            stmt.setInt(2, limit);
            
            rs = stmt.executeQuery();
            return mapResults(rs);
            
        } catch (SQLException e) {
            logger.error("Error al buscar logs por usuario", e);
            throw new RepositoryException("Error al buscar logs por usuario", "findByUsuarioId", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    @Override
    public List<LogEntity> findRecent(int limit) {
        String sql = "SELECT * FROM logs_sistema ORDER BY timestamp DESC LIMIT ?";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, limit);
            
            rs = stmt.executeQuery();
            return mapResults(rs);
            
        } catch (SQLException e) {
            logger.error("Error al buscar logs recientes", e);
            throw new RepositoryException("Error al buscar logs recientes", "findRecent", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    /**
     * Mapea un ResultSet a una lista de LogEntity.
     *
     * @param rs ResultSet a mapear
     * @return Lista de LogEntity
     * @throws SQLException si hay error al leer el ResultSet
     */
    private List<LogEntity> mapResults(ResultSet rs) throws SQLException {
        List<LogEntity> logs = new ArrayList<>();
        while (rs.next()) {
            logs.add(LogMapper.mapRow(rs));
        }
        return logs;
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

