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
        String sql = "SELECT id, remitente_id, destinatario_usuario_id, canal_id, tipo, contenido, fecha_envio, peer_remitente_id, peer_destino_id " +
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
     * ✅ NUEVO: Obtiene el historial de mensajes entre dos usuarios.
     * Retorna todos los mensajes donde uno es remitente y el otro destinatario (en ambas direcciones).
     */
    public List<Mensaje> obtenerHistorialEntre(String userId1, String userId2) {
        List<Mensaje> lista = new ArrayList<>();

        String sql = "SELECT id, remitente_id, destinatario_usuario_id, canal_id, tipo, contenido, fecha_envio, peer_remitente_id, peer_destino_id " +
                "FROM mensajes " +
                "WHERE (remitente_id = ? AND destinatario_usuario_id = ?) " +
                "   OR (remitente_id = ? AND destinatario_usuario_id = ?) " +
                "ORDER BY fecha_envio ASC, id ASC";

        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Convertir a UUID
            UUID uuid1 = UUID.fromString(userId1);
            UUID uuid2 = UUID.fromString(userId2);

            ps.setString(1, uuid1.toString());
            ps.setString(2, uuid2.toString());
            ps.setString(3, uuid2.toString());
            ps.setString(4, uuid1.toString());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearMensaje(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[MensajeRepo] Error obteniendo historial entre usuarios: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("[MensajeRepo] Error convirtiendo IDs a UUID: " + e.getMessage());
        }

        return lista;
    }

    /**
     * ✅ NUEVO: Obtiene los mensajes de un canal específico con paginación.
     * Retorna los mensajes ordenados cronológicamente.
     */
    public List<Mensaje> obtenerMensajesPorCanal(String canalId, int limite, int offset) {
        List<Mensaje> lista = new ArrayList<>();

        String sql = "SELECT id, remitente_id, destinatario_usuario_id, canal_id, tipo, contenido, fecha_envio, peer_remitente_id, peer_destino_id " +
                "FROM mensajes " +
                "WHERE canal_id = ? " +
                "ORDER BY fecha_envio DESC, id DESC " +
                "LIMIT ? OFFSET ?";

        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            UUID canalUUID = UUID.fromString(canalId);
            ps.setString(1, canalUUID.toString());
            ps.setInt(2, limite);
            ps.setInt(3, offset);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearMensaje(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[MensajeRepo] Error obteniendo mensajes del canal: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("[MensajeRepo] Error convirtiendo canal ID a UUID: " + e.getMessage());
        }

        return lista;
    }

    /**
     * Guarda un nuevo mensaje o actualiza uno existente si el ID ya está en la base de datos.
     * ✅ ACTUALIZADO: Ahora incluye los campos peer_remitente_id y peer_destino_id.
     */
    public boolean guardar(Mensaje m) {
        if (m == null || m.getId() == null) return false;

        String sql = "INSERT INTO mensajes (id, remitente_id, destinatario_usuario_id, canal_id, tipo, contenido, fecha_envio, peer_remitente_id, peer_destino_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE contenido = VALUES(contenido), fecha_envio = VALUES(fecha_envio), " +
                "peer_remitente_id = VALUES(peer_remitente_id), peer_destino_id = VALUES(peer_destino_id)";

        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, m.getId());
            ps.setString(2, m.getRemitenteId() != null ? m.getRemitenteId().toString() : null);
            ps.setString(3, m.getDestinatarioUsuarioId() != null ? m.getDestinatarioUsuarioId().toString() : null);
            ps.setString(4, m.getCanalId() != null ? m.getCanalId().toString() : null);
            ps.setString(5, m.getTipo() != null ? m.getTipo().name() : Mensaje.Tipo.TEXTO.name());
            ps.setString(6, m.getContenido());
            ps.setTimestamp(7, m.getFechaEnvio() != null ? Timestamp.from(m.getFechaEnvio()) : Timestamp.from(Instant.now()));
            ps.setString(8, m.getPeerRemitenteId());
            ps.setString(9, m.getPeerDestinoId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[MensajeRepo] Error guardando mensaje " + m.getId() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * ✅ NUEVO: Actualiza un mensaje existente y actualiza su timestamp automáticamente.
     */
    public boolean actualizar(Mensaje m) {
        if (m == null || m.getId() == null) return false;

        // Actualizar timestamp al momento actual
        m.setFechaEnvio(Instant.now());

        String sql = "UPDATE mensajes SET contenido=?, fecha_envio=?, peer_remitente_id=?, peer_destino_id=? WHERE id=?";

        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, m.getContenido());
            ps.setTimestamp(2, Timestamp.from(m.getFechaEnvio()));
            ps.setString(3, m.getPeerRemitenteId());
            ps.setString(4, m.getPeerDestinoId());
            ps.setString(5, m.getId());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                System.out.println("[MensajeRepo] ✓ Mensaje actualizado con timestamp: " + m.getId());
            }
            return affected > 0;
        } catch (SQLException e) {
            System.err.println("[MensajeRepo] Error actualizando mensaje " + m.getId() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Método auxiliar para convertir un ResultSet en un objeto Mensaje.
     * ✅ ACTUALIZADO: Ahora mapea también los campos de peer.
     */
    private Mensaje mapearMensaje(ResultSet rs) throws SQLException {
        Mensaje m = new Mensaje();

        // Conversión segura de String a UUID
        m.setId(toUUID(rs.getString("id")));
        m.setRemitenteId(toUUID(rs.getString("remitente_id")));
        m.setDestinatarioUsuarioId(toUUID(rs.getString("destinatario_usuario_id")));
        m.setCanalId(toUUID(rs.getString("canal_id")));

        // ✅ NUEVO: Mapear campos de peer
        m.setPeerRemitenteId(rs.getString("peer_remitente_id"));
        m.setPeerDestinoId(rs.getString("peer_destino_id"));

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

    /**
     * ✅ NUEVO: Busca un mensaje por su contenido (útil para buscar mensajes de audio por fileId)
     */
    public Mensaje buscarPorContenido(String contenido) {
        if (contenido == null || contenido.trim().isEmpty()) return null;

        String sql = "SELECT id, remitente_id, destinatario_usuario_id, canal_id, tipo, contenido, fecha_envio, peer_remitente_id, peer_destino_id " +
                "FROM mensajes " +
                "WHERE contenido = ? " +
                "LIMIT 1";

        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, contenido);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearMensaje(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[MensajeRepo] Error buscando mensaje por contenido: " + e.getMessage());
        }

        return null;
    }
}