package repositorio.p2p;

import dominio.p2p.Peer;
import dominio.p2p.Peer.Estado;
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

/**
 * Repositorio para persistir Peers en la tabla `peers`.
 */
public class PeerRepositorio {

    private final MySQLManager mysql;

    public PeerRepositorio() {
        this.mysql = MySQLManager.getInstance();
    }

    /**
     * Guarda o actualiza un peer en la base de datos.
     * @param peer Objeto Peer (id, ip, estado, fechaCreacion)
     * @param socketInfo Información adicional sobre el socket (ej. "host:port"), puede ser null
     * @return true si la operación se completó correctamente
     */
    public boolean guardarOActualizarPeer(Peer peer, String socketInfo) {
        if (peer == null || peer.getId() == null) return false;

        String sql = "INSERT INTO peers (id, ip, socket_info, estado, fecha_creacion) VALUES (?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE ip = VALUES(ip), socket_info = VALUES(socket_info), estado = VALUES(estado), fecha_creacion = VALUES(fecha_creacion)";

        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, peer.getId().toString());
            ps.setString(2, peer.getIp());
            ps.setString(3, socketInfo);
            ps.setString(4, peer.getEstado() != null ? peer.getEstado().name() : Estado.OFFLINE.name());
            Instant fecha = peer.getFechaCreacion() != null ? peer.getFechaCreacion() : Instant.now();
            ps.setTimestamp(5, Timestamp.from(fecha));
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("[PeerRepositorio] Error al guardar/actualizar peer: " + e.getMessage());
            return false;
        }
    }

    public Peer obtenerPorId(UUID id) {
        // Implementación mínima: pendiente (se puede añadir si es necesario)
        return null;
    }

    /**
     * Obtiene un peer por su socketInfo (host:port). Devuelve null si no existe.
     */
    public Peer obtenerPorSocketInfo(String socketInfo) {
        if (socketInfo == null || socketInfo.isEmpty()) return null;
        String sql = "SELECT id, ip, socket_info, estado, fecha_creacion FROM peers WHERE socket_info = ? LIMIT 1";
        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, socketInfo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    UUID id = null;
                    try { id = UUID.fromString(rs.getString("id")); } catch (Exception ignored) {}
                    String ip = rs.getString("ip");
                    String estadoStr = rs.getString("estado");
                    Estado estado = Estado.OFFLINE;
                    try { if (estadoStr != null) estado = Estado.valueOf(estadoStr); } catch (Exception ignored) {}
                    Timestamp ts = rs.getTimestamp("fecha_creacion");
                    Instant fecha = ts != null ? ts.toInstant() : Instant.now();
                    return new Peer(id, ip, null, estado, fecha);
                }
            }
        } catch (SQLException e) {
            System.err.println("[PeerRepositorio] Error obtenerPorSocketInfo: " + e.getMessage());
        }
        return null;
    }

    /**
     * Lista los socket_info de todos los peers guardados (ej. host:port).
     */
    public List<String> listarSocketInfos() {
        List<String> lista = new ArrayList<>();
        String sql = "SELECT socket_info FROM peers WHERE socket_info IS NOT NULL";
        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String s = rs.getString("socket_info");
                if (s != null && !s.isEmpty()) lista.add(s);
            }
        } catch (SQLException e) {
            System.err.println("[PeerRepositorio] Error listarSocketInfos: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Devuelve la lista completa de peers con id, ip, puerto, estado y fechaCreacion.
     */
    public static class PeerInfo {
        public UUID id;
        public String ip;
        public int puerto;
        public Estado estado;
        public Instant fechaCreacion;

        public PeerInfo(UUID id, String ip, int puerto, Estado estado, Instant fechaCreacion) {
            this.id = id;
            this.ip = ip;
            this.puerto = puerto;
            this.estado = estado;
            this.fechaCreacion = fechaCreacion;
        }
    }

    public List<PeerInfo> listarPeersInfo() {
        List<PeerInfo> lista = new ArrayList<>();
        String sql = "SELECT id, ip, socket_info, estado, fecha_creacion FROM peers";
        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UUID id = null;
                try { id = UUID.fromString(rs.getString("id")); } catch (Exception ignored) {}
                String ip = rs.getString("ip");
                String socketInfo = rs.getString("socket_info");
                int puerto = -1;
                if (socketInfo != null && socketInfo.contains(":")) {
                    try {
                        String[] parts = socketInfo.split(":");
                        puerto = Integer.parseInt(parts[1]);
                    } catch (Exception ignored) {}
                }
                String estadoStr = rs.getString("estado");
                Estado estado = Estado.OFFLINE;
                try { if (estadoStr != null) estado = Estado.valueOf(estadoStr); } catch (Exception ignored) {}
                Timestamp ts = rs.getTimestamp("fecha_creacion");
                Instant fecha = ts != null ? ts.toInstant() : Instant.now();
                lista.add(new PeerInfo(id, ip, puerto, estado, fecha));
            }
        } catch (SQLException e) {
            System.err.println("[PeerRepositorio] Error listarPeersInfo: " + e.getMessage());
        }
        return lista;
    }
}
