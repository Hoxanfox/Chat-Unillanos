package com.unillanos.server.repository.impl;

import com.unillanos.server.exception.RepositoryException;
import com.unillanos.server.repository.interfaces.IArchivoRepository;
import com.unillanos.server.repository.mappers.ArchivoMapper;
import com.unillanos.server.entity.ArchivoEntity;
import com.unillanos.server.entity.TipoArchivo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementación JDBC del IArchivoRepository para interactuar con la base de datos MySQL.
 */
@Repository
public class ArchivoRepositoryImpl implements IArchivoRepository {

    private static final Logger logger = LoggerFactory.getLogger(ArchivoRepositoryImpl.class);
    private final DataSource dataSource;

    public ArchivoRepositoryImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<ArchivoEntity> findById(String id) {
        String sql = "SELECT * FROM archivos WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(ArchivoMapper.mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error al buscar archivo por ID: {}", e.getMessage(), e);
            throw new RepositoryException("Error al buscar archivo por ID", "ARCHIVO_FIND_BY_ID_ERROR", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<ArchivoEntity> findByHash(String hashSha256) {
        String sql = "SELECT * FROM archivos WHERE hash_sha256 = ? LIMIT 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, hashSha256);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(ArchivoMapper.mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error al buscar archivo por hash: {}", e.getMessage(), e);
            throw new RepositoryException("Error al buscar archivo por hash", "ARCHIVO_FIND_BY_HASH_ERROR", e);
        }
        return Optional.empty();
    }

    @Override
    public List<ArchivoEntity> findByUsuario(String usuarioId, int limit, int offset) {
        List<ArchivoEntity> archivos = new ArrayList<>();
        String sql = "SELECT * FROM archivos WHERE usuario_id = ? ORDER BY fecha_subida DESC LIMIT ? OFFSET ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, usuarioId);
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    archivos.add(ArchivoMapper.mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error al buscar archivos por usuario: {}", e.getMessage(), e);
            throw new RepositoryException("Error al buscar archivos por usuario", "ARCHIVO_FIND_BY_USUARIO_ERROR", e);
        }
        return archivos;
    }

    @Override
    public List<ArchivoEntity> findByUsuarioYTipo(String usuarioId, TipoArchivo tipo, int limit, int offset) {
        List<ArchivoEntity> archivos = new ArrayList<>();
        String sql = "SELECT * FROM archivos WHERE usuario_id = ? AND tipo_archivo = ? ORDER BY fecha_subida DESC LIMIT ? OFFSET ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, usuarioId);
            stmt.setString(2, tipo.name());
            stmt.setInt(3, limit);
            stmt.setInt(4, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    archivos.add(ArchivoMapper.mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error al buscar archivos por usuario y tipo: {}", e.getMessage(), e);
            throw new RepositoryException("Error al buscar archivos por usuario y tipo", "ARCHIVO_FIND_BY_USUARIO_TIPO_ERROR", e);
        }
        return archivos;
    }

    @Override
    public int countByUsuario(String usuarioId) {
        String sql = "SELECT COUNT(*) FROM archivos WHERE usuario_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, usuarioId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al contar archivos por usuario: {}", e.getMessage(), e);
            throw new RepositoryException("Error al contar archivos por usuario", "ARCHIVO_COUNT_BY_USUARIO_ERROR", e);
        }
        return 0;
    }

    @Override
    public boolean existsByHash(String hashSha256) {
        String sql = "SELECT EXISTS(SELECT 1 FROM archivos WHERE hash_sha256 = ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, hashSha256);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al verificar existencia de archivo por hash: {}", e.getMessage(), e);
            throw new RepositoryException("Error al verificar existencia de archivo por hash", "ARCHIVO_EXISTS_BY_HASH_ERROR", e);
        }
        return false;
    }

    @Override
    public ArchivoEntity save(ArchivoEntity archivo) {
        String sql = "INSERT INTO archivos (id, nombre_original, nombre_almacenado, tipo_mime, tipo_archivo, hash_sha256, tamano_bytes, ruta_almacenamiento, usuario_id, fecha_subida) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Generar ID si no existe
            if (archivo.getId() == null) {
                archivo.setId(UUID.randomUUID().toString());
            }
            if (archivo.getFechaSubida() == null) {
                archivo.setFechaSubida(LocalDateTime.now());
            }

            stmt.setString(1, archivo.getId());
            stmt.setString(2, archivo.getNombreOriginal());
            stmt.setString(3, archivo.getNombreAlmacenado());
            stmt.setString(4, archivo.getTipoMime());
            stmt.setString(5, archivo.getTipoArchivo() != null ? archivo.getTipoArchivo().name() : null);
            stmt.setString(6, archivo.getHashSha256());
            stmt.setLong(7, archivo.getTamanoBytes());
            stmt.setString(8, archivo.getRutaAlmacenamiento());
            stmt.setString(9, archivo.getUsuarioId());
            stmt.setTimestamp(10, Timestamp.valueOf(archivo.getFechaSubida()));

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creación de archivo fallida, no se afectaron filas.");
            }
            logger.debug("Archivo guardado exitosamente con ID: {}", archivo.getId());
            return archivo;
        } catch (SQLException e) {
            logger.error("Error al guardar archivo en la base de datos: {}", e.getMessage(), e);
            throw new RepositoryException("Error al guardar archivo", "ARCHIVO_SAVE_ERROR", e);
        }
    }

    @Override
    public void deleteById(String id) {
        String sql = "DELETE FROM archivos WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                logger.warn("No se encontró archivo para eliminar con ID: {}", id);
                throw new RepositoryException("No se encontró archivo para eliminar", "ARCHIVO_NOT_FOUND_FOR_DELETE");
            }
            logger.debug("Archivo eliminado exitosamente con ID: {}", id);
        } catch (SQLException e) {
            logger.error("Error al eliminar archivo de la base de datos: {}", e.getMessage(), e);
            throw new RepositoryException("Error al eliminar archivo", "ARCHIVO_DELETE_ERROR", e);
        }
    }
}

