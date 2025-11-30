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
        // Ordenar por fecha de creación para mantener consistencia entre peers
        // pero sin incluir la fecha en el hash para evitar diferencias falsas
        String sql = "SELECT * FROM usuarios ORDER BY fecha_creacion ASC, id ASC";
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
        // ✅ MEJORADO: Actualizar timestamp automáticamente cuando se modifica un usuario
        String sql = "INSERT INTO usuarios (id, nombre, email, foto, peer_padre, contrasena, ip, estado, fecha_creacion) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "nombre=VALUES(nombre), " +
                "email=VALUES(email), " +
                "foto=VALUES(foto), " +
                "ip=VALUES(ip), " +
                "estado=VALUES(estado), " +
                "contrasena=VALUES(contrasena), " +
                "fecha_creacion=VALUES(fecha_creacion)"; // ✅ NUEVO: Actualizar timestamp en modificaciones

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

            // ✅ NUEVO: Si el usuario ya existe (UPDATE), usar timestamp actual
            // Si es nuevo (INSERT), usar el timestamp que trae
            Instant timestamp = u.getFechaCreacion();
            if (timestamp == null) {
                timestamp = Instant.now();
                u.setFechaCreacion(timestamp);
            }
            ps.setTimestamp(9, Timestamp.from(timestamp));

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[RepoUsuario] Error guardando: " + e.getMessage());
            return false;
        }
    }

    /**
     * Busca un usuario por su email (para login)
     */
    public Usuario buscarPorEmail(String email) {
        String sql = "SELECT * FROM usuarios WHERE email = ?";
        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[RepoUsuario] Error buscando por email: " + e.getMessage());
        }
        return null;
    }

    /**
     * Busca un usuario por su ID
     */
    public Usuario buscarPorId(UUID id) {
        String sql = "SELECT * FROM usuarios WHERE id = ?";
        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[RepoUsuario] Error buscando por ID: " + e.getMessage());
        }
        return null;
    }

    /**
     * Busca un usuario por su ID (versión con String)
     */
    public Usuario buscarPorId(String id) {
        if (id == null || id.isEmpty()) return null;
        try {
            return buscarPorId(UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            System.err.println("[RepoUsuario] ID inválido: " + id);
            return null;
        }
    }

    /**
     * Actualiza el estado de un usuario
     */
    public boolean actualizarEstado(UUID id, Usuario.Estado estado) {
        String sql = "UPDATE usuarios SET estado = ?, fecha_creacion = ? WHERE id = ?";
        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, estado.name());
            ps.setTimestamp(2, Timestamp.from(Instant.now())); // ✅ Actualizar timestamp
            ps.setString(3, id.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[RepoUsuario] Error actualizando estado: " + e.getMessage());
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