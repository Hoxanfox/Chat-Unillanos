package repositorio.clienteServidor;

import dominio.clienteServidor.Usuario;
import repositorio.comunicacion.MySQLManager;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UsuarioRepositorio {
    private final MySQLManager mysql;

    public UsuarioRepositorio() {
        this.mysql = MySQLManager.getInstance();
    }

    public List<Usuario> obtenerTodosParaSync() {
        List<Usuario> lista = new ArrayList<>();
        String sql = "SELECT * FROM usuarios ORDER BY id ASC";
        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return lista;
    }

    public boolean guardar(Usuario u) {
        String sql = "INSERT INTO usuarios (id, nombre, email, foto, peer_padre, contrasena, ip, estado, fecha_creacion) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE nombre=VALUES(nombre), email=VALUES(email), foto=VALUES(foto), ip=VALUES(ip), estado=VALUES(estado)";
        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, u.getId().toString());
            ps.setString(2, u.getNombre());
            ps.setString(3, u.getEmail());
            ps.setString(4, u.getFoto());
            ps.setString(5, u.getPeerPadre() != null ? u.getPeerPadre().toString() : null);
            ps.setString(6, u.getContrasena());
            ps.setString(7, u.getIp());
            ps.setString(8, u.getEstado().name());
            ps.setTimestamp(9, Timestamp.from(u.getFechaCreacion()));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[RepoUsuario] Error guardando: " + e.getMessage());
            return false;
        }
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setId(UUID.fromString(rs.getString("id")));
        u.setNombre(rs.getString("nombre"));
        u.setEmail(rs.getString("email"));
        u.setFoto(rs.getString("foto"));
        String pid = rs.getString("peer_padre");
        if (pid != null) u.setPeerPadre(UUID.fromString(pid));
        u.setContrasena(rs.getString("contrasena"));
        u.setIp(rs.getString("ip"));
        try { u.setEstado(Usuario.Estado.valueOf(rs.getString("estado"))); } catch (Exception e) {}
        u.setFechaCreacion(rs.getTimestamp("fecha_creacion").toInstant());
        return u;
    }
}