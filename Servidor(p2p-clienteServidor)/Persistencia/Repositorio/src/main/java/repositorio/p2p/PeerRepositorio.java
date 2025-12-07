package repositorio.p2p;

import dominio.p2p.Peer;
import dominio.p2p.Peer.Estado;
import logger.LoggerCentral;
import repositorio.comunicacion.MySQLManager;
import observador.IObservador;
import observador.ISujeto;

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
 * ✅ ACTUALIZADO: Implementa ISujeto para notificar cambios a la interfaz
 */
public class PeerRepositorio implements ISujeto {

    private static final String TAG = "PeerRepositorio";
    private final MySQLManager mysql;
    private final List<IObservador> observadores;

    public PeerRepositorio() {
        this.mysql = MySQLManager.getInstance();
        this.observadores = new ArrayList<>();
    }

    // ✅ NUEVO: Implementación del patrón Observador
    @Override
    public void registrarObservador(IObservador observador) {
        if (observador != null && !observadores.contains(observador)) {
            observadores.add(observador);
            LoggerCentral.info(TAG, "✓ Observador registrado");
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
     * Guarda o actualiza un peer en la base de datos.
     * ✅ ACTUALIZADO: Ahora notifica eventos cuando se guarda o actualiza un peer
     * @param peer Objeto Peer (id, ip, estado, fechaCreacion)
     * @param socketInfo Información adicional sobre el socket (ej. "host:port"), puede ser null
     * @return true si la operación se completó correctamente
     */
    public boolean guardarOActualizarPeer(Peer peer, String socketInfo) {
        if (peer == null || peer.getId() == null) {
            LoggerCentral.warn(TAG, "Intento de guardar peer nulo o sin ID.");
            return false;
        }

        // ✅ NUEVO: Verificar si es actualización o creación
        boolean esNuevo = (obtenerPorId(peer.getId()) == null);

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

            if (rows > 0) {
                LoggerCentral.debug(TAG, "Peer guardado/actualizado: " + peer.getId() + " | " + socketInfo + " | Estado: " + peer.getEstado());

                // ✅ NUEVO: Notificar a los observadores según el tipo de operación
                if (esNuevo) {
                    notificarObservadores("PEER_CREADO", peer);
                } else {
                    notificarObservadores("PEER_ACTUALIZADO", peer);
                }
            } else {
                LoggerCentral.warn(TAG, "No se afectaron filas al guardar peer: " + peer.getId());
            }

            return rows > 0;
        } catch (SQLException e) {
            LoggerCentral.error(TAG, "Error al guardar/actualizar peer: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public Peer obtenerPorId(UUID id) {
        if (id == null) {
            LoggerCentral.warn(TAG, "obtenerPorId llamado con UUID nulo.");
            return null;
        }

        String sql = "SELECT id, ip, socket_info, estado, fecha_creacion FROM peers WHERE id = ? LIMIT 1";
        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String ip = rs.getString("ip");
                    String socketInfo = rs.getString("socket_info");
                    String estadoStr = rs.getString("estado");
                    Estado estado = Estado.OFFLINE;
                    if (estadoStr != null) {
                        try { estado = Estado.valueOf(estadoStr); } 
                        catch (IllegalArgumentException e) { LoggerCentral.debug(TAG, "Estado invalido: " + estadoStr); }
                    }
                    Timestamp ts = rs.getTimestamp("fecha_creacion");
                    Instant fecha = ts != null ? ts.toInstant() : Instant.now();

                    Peer peer = new Peer(id, ip, null, estado, fecha);
                    LoggerCentral.debug(TAG, "Peer obtenido por ID: " + id + " | IP: " + ip + " | Estado: " + estado);
                    return peer;
                }
            }
        } catch (SQLException e) {
            LoggerCentral.error(TAG, "Error obtenerPorId: " + e.getMessage());
        }

        LoggerCentral.debug(TAG, "Peer no encontrado por ID: " + id);
        return null;
    }

    /**
     * Obtiene un peer por su socketInfo (host:port). Devuelve null si no existe.
     */
    public Peer obtenerPorSocketInfo(String socketInfo) {
        if (socketInfo == null || socketInfo.isEmpty()) {
            LoggerCentral.warn(TAG, "obtenerPorSocketInfo llamado con socketInfo vacío.");
            return null;
        }

        String sql = "SELECT id, ip, socket_info, estado, fecha_creacion FROM peers WHERE socket_info = ? LIMIT 1";
        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, socketInfo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    UUID id = null;
                    String idStr = rs.getString("id");
                    if (idStr != null) {
                        try { id = UUID.fromString(idStr); } 
                        catch (IllegalArgumentException e) { LoggerCentral.debug(TAG, "ID de peer invalido: " + idStr); }
                    }
                    String ip = rs.getString("ip");
                    String estadoStr = rs.getString("estado");
                    Estado estado = Estado.OFFLINE;
                    if (estadoStr != null) {
                        try { estado = Estado.valueOf(estadoStr); } 
                        catch (IllegalArgumentException e) { LoggerCentral.debug(TAG, "Estado invalido: " + estadoStr); }
                    }
                    Timestamp ts = rs.getTimestamp("fecha_creacion");
                    Instant fecha = ts != null ? ts.toInstant() : Instant.now();

                    LoggerCentral.debug(TAG, "Peer obtenido por socketInfo: " + socketInfo + " | ID: " + id + " | Estado: " + estado);
                    return new Peer(id, ip, null, estado, fecha);
                }
            }
        } catch (SQLException e) {
            LoggerCentral.error(TAG, "Error obtenerPorSocketInfo: " + e.getMessage());
            e.printStackTrace();
        }

        LoggerCentral.debug(TAG, "Peer no encontrado por socketInfo: " + socketInfo);
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
            LoggerCentral.error(TAG, "Error listarSocketInfos: " + e.getMessage());
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
                String idStr = rs.getString("id");
                if (idStr != null) {
                    try { id = UUID.fromString(idStr); } 
                    catch (IllegalArgumentException e) { /* ID invalido, se ignora */ }
                }
                String ip = rs.getString("ip");
                String socketInfo = rs.getString("socket_info");
                int puerto = -1;
                if (socketInfo != null && socketInfo.contains(":")) {
                    try {
                        String[] parts = socketInfo.split(":");
                        puerto = Integer.parseInt(parts[1]);
                    } catch (NumberFormatException e) { /* Puerto invalido, se usa -1 */ }
                }
                String estadoStr = rs.getString("estado");
                Estado estado = Estado.OFFLINE;
                if (estadoStr != null) {
                    try { estado = Estado.valueOf(estadoStr); } 
                    catch (IllegalArgumentException e) { /* Estado invalido, se usa OFFLINE */ }
                }
                Timestamp ts = rs.getTimestamp("fecha_creacion");
                Instant fecha = ts != null ? ts.toInstant() : Instant.now();
                lista.add(new PeerInfo(id, ip, puerto, estado, fecha));
            }
        } catch (SQLException e) {
            LoggerCentral.error(TAG, "Error listarPeersInfo: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Devuelve solo los peers que están ONLINE.
     * Útil para sincronización y broadcast selectivo.
     */
    public List<PeerInfo> listarPeersOnline() {
        List<PeerInfo> lista = new ArrayList<>();
        String sql = "SELECT id, ip, socket_info, estado, fecha_creacion FROM peers WHERE estado = 'ONLINE'";
        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UUID id = null;
                String idStr = rs.getString("id");
                if (idStr != null) {
                    try { id = UUID.fromString(idStr); } 
                    catch (IllegalArgumentException e) { /* ID invalido, se ignora */ }
                }
                String ip = rs.getString("ip");
                String socketInfo = rs.getString("socket_info");
                int puerto = -1;
                if (socketInfo != null && socketInfo.contains(":")) {
                    try {
                        String[] parts = socketInfo.split(":");
                        puerto = Integer.parseInt(parts[1]);
                    } catch (NumberFormatException e) { /* Puerto invalido, se usa -1 */ }
                }
                String estadoStr = rs.getString("estado");
                Estado estado = Estado.ONLINE; // Ya sabemos que es ONLINE por el WHERE
                Timestamp ts = rs.getTimestamp("fecha_creacion");
                Instant fecha = ts != null ? ts.toInstant() : Instant.now();
                lista.add(new PeerInfo(id, ip, puerto, estado, fecha));
            }
        } catch (SQLException e) {
            LoggerCentral.error(TAG, "Error listarPeersOnline: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Actualiza el estado de un peer por su socketInfo (IP:puerto).
     * @param socketInfo Formato "IP:puerto" (ej. "192.168.1.9:9000")
     * @param nuevoEstado Nuevo estado del peer
     * @return true si se actualizó correctamente
     */
    public boolean actualizarEstado(String socketInfo, Estado nuevoEstado) {
        if (socketInfo == null || nuevoEstado == null) {
            LoggerCentral.warn(TAG, "actualizarEstado llamado con parámetros nulos.");
            return false;
        }

        LoggerCentral.info(TAG, "Actualizando estado por socketInfo: " + socketInfo + " -> " + nuevoEstado);

        String sql = "UPDATE peers SET estado = ? WHERE socket_info = ?";
        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado.name());
            ps.setString(2, socketInfo);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                LoggerCentral.info(TAG, "✓ Estado actualizado exitosamente: " + socketInfo + " -> " + nuevoEstado);
            } else {
                LoggerCentral.warn(TAG, "✗ No se encontró peer con socketInfo: " + socketInfo);
            }

            return rows > 0;
        } catch (SQLException e) {
            LoggerCentral.error(TAG, "Error actualizarEstado por socketInfo: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Actualiza el estado de un peer por su UUID.
     * @param uuid UUID del peer como String
     * @param nuevoEstado Nuevo estado del peer
     * @return true si se actualizó correctamente
     */
    public boolean actualizarEstadoPorUUID(String uuid, Estado nuevoEstado) {
        if (uuid == null || nuevoEstado == null) {
            LoggerCentral.warn(TAG, "actualizarEstadoPorUUID llamado con parámetros nulos.");
            return false;
        }

        LoggerCentral.info(TAG, "Actualizando estado por UUID: " + uuid + " -> " + nuevoEstado);

        String sql = "UPDATE peers SET estado = ? WHERE id = ?";
        try (Connection conn = mysql.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado.name());
            ps.setString(2, uuid);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                LoggerCentral.info(TAG, "✓ Estado actualizado exitosamente por UUID: " + uuid + " -> " + nuevoEstado);
            } else {
                LoggerCentral.warn(TAG, "✗ No se encontró peer con UUID: " + uuid);
            }

            return rows > 0;
        } catch (SQLException e) {
            LoggerCentral.error(TAG, "Error actualizarEstadoPorUUID: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
