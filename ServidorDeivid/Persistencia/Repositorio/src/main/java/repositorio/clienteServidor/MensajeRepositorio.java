package repositorio.clienteServidor;

import dominio.clienteServidor.Mensaje;
import repositorio.comunicacion.MySQLManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MensajeRepositorio {

    private final MySQLManager mysql;

    public MensajeRepositorio() {
        this.mysql = MySQLManager.getInstance();
    }

    /**
     * Obtiene todos los mensajes ordenados cronológicamente y por ID.
     * Este orden es CRÍTICO para que el Árbol de Merkle genere el mismo hash en todos los nodos.
     */
    public List<Mensaje> obtenerTodosParaSync() {
        List<Mensaje> lista = new ArrayList<>();
        // Seleccionamos TODAS las columnas relevantes
        String sql = "SELECT id, remitente_id, destinatario_usuario_id, canal_id, tipo, contenido, fecha_envio " +
                "FROM mensajes ORDER BY fecha_envio ASC, id ASC";

        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearMensaje(rs));
            }
        } catch (SQLException e) {
            System.err.println("[MensajeRepo] Error cargando mensajes para sync: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Guarda un nuevo mensaje o actualiza uno existente si el ID ya está en la base de datos.
     * Vital para recibir mensajes del chat en tiempo real o de la sincronización.
     */
    public boolean guardar(Mensaje m) {
        if (m == null || m.getId() == null) return false;

        String sql = "INSERT INTO mensajes (id, remitente_id, destinatario_usuario_id, canal_id, tipo, contenido, fecha_envio) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE contenido = VALUES(contenido), fecha_envio = VALUES(fecha_envio)";

        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, m.getId());
            ps.setString(2, m.getRemitenteId() != null ? m.getRemitenteId().toString() : null);
            ps.setString(3, m.getDestinatarioUsuarioId() != null ? m.getDestinatarioUsuarioId().toString() : null);
            ps.setString(4, m.getCanalId() != null ? m.getCanalId().toString() : null);
            ps.setString(5, m.getTipo() != null ? m.getTipo().name() : Mensaje.Tipo.TEXTO.name());
            ps.setString(6, m.getContenido());
            ps.setTimestamp(7, m.getFechaEnvio() != null ? Timestamp.from(m.getFechaEnvio()) : Timestamp.from(Instant.now()));

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[MensajeRepo] Error guardando mensaje " + m.getId() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Método auxiliar para convertir un ResultSet en un objeto Mensaje.
     * Centraliza la lógica de mapeo y evita duplicación de código.
     */
    private Mensaje mapearMensaje(ResultSet rs) throws SQLException {
        Mensaje m = new Mensaje();

        // Conversión segura de String a UUID
        m.setId(toUUID(rs.getString("id")));
        m.setRemitenteId(toUUID(rs.getString("remitente_id")));
        m.setDestinatarioUsuarioId(toUUID(rs.getString("destinatario_usuario_id")));
        m.setCanalId(toUUID(rs.getString("canal_id")));

        // Enum Tipo
        String tipoStr = rs.getString("tipo");
        if (tipoStr != null) {
            try {
                m.setTipo(Mensaje.Tipo.valueOf(tipoStr));
            } catch (Exception e) {
                m.setTipo(Mensaje.Tipo.TEXTO); // Valor por defecto
            }
        }

        m.setContenido(rs.getString("contenido"));

        // Fecha
        Timestamp ts = rs.getTimestamp("fecha_envio");
        if (ts != null) {
            m.setFechaEnvio(ts.toInstant());
        } else {
            m.setFechaEnvio(Instant.EPOCH);
        }

        return m;
    }

    /**
     * Helper para evitar NullPointerException al convertir Strings de la BD a UUID.
     */
    private UUID toUUID(String uuidStr) {
        if (uuidStr == null || uuidStr.trim().isEmpty()) return null;
        try {
            return UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}