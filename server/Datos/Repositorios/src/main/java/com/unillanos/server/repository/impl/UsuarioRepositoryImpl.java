package com.unillanos.server.repository.impl;

import com.unillanos.server.exception.RepositoryException;
import com.unillanos.server.repository.interfaces.IUsuarioRepository;
import com.unillanos.server.repository.mappers.UsuarioMapper;
import com.unillanos.server.repository.models.EstadoUsuario;
import com.unillanos.server.repository.models.UsuarioEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementación del repositorio de usuarios usando JDBC puro.
 * Maneja todas las operaciones de persistencia de usuarios en la base de datos.
 */
@Repository
public class UsuarioRepositoryImpl implements IUsuarioRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(UsuarioRepositoryImpl.class);
    private final DataSource dataSource;

    public UsuarioRepositoryImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // --- MÉTODOS DE CONSULTA ---

    @Override
    public Optional<UsuarioEntity> findById(String id) {
        String sql = "SELECT * FROM usuarios WHERE id = ?";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, id);
            
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(UsuarioMapper.mapRow(rs));
            }
            return Optional.empty();
            
        } catch (SQLException e) {
            logger.error("Error al buscar usuario por ID: {}", id, e);
            throw new RepositoryException("Error al buscar usuario por ID", "findById", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    @Override
    public Optional<UsuarioEntity> findByEmail(String email) {
        String sql = "SELECT * FROM usuarios WHERE email = ?";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(UsuarioMapper.mapRow(rs));
            }
            return Optional.empty();
            
        } catch (SQLException e) {
            logger.error("Error al buscar usuario por email: {}", email, e);
            throw new RepositoryException("Error al buscar usuario por email", "findByEmail", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    @Override
    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM usuarios WHERE email = ?";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
            
        } catch (SQLException e) {
            logger.error("Error al verificar existencia de email: {}", email, e);
            throw new RepositoryException("Error al verificar existencia de email", "existsByEmail", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    @Override
    public List<UsuarioEntity> findAll(int limit, int offset) {
        String sql = "SELECT * FROM usuarios ORDER BY fecha_registro DESC LIMIT ? OFFSET ?";
        
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
            logger.error("Error al listar usuarios con paginación", e);
            throw new RepositoryException("Error al listar usuarios", "findAll", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    @Override
    public List<UsuarioEntity> findAll() {
        String sql = "SELECT * FROM usuarios ORDER BY fecha_registro DESC";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(sql);
            
            rs = stmt.executeQuery();
            return mapResults(rs);
            
        } catch (SQLException e) {
            logger.error("Error al listar todos los usuarios", e);
            throw new RepositoryException("Error al listar usuarios", "findAll", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }

    @Override
    public List<UsuarioEntity> findByEstado(EstadoUsuario estado, int limit, int offset) {
        List<UsuarioEntity> usuarios = new ArrayList<>();
        String sql = "SELECT * FROM usuarios WHERE estado = ? ORDER BY fecha_registro DESC LIMIT ? OFFSET ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, estado.name());
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    usuarios.add(UsuarioMapper.mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error al buscar usuarios por estado: {}", e.getMessage(), e);
            throw new RepositoryException("Error al buscar usuarios por estado", "USER_FIND_BY_STATUS_ERROR", e);
        }
        return usuarios;
    }

    @Override
    public int countAll() {
        String sql = "SELECT COUNT(*) FROM usuarios";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error al contar usuarios: {}", e.getMessage(), e);
            throw new RepositoryException("Error al contar usuarios", "USER_COUNT_ALL_ERROR", e);
        }
        return 0;
    }

    @Override
    public int countByEstado(EstadoUsuario estado) {
        String sql = "SELECT COUNT(*) FROM usuarios WHERE estado = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, estado.name());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al contar usuarios por estado: {}", e.getMessage(), e);
            throw new RepositoryException("Error al contar usuarios por estado", "USER_COUNT_BY_STATUS_ERROR", e);
        }
        return 0;
    }

    // --- MÉTODOS DE ESCRITURA ---

    @Override
    public UsuarioEntity save(UsuarioEntity usuario) {
        String sql = "INSERT INTO usuarios (id, nombre, email, password_hash, photo_id, ip_address, fecha_registro, estado) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(sql);
            
            stmt.setString(1, usuario.getId());
            stmt.setString(2, usuario.getNombre());
            stmt.setString(3, usuario.getEmail());
            stmt.setString(4, usuario.getPasswordHash());
            stmt.setString(5, usuario.getPhotoId());
            stmt.setString(6, usuario.getIpAddress());
            stmt.setTimestamp(7, Timestamp.valueOf(usuario.getFechaRegistro()));
            stmt.setString(8, usuario.getEstado().name());
            
            stmt.executeUpdate();
            logger.debug("Usuario guardado: id={}, email={}", usuario.getId(), usuario.getEmail());
            
            return usuario;
            
        } catch (SQLException e) {
            logger.error("Error al guardar usuario: {}", usuario.getEmail(), e);
            throw new RepositoryException("Error al guardar usuario", "save", e);
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    @Override
    public void update(UsuarioEntity usuario) {
        String sql = "UPDATE usuarios SET nombre = ?, photo_id = ?, password_hash = ? WHERE id = ?";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(sql);
            
            stmt.setString(1, usuario.getNombre());
            stmt.setString(2, usuario.getPhotoId());
            stmt.setString(3, usuario.getPasswordHash());
            stmt.setString(4, usuario.getId());
            
            int rowsAffected = stmt.executeUpdate();
            logger.debug("Usuario actualizado: id={}, rows={}", usuario.getId(), rowsAffected);
            
        } catch (SQLException e) {
            logger.error("Error al actualizar usuario: {}", usuario.getId(), e);
            throw new RepositoryException("Error al actualizar usuario", "update", e);
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    @Override
    public void updateEstado(String id, EstadoUsuario estado) {
        String sql = "UPDATE usuarios SET estado = ? WHERE id = ?";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, estado.name());
            stmt.setString(2, id);
            
            int rowsAffected = stmt.executeUpdate();
            logger.debug("Estado actualizado: id={}, estado={}, rows={}", id, estado, rowsAffected);
            
        } catch (SQLException e) {
            logger.error("Error al actualizar estado del usuario: {}", id, e);
            throw new RepositoryException("Error al actualizar estado", "updateEstado", e);
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    @Override
    public void updateIpAddress(String id, String ipAddress) {
        String sql = "UPDATE usuarios SET ip_address = ? WHERE id = ?";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, ipAddress);
            stmt.setString(2, id);
            
            int rowsAffected = stmt.executeUpdate();
            logger.debug("IP actualizada: id={}, ip={}, rows={}", id, ipAddress, rowsAffected);
            
        } catch (SQLException e) {
            logger.error("Error al actualizar IP del usuario: {}", id, e);
            throw new RepositoryException("Error al actualizar IP", "updateIpAddress", e);
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    @Override
    public void deleteById(String id) {
        String sql = "DELETE FROM usuarios WHERE id = ?";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, id);
            
            int rowsAffected = stmt.executeUpdate();
            logger.debug("Usuario eliminado: id={}, rows={}", id, rowsAffected);
            
        } catch (SQLException e) {
            logger.error("Error al eliminar usuario: {}", id, e);
            throw new RepositoryException("Error al eliminar usuario", "deleteById", e);
        } finally {
            closeResources(conn, stmt, null);
        }
    }

    // --- MÉTODOS AUXILIARES ---

    /**
     * Mapea un ResultSet a una lista de UsuarioEntity.
     *
     * @param rs ResultSet a mapear
     * @return Lista de UsuarioEntity
     * @throws SQLException si hay error al leer el ResultSet
     */
    private List<UsuarioEntity> mapResults(ResultSet rs) throws SQLException {
        List<UsuarioEntity> usuarios = new ArrayList<>();
        while (rs.next()) {
            usuarios.add(UsuarioMapper.mapRow(rs));
        }
        return usuarios;
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

    @Override
    public List<Map<String, Object>> getTopActivos(int limit) {
        String sql = """
            SELECT u.nombre_usuario as nombre, COUNT(m.id) as mensajes
            FROM usuarios u
            LEFT JOIN mensajes m ON u.id = m.remitente_id 
                AND DATE(m.fecha_envio) = CURDATE()
            GROUP BY u.id, u.nombre_usuario
            HAVING COUNT(m.id) > 0
            ORDER BY mensajes DESC
            LIMIT ?
            """;

        List<Map<String, Object>> topUsuarios = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> usuario = new HashMap<>();
                    usuario.put("nombre", rs.getString("nombre"));
                    usuario.put("mensajes", rs.getInt("mensajes"));
                    topUsuarios.add(usuario);
                }
            }

            logger.debug("Encontrados {} usuarios activos", topUsuarios.size());

        } catch (SQLException e) {
            logger.error("Error al obtener usuarios más activos: {}", e.getMessage(), e);
            throw new RepositoryException("Error al obtener usuarios más activos", "TOP_ACTIVE_USERS_ERROR", e);
        }

        return topUsuarios;
    }
}
