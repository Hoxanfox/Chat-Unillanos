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
 * CORREGIDO: Utiliza try-with-resources para manejar conexiones del pool
 * y elimina la columna contenido_base64.
 */
public class RepositorioArchivoImpl implements IRepositorioArchivo {

    private final GestorConexionH2 gestorConexion;

    public RepositorioArchivoImpl() {
        this.gestorConexion = GestorConexionH2.getInstancia();
    }

    @Override
    public CompletableFuture<Boolean> guardar(Archivo archivo) {
        return CompletableFuture.supplyAsync(() -> {
            // Primero verificar si el archivo ya existe
            try {
                // .join() es aceptable aquí porque estamos dentro de supplyAsync
                boolean yaExiste = existe(archivo.getFileIdServidor()).join();

                if (yaExiste) {
                    // Si ya existe, hacer UPDATE completo
                    System.out.println("[RepositorioArchivo] Archivo ya existe, actualizando: " + archivo.getFileIdServidor());
                    return actualizarCompleto(archivo);
                } else {
                    // Si no existe, hacer INSERT
                    System.out.println("[RepositorioArchivo] Archivo nuevo, insertando: " + archivo.getFileIdServidor());
                    return insertar(archivo);
                }
            } catch (Exception e) {
                System.err.println("[RepositorioArchivo] Error en guardar: " + e.getMessage());
                throw new RuntimeException("Fallo en la operación de guardar archivo", e);
            }
        });
    }

    /**
     * Inserta un nuevo archivo en la base de datos
     */
    private boolean insertar(Archivo archivo) {
        // SQL sin contenido_base64 (11 parámetros)
        String sql = "INSERT INTO archivos (id_archivo, file_id_servidor, nombre_archivo, mime_type, " +
                "tamanio_bytes, hash_sha256, fecha_descarga, fecha_ultima_actualizacion, " +
                "asociado_a, id_asociado, estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        // 1. Usar try-with-resources para la conexión y el statement
        // La conexión se obtiene del pool y se devuelve automáticamente
        try (Connection conn = gestorConexion.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, archivo.getIdArchivo().toString());
            pstmt.setString(2, archivo.getFileIdServidor());
            pstmt.setString(3, archivo.getNombreArchivo());
            pstmt.setString(4, archivo.getMimeType());
            pstmt.setLong(5, archivo.getTamanioBytes());
            // pstmt.setString(6, archivo.getContenidoBase64()); // <-- ELIMINADO
            pstmt.setString(6, archivo.getHashSHA256()); // <-- Parámetro 6 (antes 7)
            pstmt.setTimestamp(7, Timestamp.valueOf(archivo.getFechaDescarga()));
            pstmt.setTimestamp(8, Timestamp.valueOf(archivo.getFechaUltimaActualizacion()));
            pstmt.setString(9, archivo.getAsociadoA());
            pstmt.setString(10, archivo.getIdAsociado() != null ? archivo.getIdAsociado().toString() : null);
            pstmt.setString(11, archivo.getEstado());

            int filasAfectadas = pstmt.executeUpdate();
            System.out.println("[RepositorioArchivo] ✅ Archivo insertado: " + archivo.getFileIdServidor());
            return filasAfectadas > 0;

        } catch (SQLException e) {
            System.err.println("[RepositorioArchivo] ❌ Error al insertar archivo: " + e.getMessage());
            throw new RuntimeException("Fallo al insertar archivo", e);
        }
        // 2. No se necesita 'finally' para cerrar conn o pstmt
    }

    /**
     * Actualiza completamente un archivo existente en la base de datos
     */
    private boolean actualizarCompleto(Archivo archivo) {
        // SQL sin contenido_base64 (9 parámetros de SET + 1 de WHERE)
        String sql = "UPDATE archivos SET nombre_archivo = ?, mime_type = ?, tamanio_bytes = ?, " +
                "hash_sha256 = ?, fecha_ultima_actualizacion = ?, " +
                "asociado_a = ?, id_asociado = ?, estado = ? WHERE file_id_servidor = ?";

        // 1. Usar try-with-resources
        try (Connection conn = gestorConexion.getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, archivo.getNombreArchivo());
            pstmt.setString(2, archivo.getMimeType());
            pstmt.setLong(3, archivo.getTamanioBytes());
            // pstmt.setString(4, archivo.getContenidoBase64()); // <-- ELIMINADO
            pstmt.setString(4, archivo.getHashSHA256()); // <-- Parámetro 4 (antes 5)
            pstmt.setTimestamp(5, Timestamp.valueOf(archivo.getFechaUltimaActualizacion()));
            pstmt.setString(6, archivo.getAsociadoA());
            pstmt.setString(7, archivo.getIdAsociado() != null ? archivo.getIdAsociado().toString() : null);
            pstmt.setString(8, archivo.getEstado());
            pstmt.setString(9, archivo.getFileIdServidor()); // Parámetro del WHERE

            int filasAfectadas = pstmt.executeUpdate();
            System.out.println("[RepositorioArchivo] ✅ Archivo actualizado: " + archivo.getFileIdServidor());
            return filasAfectadas > 0;

        } catch (SQLException e) {
            System.err.println("[RepositorioArchivo] ❌ Error al actualizar archivo: " + e.getMessage());
            throw new RuntimeException("Fallo al actualizar archivo", e);
        }
        // 2. No se necesita 'finally'
    }

    @Override
    public CompletableFuture<Archivo> buscarPorFileIdServidor(String fileIdServidor) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM archivos WHERE file_id_servidor = ?";

            // 1. Usar try-with-resources para Connection, PreparedStatement y ResultSet
            // Se elimina el bucle de reintento, ya que el pool maneja conexiones inválidas.
            try (Connection conn = gestorConexion.getConexion();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, fileIdServidor);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return mapearArchivo(rs);
                    }
                }
            } catch (SQLException e) {
                System.err.println("[RepositorioArchivo] Error al buscar archivo: " + e.getMessage());
                throw new RuntimeException("Fallo al buscar archivo por fileId", e);
            }
            // 2. No se necesita 'finally'

            return null; // No encontrado
        });
    }


    @Override
    public CompletableFuture<List<Archivo>> buscarPorAsociacion(String asociadoA, String idAsociado) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM archivos WHERE asociado_a = ? AND id_asociado = ?";
            List<Archivo> archivos = new ArrayList<>();

            // 1. Usar try-with-resources
            try (Connection conn = gestorConexion.getConexion();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, asociadoA);
                pstmt.setString(2, idAsociado);

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        archivos.add(mapearArchivo(rs));
                    }
                }

                System.out.println("[RepositorioArchivo] Encontrados " + archivos.size() +
                        " archivos para " + asociadoA + ":" + idAsociado);
                return archivos;

            } catch (SQLException e) {
                System.err.println("[RepositorioArchivo] Error al buscar archivos por asociación: " + e.getMessage());
                throw new RuntimeException("Fallo al buscar archivos por asociación", e);
            }
            // 2. No se necesita 'finally'
        });
    }

    @Override
    public CompletableFuture<Boolean> actualizarEstado(String fileIdServidor, String nuevoEstado) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE archivos SET estado = ?, fecha_ultima_actualizacion = ? WHERE file_id_servidor = ?";

            // 1. Usar try-with-resources
            try (Connection conn = gestorConexion.getConexion();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

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
            }
            // 2. No se necesita 'finally'
        });
    }

    /**
     * Este método ya no es necesario, ya que contenido_base64 fue eliminado.
     * La lógica de actualización de hash y estado se maneja en 'guardar'/'actualizarCompleto'.
     */
    @Override
    public CompletableFuture<Boolean> actualizarContenido(String fileIdServidor, String contenidoBase64) {
        System.err.println("[RepositorioArchivo] ADVERTENCIA: actualizarContenido() está obsoleto y no debe usarse.");
        // Simplemente actualiza la fecha para indicar actividad, pero no guarda contenido.
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE archivos SET fecha_ultima_actualizacion = ?, estado = 'completo' " +
                    "WHERE file_id_servidor = ?";

            try (Connection conn = gestorConexion.getConexion();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                pstmt.setString(2, fileIdServidor);

                int filasAfectadas = pstmt.executeUpdate();
                System.out.println("[RepositorioArchivo] Contenido (obsoleto) actualizado para archivo: " + fileIdServidor);
                return filasAfectadas > 0;

            } catch (SQLException e) {
                System.err.println("[RepositorioArchivo] Error al actualizar contenido (obsoleto): " + e.getMessage());
                throw new RuntimeException("Fallo al actualizar contenido del archivo", e);
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> eliminar(String fileIdServidor) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM archivos WHERE file_id_servidor = ?";

            // 1. Usar try-with-resources
            try (Connection conn = gestorConexion.getConexion();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, fileIdServidor);
                int filasAfectadas = pstmt.executeUpdate();

                System.out.println("[RepositorioArchivo] Archivo eliminado: " + fileIdServidor);
                return filasAfectadas > 0;

            } catch (SQLException e) {
                System.err.println("[RepositorioArchivo] Error al eliminar archivo: " + e.getMessage());
                throw new RuntimeException("Fallo al eliminar archivo", e);
            }
            // 2. No se necesita 'finally'
        });
    }

    @Override
    public CompletableFuture<Boolean> existe(String fileIdServidor) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(1) AS cnt FROM archivos WHERE file_id_servidor = ?";

            // 1. Usar try-with-resources
            try (Connection conn = gestorConexion.getConexion();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, fileIdServidor);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("cnt") > 0;
                    }
                }
                return false;

            } catch (SQLException e) {
                System.err.println("[RepositorioArchivo] Error al verificar existencia: " + e.getMessage());
                throw new RuntimeException("Fallo al verificar existencia del archivo", e);
            }
            // 2. No se necesita 'finally'
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
