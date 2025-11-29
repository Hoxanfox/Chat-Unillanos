package repositorio.clienteServidor;

import dominio.clienteServidor.relaciones.CanalInvitacion;
import repositorio.comunicacion.MySQLManager;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Repositorio para gestionar invitaciones de canal en la base de datos.
 */
public class CanalInvitacionRepositorio {

    private final MySQLManager mysql;

    public CanalInvitacionRepositorio() {
        this.mysql = MySQLManager.getInstance();
    }

    /**
     * Obtiene todas las invitaciones para sincronización P2P.
     */
    public List<CanalInvitacion> obtenerTodosParaSync() {
        List<CanalInvitacion> lista = new ArrayList<>();
        String sql = "SELECT id, canal_id, invitador_id, invitado_id, fecha_creacion, estado " +
                     "FROM canal_invitaciones ORDER BY fecha_creacion ASC";
        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                CanalInvitacion invitacion = new CanalInvitacion(
                    UUID.fromString(rs.getString("id")),
                    UUID.fromString(rs.getString("canal_id")),
                    UUID.fromString(rs.getString("invitador_id")),
                    UUID.fromString(rs.getString("invitado_id")),
                    rs.getTimestamp("fecha_creacion").toInstant(),
                    rs.getString("estado")
                );
                lista.add(invitacion);
            }
        } catch (SQLException e) {
            System.err.println("[CanalInvitacionRepo] Error al obtener todos: " + e.getMessage());
            e.printStackTrace();
        }
        return lista;
    }

    /**
     * Guarda una nueva invitación en la base de datos.
     * ✅ MEJORADO: Usa INSERT ... ON DUPLICATE KEY UPDATE para sincronización P2P
     */
    public boolean guardar(CanalInvitacion invitacion) {
        String sql = "INSERT INTO canal_invitaciones (id, canal_id, invitador_id, invitado_id, fecha_creacion, estado) " +
                     "VALUES (?, ?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE " +
                     "canal_id = VALUES(canal_id), " +
                     "invitador_id = VALUES(invitador_id), " +
                     "invitado_id = VALUES(invitado_id), " +
                     "fecha_creacion = VALUES(fecha_creacion), " +
                     "estado = VALUES(estado)";
        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, invitacion.getId().toString());
            ps.setString(2, invitacion.getCanalId().toString());
            ps.setString(3, invitacion.getInvitadorId().toString());
            ps.setString(4, invitacion.getInvitadoId().toString());
            ps.setTimestamp(5, Timestamp.from(invitacion.getFechaCreacion()));
            ps.setString(6, invitacion.getEstado());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[CanalInvitacionRepo] Error al guardar: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * ✅ NUEVO: Guarda o actualiza una invitación completa (para sincronización P2P).
     * Este método asegura que todos los campos se sincronicen correctamente.
     */
    public boolean guardarOActualizar(CanalInvitacion invitacion) {
        CanalInvitacion existente = obtenerPorId(invitacion.getIdUUID());

        if (existente == null) {
            // No existe, insertar
            return guardar(invitacion);
        } else {
            // Existe, actualizar todos los campos
            String sql = "UPDATE canal_invitaciones SET " +
                        "canal_id = ?, invitador_id = ?, invitado_id = ?, " +
                        "fecha_creacion = ?, estado = ? WHERE id = ?";
            try (Connection conn = mysql.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, invitacion.getCanalId().toString());
                ps.setString(2, invitacion.getInvitadorId().toString());
                ps.setString(3, invitacion.getInvitadoId().toString());
                ps.setTimestamp(4, Timestamp.from(invitacion.getFechaCreacion()));
                ps.setString(5, invitacion.getEstado());
                ps.setString(6, invitacion.getId().toString());
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                System.err.println("[CanalInvitacionRepo] Error al actualizar: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }
    }

    /**
     * Obtiene una invitación por su ID.
     */
    public CanalInvitacion obtenerPorId(UUID id) {
        String sql = "SELECT id, canal_id, invitador_id, invitado_id, fecha_creacion, estado " +
                     "FROM canal_invitaciones WHERE id = ?";
        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new CanalInvitacion(
                        UUID.fromString(rs.getString("id")),
                        UUID.fromString(rs.getString("canal_id")),
                        UUID.fromString(rs.getString("invitador_id")),
                        UUID.fromString(rs.getString("invitado_id")),
                        rs.getTimestamp("fecha_creacion").toInstant(),
                        rs.getString("estado")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("[CanalInvitacionRepo] Error al obtener por ID: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Obtiene todas las invitaciones pendientes para un usuario específico.
     */
    public List<CanalInvitacion> obtenerInvitacionesPendientesPorUsuario(UUID usuarioId) {
        List<CanalInvitacion> lista = new ArrayList<>();
        String sql = "SELECT id, canal_id, invitador_id, invitado_id, fecha_creacion, estado " +
                     "FROM canal_invitaciones WHERE invitado_id = ? AND estado = 'PENDIENTE' " +
                     "ORDER BY fecha_creacion DESC";
        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, usuarioId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CanalInvitacion invitacion = new CanalInvitacion(
                        UUID.fromString(rs.getString("id")),
                        UUID.fromString(rs.getString("canal_id")),
                        UUID.fromString(rs.getString("invitador_id")),
                        UUID.fromString(rs.getString("invitado_id")),
                        rs.getTimestamp("fecha_creacion").toInstant(),
                        rs.getString("estado")
                    );
                    lista.add(invitacion);
                }
            }
        } catch (SQLException e) {
            System.err.println("[CanalInvitacionRepo] Error al obtener pendientes: " + e.getMessage());
            e.printStackTrace();
        }
        return lista;
    }

    /**
     * Verifica si ya existe una invitación pendiente para un usuario en un canal.
     */
    public boolean existeInvitacionPendiente(UUID canalId, UUID usuarioId) {
        String sql = "SELECT COUNT(*) FROM canal_invitaciones " +
                     "WHERE canal_id = ? AND invitado_id = ? AND estado = 'PENDIENTE'";
        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, canalId.toString());
            ps.setString(2, usuarioId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("[CanalInvitacionRepo] Error al verificar existencia: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 🆕 Busca una invitación pendiente específica por canal y usuario invitado.
     * Necesario para obtener el ID de invitación en las notificaciones push.
     */
    public CanalInvitacion buscarInvitacionPendiente(UUID canalId, UUID usuarioInvitadoId) {
        String sql = "SELECT id, canal_id, invitador_id, invitado_id, fecha_creacion, estado " +
                     "FROM canal_invitaciones WHERE canal_id = ? AND invitado_id = ? AND estado = 'PENDIENTE'";
        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, canalId.toString());
            ps.setString(2, usuarioInvitadoId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new CanalInvitacion(
                        UUID.fromString(rs.getString("id")),
                        UUID.fromString(rs.getString("canal_id")),
                        UUID.fromString(rs.getString("invitador_id")),
                        UUID.fromString(rs.getString("invitado_id")),
                        rs.getTimestamp("fecha_creacion").toInstant(),
                        rs.getString("estado")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("[CanalInvitacionRepo] Error al buscar invitación pendiente: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Actualiza el estado de una invitación.
     */
    public boolean actualizarEstado(UUID invitacionId, String nuevoEstado) {
        String sql = "UPDATE canal_invitaciones SET estado = ? WHERE id = ?";
        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado);
            ps.setString(2, invitacionId.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[CanalInvitacionRepo] Error al actualizar estado: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
