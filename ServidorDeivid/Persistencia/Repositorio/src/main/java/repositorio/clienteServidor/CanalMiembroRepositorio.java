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
        } catch (SQLException e) { e.printStackTrace(); }
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
}