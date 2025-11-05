package repositorio.archivo;

import dominio.Archivo;
import repositorio.conexion.GestorConexionH2;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Implementación del repositorio de archivos que persiste en H2.
 */
public class RepositorioArchivoImpl implements IRepositorioArchivo {

    private final GestorConexionH2 gestorConexion;

    public RepositorioArchivoImpl() {
        this.gestorConexion = GestorConexionH2.getInstancia();
    }

    @Override
    public CompletableFuture<Boolean> guardar(Archivo archivo) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT INTO archivos (id_archivo, file_id_servidor, nombre_archivo, mime_type, " +
                        "tamanio_bytes, contenido_base64, hash_sha256, fecha_descarga, fecha_ultima_actualizacion, " +
                        "asociado_a, id_asociado, estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            Connection conn = null;
            PreparedStatement pstmt = null;
            try {
                conn = gestorConexion.getConexion();
                pstmt = conn.prepareStatement(sql);

                pstmt.setString(1, archivo.getIdArchivo().toString());
                pstmt.setString(2, archivo.getFileIdServidor());
                pstmt.setString(3, archivo.getNombreArchivo());
                pstmt.setString(4, archivo.getMimeType());
                pstmt.setLong(5, archivo.getTamanioBytes());
                pstmt.setString(6, archivo.getContenidoBase64());
                pstmt.setString(7, archivo.getHashSHA256());
                pstmt.setTimestamp(8, Timestamp.valueOf(archivo.getFechaDescarga()));
                pstmt.setTimestamp(9, Timestamp.valueOf(archivo.getFechaUltimaActualizacion()));
                pstmt.setString(10, archivo.getAsociadoA());
                pstmt.setString(11, archivo.getIdAsociado() != null ? archivo.getIdAsociado().toString() : null);
                pstmt.setString(12, archivo.getEstado());

                int filasAfectadas = pstmt.executeUpdate();
                System.out.println("[RepositorioArchivo] Archivo guardado: " + archivo.getFileIdServidor());
                return filasAfectadas > 0;
                
            } catch (SQLException e) {
                System.err.println("[RepositorioArchivo] Error al guardar archivo: " + e.getMessage());
                throw new RuntimeException("Fallo en la operación de base de datos al guardar archivo", e);
            } finally {
                try {
                    if (pstmt != null) pstmt.close();
                    // ⚠️ NO cerrar la conexión - es compartida (singleton)
                } catch (SQLException e) {
                    System.err.println("[RepositorioArchivo] Error al cerrar PreparedStatement: " + e.getMessage());
                }
            }
        });
    }

    @Override
    public CompletableFuture<Archivo> buscarPorFileIdServidor(String fileIdServidor) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM archivos WHERE file_id_servidor = ?";
            
            Connection conn = null;
            PreparedStatement pstmt = null;
            ResultSet rs = null;
            try {
                conn = gestorConexion.getConexion();
                pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, fileIdServidor);
                rs = pstmt.executeQuery();

                if (rs.next()) {
                    return mapearArchivo(rs);
                }
                return null;
                
            } catch (SQLException e) {
                System.err.println("[RepositorioArchivo] Error al buscar archivo: " + e.getMessage());
                throw new RuntimeException("Fallo al buscar archivo por fileId", e);
            } finally {
                try {
                    if (rs != null) rs.close();
                    if (pstmt != null) pstmt.close();
                    // ⚠️ NO cerrar la conexión
                } catch (SQLException e) {
                    System.err.println("[RepositorioArchivo] Error al cerrar recursos: " + e.getMessage());
                }
            }
        });
    }

    @Override
    public CompletableFuture<List<Archivo>> buscarPorAsociacion(String asociadoA, String idAsociado) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM archivos WHERE asociado_a = ? AND id_asociado = ?";
            List<Archivo> archivos = new ArrayList<>();
            
            Connection conn = null;
            PreparedStatement pstmt = null;
            ResultSet rs = null;
            try {
                conn = gestorConexion.getConexion();
                pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, asociadoA);
                pstmt.setString(2, idAsociado);
                rs = pstmt.executeQuery();

                while (rs.next()) {
                    archivos.add(mapearArchivo(rs));
                }
                
                System.out.println("[RepositorioArchivo] Encontrados " + archivos.size() + 
                                 " archivos para " + asociadoA + ":" + idAsociado);
                return archivos;
                
            } catch (SQLException e) {
                System.err.println("[RepositorioArchivo] Error al buscar archivos por asociación: " + e.getMessage());
                throw new RuntimeException("Fallo al buscar archivos por asociación", e);
            } finally {
                try {
                    if (rs != null) rs.close();
                    if (pstmt != null) pstmt.close();
                    // ⚠️ NO cerrar la conexión
                } catch (SQLException e) {
                    System.err.println("[RepositorioArchivo] Error al cerrar recursos: " + e.getMessage());
                }
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> actualizarEstado(String fileIdServidor, String nuevoEstado) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE archivos SET estado = ?, fecha_ultima_actualizacion = ? WHERE file_id_servidor = ?";
            
            Connection conn = null;
            PreparedStatement pstmt = null;
            try {
                conn = gestorConexion.getConexion();
                pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, nuevoEstado);
                pstmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                pstmt.setString(3, fileIdServidor);

                int filasAfectadas = pstmt.executeUpdate();
                System.out.println("[RepositorioArchivo] Estado actualizado a '" + nuevoEstado + 
                                 "' para archivo: " + fileIdServidor);
                return filasAfectadas > 0;
                
            } catch (SQLException e) {
                System.err.println("[RepositorioArchivo] Error al actualizar estado: " + e.getMessage());
                throw new RuntimeException("Fallo al actualizar estado del archivo", e);
            } finally {
                try {
                    if (pstmt != null) pstmt.close();
                    // ⚠️ NO cerrar la conexión
                } catch (SQLException e) {
                    System.err.println("[RepositorioArchivo] Error al cerrar PreparedStatement: " + e.getMessage());
                }
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> actualizarContenido(String fileIdServidor, String contenidoBase64) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE archivos SET contenido_base64 = ?, fecha_ultima_actualizacion = ?, estado = 'completo' " +
                        "WHERE file_id_servidor = ?";
            
            Connection conn = null;
            PreparedStatement pstmt = null;
            try {
                conn = gestorConexion.getConexion();
                pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, contenidoBase64);
                pstmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                pstmt.setString(3, fileIdServidor);

                int filasAfectadas = pstmt.executeUpdate();
                System.out.println("[RepositorioArchivo] Contenido actualizado para archivo: " + fileIdServidor);
                return filasAfectadas > 0;
                
            } catch (SQLException e) {
                System.err.println("[RepositorioArchivo] Error al actualizar contenido: " + e.getMessage());
                throw new RuntimeException("Fallo al actualizar contenido del archivo", e);
            } finally {
                try {
                    if (pstmt != null) pstmt.close();
                    // ⚠️ NO cerrar la conexión
                } catch (SQLException e) {
                    System.err.println("[RepositorioArchivo] Error al cerrar PreparedStatement: " + e.getMessage());
                }
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> eliminar(String fileIdServidor) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM archivos WHERE file_id_servidor = ?";
            
            Connection conn = null;
            PreparedStatement pstmt = null;
            try {
                conn = gestorConexion.getConexion();
                pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, fileIdServidor);
                int filasAfectadas = pstmt.executeUpdate();
                
                System.out.println("[RepositorioArchivo] Archivo eliminado: " + fileIdServidor);
                return filasAfectadas > 0;
                
            } catch (SQLException e) {
                System.err.println("[RepositorioArchivo] Error al eliminar archivo: " + e.getMessage());
                throw new RuntimeException("Fallo al eliminar archivo", e);
            } finally {
                try {
                    if (pstmt != null) pstmt.close();
                    // ⚠️ NO cerrar la conexión
                } catch (SQLException e) {
                    System.err.println("[RepositorioArchivo] Error al cerrar PreparedStatement: " + e.getMessage());
                }
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> existe(String fileIdServidor) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(1) AS cnt FROM archivos WHERE file_id_servidor = ?";
            
            Connection conn = null;
            PreparedStatement pstmt = null;
            ResultSet rs = null;
            try {
                conn = gestorConexion.getConexion();
                pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, fileIdServidor);
                rs = pstmt.executeQuery();

                if (rs.next()) {
                    return rs.getInt("cnt") > 0;
                }
                return false;
                
            } catch (SQLException e) {
                System.err.println("[RepositorioArchivo] Error al verificar existencia: " + e.getMessage());
                throw new RuntimeException("Fallo al verificar existencia del archivo", e);
            } finally {
                try {
                    if (rs != null) rs.close();
                    if (pstmt != null) pstmt.close();
                    // ⚠️ NO cerrar la conexión - es compartida
                } catch (SQLException e) {
                    System.err.println("[RepositorioArchivo] Error al cerrar recursos: " + e.getMessage());
                }
            }
        });
    }

    private Archivo mapearArchivo(ResultSet rs) throws SQLException {
        Archivo archivo = new Archivo();
        archivo.setIdArchivo(UUID.fromString(rs.getString("id_archivo")));
        archivo.setFileIdServidor(rs.getString("file_id_servidor"));
        archivo.setNombreArchivo(rs.getString("nombre_archivo"));
        archivo.setMimeType(rs.getString("mime_type"));
        archivo.setTamanioBytes(rs.getLong("tamanio_bytes"));
        archivo.setContenidoBase64(rs.getString("contenido_base64"));
        archivo.setHashSHA256(rs.getString("hash_sha256"));
        
        Timestamp fechaDescarga = rs.getTimestamp("fecha_descarga");
        if (fechaDescarga != null) {
            archivo.setFechaDescarga(fechaDescarga.toLocalDateTime());
        }
        
        Timestamp fechaActualizacion = rs.getTimestamp("fecha_ultima_actualizacion");
        if (fechaActualizacion != null) {
            archivo.setFechaUltimaActualizacion(fechaActualizacion.toLocalDateTime());
        }
        
        archivo.setAsociadoA(rs.getString("asociado_a"));
        
        String idAsociado = rs.getString("id_asociado");
        if (idAsociado != null) {
            archivo.setIdAsociado(UUID.fromString(idAsociado));
        }
        
        archivo.setEstado(rs.getString("estado"));
        return archivo;
    }
}

