package repositorio.clienteServidor;

import dominio.clienteServidor.Canal;
import dominio.clienteServidor.Usuario; // Solo referencia
import repositorio.comunicacion.MySQLManager;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CanalRepositorio {
    private final MySQLManager mysql;

    public CanalRepositorio() { this.mysql = MySQLManager.getInstance(); }

    public List<Canal> obtenerTodosParaSync() {
        List<Canal> lista = new ArrayList<>();
        String sql = "SELECT * FROM canales ORDER BY id ASC";
        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Canal c = new Canal();
                c.setId(UUID.fromString(rs.getString("id")));
                c.setNombre(rs.getString("nombre"));
                c.setPeerPadre(rs.getString("peer_padre") != null ? UUID.fromString(rs.getString("peer_padre")) : null);

                // Para Merkle solo necesitamos el ID del creador en el objeto, no el objeto completo
                Usuario creador = new Usuario();
                creador.setId(UUID.fromString(rs.getString("creador_id")));
                c.setCreador(creador);

                try { c.setTipo(Canal.Tipo.valueOf(rs.getString("tipo"))); } catch (Exception e) {}
                c.setFechaCreacion(rs.getTimestamp("fecha_creacion").toInstant());
                lista.add(c);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return lista;
    }

    public boolean guardar(Canal c) {
        String sql = "INSERT INTO canales (id, peer_padre, creador_id, nombre, tipo, fecha_creacion) VALUES (?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE nombre=VALUES(nombre), tipo=VALUES(tipo)";
        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getId().toString());
            ps.setString(2, c.getPeerPadre() != null ? c.getPeerPadre().toString() : null);
            ps.setString(3, c.getCreador().getId().toString());
            ps.setString(4, c.getNombre());
            ps.setString(5, c.getTipo().name());
            ps.setTimestamp(6, Timestamp.from(c.getFechaCreacion()));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[RepoCanal] Error (Posible falta de Usuario creador): " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene un canal por su ID.
     */
    public Canal obtenerPorId(UUID id) {
        String sql = "SELECT * FROM canales WHERE id = ?";
        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Canal c = new Canal();
                    c.setId(UUID.fromString(rs.getString("id")));
                    c.setNombre(rs.getString("nombre"));
                    c.setPeerPadre(rs.getString("peer_padre") != null ? UUID.fromString(rs.getString("peer_padre")) : null);

                    Usuario creador = new Usuario();
                    creador.setId(UUID.fromString(rs.getString("creador_id")));
                    c.setCreador(creador);

                    try { c.setTipo(Canal.Tipo.valueOf(rs.getString("tipo"))); } catch (Exception e) {}
                    c.setFechaCreacion(rs.getTimestamp("fecha_creacion").toInstant());
                    return c;
                }
            }
        } catch (SQLException e) {
            System.err.println("[RepoCanal] Error al obtener canal por ID: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}