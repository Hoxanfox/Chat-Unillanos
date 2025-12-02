package repositorio.clienteServidor;

import dominio.clienteServidor.Archivo;
import repositorio.comunicacion.MySQLManager;
import observador.ISujeto;
import observador.IObservador;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ArchivoRepositorio implements ISujeto {
    private final MySQLManager mysql;
    private final List<IObservador> observadores;

    public ArchivoRepositorio() {
        this.mysql = MySQLManager.getInstance();
        this.observadores = new ArrayList<>();
    }

    @Override
    public void registrarObservador(IObservador observador) {
        if (observador != null && !observadores.contains(observador)) {
            observadores.add(observador);
            System.out.println("[RepoArchivo] ✓ Observador registrado");
        }
    }

    @Override
    public void removerObservador(IObservador observador) {
        observadores.remove(observador);
        System.out.println("[RepoArchivo] Observador removido");
    }

    @Override
    public void notificarObservadores(String tipoDeDato, Object datos) {
        for (IObservador observador : observadores) {
            try {
                observador.actualizar(tipoDeDato, datos);
            } catch (Exception e) {
                System.err.println("[RepoArchivo] Error notificando observador: " + e.getMessage());
            }
        }
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

            boolean exitoso = ps.executeUpdate() > 0;

            if (exitoso) {
                System.out.println("[RepoArchivo] ✓ Archivo guardado: " + archivo.getFileId());

                // ✅ NUEVO: Notificar a observadores cuando se guarda un archivo
                String tipoArchivo = determinarTipoArchivo(archivo.getMimeType());
                notificarObservadores("ARCHIVO_PERSISTIDO", archivo);

                // Notificación específica por tipo
                if (tipoArchivo.equals("audio")) {
                    notificarObservadores("AUDIO_PERSISTIDO", archivo);
                    System.out.println("[RepoArchivo] 🔔 Audio persistido - notificando a observadores");
                } else if (tipoArchivo.equals("texto")) {
                    notificarObservadores("TEXTO_PERSISTIDO", archivo);
                    System.out.println("[RepoArchivo] 🔔 Texto persistido - notificando a observadores");
                }
            }

            return exitoso;
        } catch (SQLException e) {
            System.err.println("[RepoArchivo] Error guardando: " + e.getMessage());
            return false;
        }
    }

    /**
     * ✅ NUEVO: Actualiza un archivo existente y actualiza su timestamp automáticamente.
     */
    public boolean actualizar(Archivo archivo) {
        // Actualizar timestamp de modificación al momento actual
        archivo.setFechaUltimaActualizacion(java.time.Instant.now());

        String sql = "UPDATE archivos SET nombre_archivo=?, ruta_relativa=?, mime_type=?, tamanio=?, hash_sha256=?, fecha_actualizacion=? WHERE id=?";
        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, archivo.getNombreArchivo());
            ps.setString(2, archivo.getRutaRelativa());
            ps.setString(3, archivo.getMimeType());
            ps.setLong(4, archivo.getTamanio());
            ps.setString(5, archivo.getHashSHA256());
            ps.setTimestamp(6, Timestamp.from(archivo.getFechaUltimaActualizacion()));
            ps.setString(7, archivo.getId().toString());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                System.out.println("[RepoArchivo] ✓ Archivo actualizado con timestamp: " + archivo.getId());

                // ✅ NUEVO: Notificar cuando se actualiza un archivo
                notificarObservadores("ARCHIVO_ACTUALIZADO", archivo);

                String tipoArchivo = determinarTipoArchivo(archivo.getMimeType());
                if (tipoArchivo.equals("audio")) {
                    notificarObservadores("AUDIO_ACTUALIZADO", archivo);
                }
            }
            return affected > 0;
        } catch (SQLException e) {
            System.err.println("[RepoArchivo] Error actualizando: " + e.getMessage());
            return false;
        }
    }

    /**
     * ✅ NUEVO: Determina el tipo de archivo basado en el MIME type
     */
    private String determinarTipoArchivo(String mimeType) {
        if (mimeType == null) return "desconocido";

        if (mimeType.startsWith("audio/")) return "audio";
        if (mimeType.startsWith("video/")) return "video";
        if (mimeType.startsWith("image/")) return "imagen";
        if (mimeType.startsWith("text/")) return "texto";
        if (mimeType.contains("pdf")) return "documento";
        if (mimeType.contains("word") || mimeType.contains("document")) return "documento";
        if (mimeType.contains("spreadsheet") || mimeType.contains("excel")) return "hoja_calculo";

        return "otros";
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

    /**
     * ✅ NUEVO: Obtiene archivos de audio por tipo MIME
     */
    public List<Archivo> obtenerArchivosPorTipo(String mimeTypePattern) {
        List<Archivo> lista = new ArrayList<>();
        String sql = "SELECT * FROM archivos WHERE mime_type LIKE ? ORDER BY fecha_creacion DESC";
        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, mimeTypePattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[RepoArchivo] Error obteniendo por tipo: " + e.getMessage());
        }
        return lista;
    }

    /**
     * ✅ NUEVO: Obtiene archivos de audio
     */
    public List<Archivo> obtenerArchivosAudio() {
        return obtenerArchivosPorTipo("audio/%");
    }

    /**
     * ✅ NUEVO: Obtiene archivos de texto
     */
    public List<Archivo> obtenerArchivosTexto() {
        return obtenerArchivosPorTipo("text/%");
    }

    /**
     * ✅ NUEVO: Obtiene archivos de imagen
     */
    public List<Archivo> obtenerArchivosImagen() {
        return obtenerArchivosPorTipo("image/%");
    }

    /**
     * ✅ NUEVO: Obtiene archivos de documentos
     */
    public List<Archivo> obtenerArchivosDocumentos() {
        List<Archivo> lista = new ArrayList<>();
        String sql = "SELECT * FROM archivos WHERE mime_type LIKE 'application/pdf' " +
                "OR mime_type LIKE 'application/msword' " +
                "OR mime_type LIKE 'application/vnd.openxmlformats%' " +
                "ORDER BY fecha_creacion DESC";
        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            System.err.println("[RepoArchivo] Error obteniendo documentos: " + e.getMessage());
        }
        return lista;
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
        
        // Validar timestamps para evitar NullPointerException
        java.sql.Timestamp fechaCreacion = rs.getTimestamp("fecha_creacion");
        a.setFechaCreacion(fechaCreacion != null ? fechaCreacion.toInstant() : java.time.Instant.now());
        
        java.sql.Timestamp fechaActualizacion = rs.getTimestamp("fecha_actualizacion");
        a.setFechaUltimaActualizacion(fechaActualizacion != null ? fechaActualizacion.toInstant() : java.time.Instant.now());
        return a;
    }
}
