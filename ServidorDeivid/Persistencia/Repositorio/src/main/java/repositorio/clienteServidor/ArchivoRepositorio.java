package repositorio.clienteServidor;

import dominio.clienteServidor.Archivo;
import repositorio.comunicacion.MySQLManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ArchivoRepositorio {
    private final MySQLManager mysql;

    public ArchivoRepositorio() {
        this.mysql = MySQLManager.getInstance();
    }

    /**
     * Guarda o actualiza un archivo en la BD
     */
    public boolean guardar(Archivo archivo) {
        String sql = "INSERT INTO archivos (id, file_id, nombre_archivo, ruta_relativa, mime_type, tamanio, hash_sha256, fecha_creacion, fecha_actualizacion) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE nombre_archivo=VALUES(nombre_archivo), ruta_relativa=VALUES(ruta_relativa), " +
                "mime_type=VALUES(mime_type), tamanio=VALUES(tamanio), hash_sha256=VALUES(hash_sha256), fecha_actualizacion=VALUES(fecha_actualizacion)";

        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, archivo.getId().toString());
            ps.setString(2, archivo.getFileId());
            ps.setString(3, archivo.getNombreArchivo());
            ps.setString(4, archivo.getRutaRelativa());
            ps.setString(5, archivo.getMimeType());
            ps.setLong(6, archivo.getTamanio());
            ps.setString(7, archivo.getHashSHA256());
            ps.setTimestamp(8, Timestamp.from(archivo.getFechaCreacion()));
            ps.setTimestamp(9, Timestamp.from(archivo.getFechaUltimaActualizacion()));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[RepoArchivo] Error guardando: " + e.getMessage());
            return false;
        }
    }

    /**
     * Busca un archivo por su fileId (usado por clientes)
     */
    public Archivo buscarPorFileId(String fileId) {
        String sql = "SELECT * FROM archivos WHERE file_id = ?";
        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fileId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[RepoArchivo] Error buscando por fileId: " + e.getMessage());
        }
        return null;
    }

    /**
     * Busca un archivo por su UUID interno
     */
    public Archivo buscarPorId(UUID id) {
        String sql = "SELECT * FROM archivos WHERE id = ?";
        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[RepoArchivo] Error buscando por ID: " + e.getMessage());
        }
        return null;
    }

    /**
     * Verifica si un archivo existe por fileId
     */
    public boolean existe(String fileId) {
        String sql = "SELECT COUNT(*) FROM archivos WHERE file_id = ?";
        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fileId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("[RepoArchivo] Error verificando existencia: " + e.getMessage());
        }
        return false;
    }

    /**
     * Obtiene todos los archivos
     */
    public List<Archivo> obtenerTodos() {
        List<Archivo> lista = new ArrayList<>();
        String sql = "SELECT * FROM archivos ORDER BY fecha_creacion DESC";
        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            System.err.println("[RepoArchivo] Error obteniendo todos: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Obtiene todos los archivos para sincronización P2P
     */
    public List<Archivo> obtenerTodosParaSync() {
        return obtenerTodos();
    }

    /**
     * Elimina un archivo por fileId
     */
    public boolean eliminar(String fileId) {
        String sql = "DELETE FROM archivos WHERE file_id = ?";
        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fileId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[RepoArchivo] Error eliminando: " + e.getMessage());
            return false;
        }
    }

    private Archivo mapear(ResultSet rs) throws SQLException {
        Archivo a = new Archivo();
        a.setId(UUID.fromString(rs.getString("id")));
        a.setFileId(rs.getString("file_id"));
        a.setNombreArchivo(rs.getString("nombre_archivo"));
        a.setRutaRelativa(rs.getString("ruta_relativa"));
        a.setMimeType(rs.getString("mime_type"));
        a.setTamanio(rs.getLong("tamanio"));
        a.setHashSHA256(rs.getString("hash_sha256"));
        a.setFechaCreacion(rs.getTimestamp("fecha_creacion").toInstant());
        a.setFechaUltimaActualizacion(rs.getTimestamp("fecha_actualizacion").toInstant());
        return a;
    }
}
