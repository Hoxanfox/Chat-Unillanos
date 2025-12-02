package repositorio.clienteServidor;

import dominio.clienteServidor.Transcripcion;
import dominio.clienteServidor.Transcripcion.EstadoTranscripcion;
import repositorio.comunicacion.MySQLManager;
import observador.IObservador;
import observador.ISujeto;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Repositorio para gestionar transcripciones de audio en la base de datos
 * ✅ ACTUALIZADO: Implementa ISujeto para notificar cambios a la interfaz
 */
public class TranscripcionRepositorio implements ISujeto {

    private static final String TAG = "[TranscripcionRepo]";
    private final MySQLManager mysql;
    private final List<IObservador> observadores;

    public TranscripcionRepositorio() {
        this.mysql = MySQLManager.getInstance();
        this.observadores = new ArrayList<>();
    }

    // ✅ NUEVO: Implementación del patrón Observador
    @Override
    public void registrarObservador(IObservador observador) {
        if (observador != null && !observadores.contains(observador)) {
            observadores.add(observador);
            System.out.println(TAG + " Observador registrado");
        }
    }

    @Override
    public void removerObservador(IObservador observador) {
        observadores.remove(observador);
    }

    @Override
    public void notificarObservadores(String tipoDeDato, Object datos) {
        for (IObservador observador : observadores) {
            observador.actualizar(tipoDeDato, datos);
        }
    }

    /**
     * Guarda una nueva transcripción
     */
    public boolean guardar(Transcripcion transcripcion) {
        String sql = "INSERT INTO transcripciones (id, archivo_id, mensaje_id, transcripcion, estado, " +
                "duracion_segundos, idioma, confianza, fecha_creacion, fecha_procesamiento, fecha_actualizacion) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, transcripcion.getIdUUID().toString());
            ps.setString(2, transcripcion.getArchivoId().toString());
            ps.setString(3, transcripcion.getMensajeId() != null ? transcripcion.getMensajeId().toString() : null);
            ps.setString(4, transcripcion.getTranscripcion());
            ps.setString(5, transcripcion.getEstado().name());
            ps.setBigDecimal(6, transcripcion.getDuracionSegundos());
            ps.setString(7, transcripcion.getIdioma());
            ps.setBigDecimal(8, transcripcion.getConfianza());
            ps.setTimestamp(9, Timestamp.from(transcripcion.getFechaCreacion()));
            ps.setTimestamp(10, transcripcion.getFechaProcesamiento() != null ?
                    Timestamp.from(transcripcion.getFechaProcesamiento()) : null);
            ps.setTimestamp(11, Timestamp.from(transcripcion.getFechaActualizacion()));

            int affected = ps.executeUpdate();
            if (affected > 0) {
                System.out.println(TAG + " ✓ Transcripción guardada: " + transcripcion.getIdUUID());
            }
            return affected > 0;
        } catch (SQLException e) {
            System.err.println(TAG + " Error guardando transcripción: " + e.getMessage());
            return false;
        }
    }

    /**
     * Actualiza una transcripción existente
     * ✅ ACTUALIZADO: Ahora notifica eventos cuando cambia el estado
     */
    public boolean actualizar(Transcripcion transcripcion) {
        String sql = "UPDATE transcripciones SET transcripcion=?, estado=?, duracion_segundos=?, " +
                "idioma=?, confianza=?, fecha_procesamiento=?, fecha_actualizacion=? WHERE id=?";

        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, transcripcion.getTranscripcion());
            ps.setString(2, transcripcion.getEstado().name());
            ps.setBigDecimal(3, transcripcion.getDuracionSegundos());
            ps.setString(4, transcripcion.getIdioma());
            ps.setBigDecimal(5, transcripcion.getConfianza());
            ps.setTimestamp(6, transcripcion.getFechaProcesamiento() != null ?
                    Timestamp.from(transcripcion.getFechaProcesamiento()) : null);
            ps.setTimestamp(7, Timestamp.from(transcripcion.getFechaActualizacion()));
            ps.setString(8, transcripcion.getIdUUID().toString());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                System.out.println(TAG + " ✓ Transcripción actualizada: " + transcripcion.getIdUUID());

                // ✅ NUEVO: Notificar a los observadores del cambio
                notificarObservadores("TRANSCRIPCION_ACTUALIZADA", transcripcion);
            }
            return affected > 0;
        } catch (SQLException e) {
            System.err.println(TAG + " Error actualizando transcripción: " + e.getMessage());
            return false;
        }
    }

    /**
     * Busca una transcripción por su ID
     */
    public Transcripcion buscarPorId(UUID id) {
        String sql = "SELECT * FROM transcripciones WHERE id = ?";

        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println(TAG + " Error buscando por ID: " + e.getMessage());
        }
        return null;
    }

    /**
     * Busca una transcripción por el ID del archivo
     */
    public Transcripcion buscarPorArchivoId(UUID archivoId) {
        String sql = "SELECT * FROM transcripciones WHERE archivo_id = ? ORDER BY fecha_creacion DESC LIMIT 1";

        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, archivoId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println(TAG + " Error buscando por archivo ID: " + e.getMessage());
        }
        return null;
    }

    /**
     * Busca transcripciones por el ID del mensaje
     */
    public List<Transcripcion> buscarPorMensajeId(UUID mensajeId) {
        List<Transcripcion> lista = new ArrayList<>();
        String sql = "SELECT * FROM transcripciones WHERE mensaje_id = ? ORDER BY fecha_creacion DESC";

        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, mensajeId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println(TAG + " Error buscando por mensaje ID: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Obtiene todas las transcripciones con un estado específico
     */
    public List<Transcripcion> buscarPorEstado(EstadoTranscripcion estado) {
        List<Transcripcion> lista = new ArrayList<>();
        String sql = "SELECT * FROM transcripciones WHERE estado = ? ORDER BY fecha_creacion DESC";

        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, estado.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println(TAG + " Error buscando por estado: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Obtiene todas las transcripciones
     */
    public List<Transcripcion> obtenerTodas() {
        List<Transcripcion> lista = new ArrayList<>();
        String sql = "SELECT * FROM transcripciones ORDER BY fecha_creacion DESC";

        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            System.err.println(TAG + " Error obteniendo todas: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Obtiene los audios que aún no tienen transcripción
     * Busca en la tabla de archivos los archivos de audio sin transcripción
     */
    public List<UUID> obtenerAudiosSinTranscripcion() {
        List<UUID> listaIds = new ArrayList<>();
        String sql = "SELECT a.id FROM archivos a " +
                "LEFT JOIN transcripciones t ON a.id = t.archivo_id " +
                "WHERE a.mime_type LIKE 'audio/%' AND t.id IS NULL";

        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                listaIds.add(UUID.fromString(rs.getString("id")));
            }

            System.out.println(TAG + " Audios sin transcripción encontrados: " + listaIds.size());
        } catch (SQLException e) {
            System.err.println(TAG + " Error obteniendo audios sin transcripción: " + e.getMessage());
        }
        return listaIds;
    }

    /**
     * Elimina una transcripción
     */
    public boolean eliminar(UUID id) {
        String sql = "DELETE FROM transcripciones WHERE id = ?";

        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println(TAG + " Error eliminando: " + e.getMessage());
            return false;
        }
    }

    /**
     * ✅ NUEVO: Obtiene transcripciones de audios enviados en canales
     * Une transcripciones con mensajes para filtrar por canal_id NOT NULL
     */
    public List<Transcripcion> obtenerPorCanal(UUID canalId) {
        List<Transcripcion> lista = new ArrayList<>();
        String sql = "SELECT t.* FROM transcripciones t " +
                "INNER JOIN mensajes m ON t.mensaje_id = m.id " +
                "WHERE m.canal_id = ? " +
                "ORDER BY t.fecha_creacion DESC";

        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, canalId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println(TAG + " Error obteniendo por canal: " + e.getMessage());
        }
        return lista;
    }

    /**
     * ✅ NUEVO: Obtiene transcripciones de audios enviados en todos los canales
     */
    public List<Transcripcion> obtenerDeCanales() {
        List<Transcripcion> lista = new ArrayList<>();
        String sql = "SELECT t.* FROM transcripciones t " +
                "INNER JOIN mensajes m ON t.mensaje_id = m.id " +
                "WHERE m.canal_id IS NOT NULL " +
                "ORDER BY t.fecha_creacion DESC";

        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            System.err.println(TAG + " Error obteniendo de canales: " + e.getMessage());
        }
        return lista;
    }

    /**
     * ✅ NUEVO: Obtiene transcripciones de audios enviados en conversaciones directas (contactos)
     */
    public List<Transcripcion> obtenerDeContactos() {
        List<Transcripcion> lista = new ArrayList<>();
        String sql = "SELECT t.* FROM transcripciones t " +
                "INNER JOIN mensajes m ON t.mensaje_id = m.id " +
                "WHERE m.canal_id IS NULL AND m.destinatario_usuario_id IS NOT NULL " +
                "ORDER BY t.fecha_creacion DESC";

        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            System.err.println(TAG + " Error obteniendo de contactos: " + e.getMessage());
        }
        return lista;
    }

    /**
     * ✅ NUEVO: Obtiene transcripciones de audios entre dos usuarios específicos
     */
    public List<Transcripcion> obtenerPorContactos(UUID usuario1Id, UUID usuario2Id) {
        List<Transcripcion> lista = new ArrayList<>();
        String sql = "SELECT t.* FROM transcripciones t " +
                "INNER JOIN mensajes m ON t.mensaje_id = m.id " +
                "WHERE ((m.remitente_id = ? AND m.destinatario_usuario_id = ?) " +
                "OR (m.remitente_id = ? AND m.destinatario_usuario_id = ?)) " +
                "AND m.canal_id IS NULL " +
                "ORDER BY t.fecha_creacion DESC";

        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, usuario1Id.toString());
            ps.setString(2, usuario2Id.toString());
            ps.setString(3, usuario2Id.toString());
            ps.setString(4, usuario1Id.toString());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println(TAG + " Error obteniendo por contactos: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Mapea un ResultSet a una entidad Transcripcion
     */
    private Transcripcion mapear(ResultSet rs) throws SQLException {
        Transcripcion t = new Transcripcion();
        t.setId(UUID.fromString(rs.getString("id")));
        t.setArchivoId(UUID.fromString(rs.getString("archivo_id")));

        String mensajeId = rs.getString("mensaje_id");
        if (mensajeId != null) {
            t.setMensajeId(UUID.fromString(mensajeId));
        }

        t.setTranscripcion(rs.getString("transcripcion"));
        t.setEstado(EstadoTranscripcion.valueOf(rs.getString("estado")));
        t.setDuracionSegundos(rs.getBigDecimal("duracion_segundos"));
        t.setIdioma(rs.getString("idioma"));
        t.setConfianza(rs.getBigDecimal("confianza"));
        
        // Validar timestamps para evitar NullPointerException
        Timestamp fechaCreacion = rs.getTimestamp("fecha_creacion");
        t.setFechaCreacion(fechaCreacion != null ? fechaCreacion.toInstant() : java.time.Instant.now());

        Timestamp fechaProcesamiento = rs.getTimestamp("fecha_procesamiento");
        if (fechaProcesamiento != null) {
            t.setFechaProcesamiento(fechaProcesamiento.toInstant());
        }

        Timestamp fechaActualizacion = rs.getTimestamp("fecha_actualizacion");
        t.setFechaActualizacion(fechaActualizacion != null ? fechaActualizacion.toInstant() : java.time.Instant.now());

        return t;
    }
}
