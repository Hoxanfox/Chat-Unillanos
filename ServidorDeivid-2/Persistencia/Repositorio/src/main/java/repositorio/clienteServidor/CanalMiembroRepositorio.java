package repositorio.clienteServidor;

import dominio.clienteServidor.relaciones.CanalMiembro;
import repositorio.comunicacion.MySQLManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CanalMiembroRepositorio {
    private final MySQLManager mysql;
    public CanalMiembroRepositorio() { this.mysql = MySQLManager.getInstance(); }

    public List<CanalMiembro> obtenerTodosParaSync() {
        List<CanalMiembro> lista = new ArrayList<>();
        String sql = "SELECT canal_id, usuario_id FROM canal_miembros ORDER BY canal_id, usuario_id ASC";
        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new CanalMiembro(
                        UUID.fromString(rs.getString("canal_id")),
                        UUID.fromString(rs.getString("usuario_id"))
                ));
            }
        } catch (SQLException e) { 
            System.err.println("[RepoMiembro] Error obteniendo miembros para sync: " + e.getMessage()); 
        }
        return lista;
    }

    public boolean guardar(CanalMiembro cm) {
        String sql = "INSERT IGNORE INTO canal_miembros (canal_id, usuario_id) VALUES (?, ?)";
        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cm.getCanalId().toString());
            ps.setString(2, cm.getUsuarioId().toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[RepoMiembro] Error (Falta Canal o Usuario): " + e.getMessage());
            return false;
        }
    }

    /**
     * ✅ NUEVO: Verifica si un usuario es miembro de un canal.
     */
    public boolean esMiembroDelCanal(String canalId, String usuarioId) {
        String sql = "SELECT COUNT(*) FROM canal_miembros WHERE canal_id = ? AND usuario_id = ?";
        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, UUID.fromString(canalId).toString());
            ps.setString(2, UUID.fromString(usuarioId).toString());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("[RepoMiembro] Error verificando membresía: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("[RepoMiembro] Error convirtiendo IDs a UUID: " + e.getMessage());
        }
        return false;
    }

    /**
     * ✅ NUEVO: Obtiene todos los usuarios miembros de un canal.
     */
    public List<String> obtenerMiembrosDelCanal(String canalId) {
        List<String> miembros = new ArrayList<>();
        String sql = "SELECT usuario_id FROM canal_miembros WHERE canal_id = ?";
        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, UUID.fromString(canalId).toString());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    miembros.add(rs.getString("usuario_id"));
                }
            }
        } catch (SQLException e) {
            System.err.println("[RepoMiembro] Error obteniendo miembros: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("[RepoMiembro] Error convirtiendo canal ID a UUID: " + e.getMessage());
        }
        return miembros;
    }
}