package repositorio.mensaje;

import dominio.MensajeEnviadoCanal;
import dominio.MensajeRecibidoCanal;
import dto.canales.DTOMensajeCanal;
import repositorio.conexion.GestorConexionH2;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Implementación del repositorio de mensajes de canal con H2.
 * Maneja la persistencia de mensajes enviados y recibidos.
 */
public class RepositorioMensajeCanalImpl implements IRepositorioMensajeCanal {

    private final GestorConexionH2 gestorConexion;

    public RepositorioMensajeCanalImpl() {
        this.gestorConexion = GestorConexionH2.getInstancia();
    }

    @Override
    public CompletableFuture<Boolean> guardarMensajeEnviado(MensajeEnviadoCanal mensaje) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT INTO mensaje_enviado_canal " +
                    "(id_mensaje_enviado_canal, contenido, fecha_envio, tipo, id_remitente, id_destinatario_canal) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";

            try (Connection conn = gestorConexion.getConexion();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setObject(1, mensaje.getIdMensajeEnviadoCanal());
                stmt.setBytes(2, mensaje.getContenido());
                stmt.setTimestamp(3, Timestamp.valueOf(mensaje.getFechaEnvio()));
                stmt.setString(4, mensaje.getTipo());
                stmt.setObject(5, mensaje.getIdRemitente());
                stmt.setObject(6, mensaje.getIdDestinatarioCanal());

                int filasAfectadas = stmt.executeUpdate();
                return filasAfectadas > 0;

            } catch (SQLException e) {
                System.err.println("Error al guardar mensaje enviado a canal: " + e.getMessage());
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> guardarMensajeRecibido(MensajeRecibidoCanal mensaje) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "MERGE INTO mensaje_recibido_canal " +
                    "(id_mensaje, contenido, fecha_envio, tipo, id_destinatario, id_remitente_canal) " +
                    "KEY(id_mensaje) VALUES (?, ?, ?, ?, ?, ?)";

            try (Connection conn = gestorConexion.getConexion();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setObject(1, mensaje.getIdMensaje());
                stmt.setBytes(2, mensaje.getContenido());
                stmt.setTimestamp(3, Timestamp.valueOf(mensaje.getFechaEnvio()));
                stmt.setString(4, mensaje.getTipo());
                stmt.setObject(5, mensaje.getIdDestinatario());
                stmt.setObject(6, mensaje.getIdRemitenteCanal());

                stmt.executeUpdate();
                return true;

            } catch (SQLException e) {
                System.err.println("Error al guardar mensaje recibido de canal: " + e.getMessage());
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<List<DTOMensajeCanal>> obtenerHistorialCanal(String canalId, String usuarioId, int limite) {
        return CompletableFuture.supplyAsync(() -> {
            List<DTOMensajeCanal> historial = new ArrayList<>();

            String sql =
                "SELECT 'enviado' as origen, " +
                "       CAST(id_mensaje_enviado_canal AS VARCHAR) as mensaje_id, " +
                "       CAST(id_destinatario_canal AS VARCHAR) as canal_id, " +
                "       CAST(id_remitente AS VARCHAR) as remitente_id, " +
                "       contenido, tipo, fecha_envio " +
                "FROM mensaje_enviado_canal " +
                "WHERE id_destinatario_canal = ? " +
                "UNION ALL " +
                "SELECT 'recibido' as origen, " +
                "       CAST(id_mensaje AS VARCHAR) as mensaje_id, " +
                "       CAST(id_remitente_canal AS VARCHAR) as canal_id, " +
                "       NULL as remitente_id, " +
                "       contenido, tipo, fecha_envio " +
                "FROM mensaje_recibido_canal " +
                "WHERE id_remitente_canal = ? " +
                "ORDER BY fecha_envio DESC " +
                "LIMIT ?";

            try (Connection conn = gestorConexion.getConexion();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                UUID canalUUID = UUID.fromString(canalId);
                stmt.setObject(1, canalUUID);
                stmt.setObject(2, canalUUID);
                stmt.setInt(3, limite);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        DTOMensajeCanal dto = new DTOMensajeCanal();
                        dto.setMensajeId(rs.getString("mensaje_id"));
                        dto.setCanalId(rs.getString("canal_id"));
                        dto.setTipo(rs.getString("tipo"));
                        dto.setFechaEnvio(rs.getTimestamp("fecha_envio").toLocalDateTime());

                        String origen = rs.getString("origen");
                        boolean esPropio = "enviado".equals(origen);
                        dto.setEsPropio(esPropio);

                        byte[] contenidoBytes = rs.getBytes("contenido");
                        if (contenidoBytes != null) {
                            String contenidoStr = new String(contenidoBytes);
                            // ✅ FIX: Comparación case-insensitive para soportar "TEXT"/"texto"
                            if ("TEXT".equalsIgnoreCase(dto.getTipo())) {
                                dto.setContenido(contenidoStr);
                            } else {
                                dto.setFileId(contenidoStr);
                            }
                        }

                        if (esPropio) {
                            dto.setRemitenteId(usuarioId);
                            dto.setNombreRemitente("Tú");
                        } else {
                            dto.setRemitenteId(rs.getString("remitente_id"));
                            dto.setNombreRemitente("Usuario");
                        }

                        historial.add(dto);
                    }
                }

            } catch (SQLException e) {
                System.err.println("Error al obtener historial de canal: " + e.getMessage());
            }

            return historial;
        });
    }

    @Override
    public CompletableFuture<Void> sincronizarHistorial(String canalId, String usuarioId, List<DTOMensajeCanal> mensajes) {
        return CompletableFuture.runAsync(() -> {
            eliminarMensajesDeCanal(canalId).join();

            for (DTOMensajeCanal dto : mensajes) {
                MensajeRecibidoCanal mensaje = convertirDTOAMensajeRecibido(dto, usuarioId);
                guardarMensajeRecibido(mensaje).join();
            }

            System.out.println("Historial de canal " + canalId + " sincronizado: " + mensajes.size() + " mensajes.");
        });
    }

    @Override
    public CompletableFuture<Boolean> eliminarMensajesDeCanal(String canalId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql1 = "DELETE FROM mensaje_enviado_canal WHERE id_destinatario_canal = ?";
            String sql2 = "DELETE FROM mensaje_recibido_canal WHERE id_remitente_canal = ?";

            try (Connection conn = gestorConexion.getConexion();
                 PreparedStatement stmt1 = conn.prepareStatement(sql1);
                 PreparedStatement stmt2 = conn.prepareStatement(sql2)) {

                UUID canalUUID = UUID.fromString(canalId);
                stmt1.setObject(1, canalUUID);
                stmt2.setObject(1, canalUUID);

                stmt1.executeUpdate();
                stmt2.executeUpdate();
                return true;

            } catch (SQLException e) {
                System.err.println("Error al eliminar mensajes de canal: " + e.getMessage());
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<Integer> contarMensajesNoLeidos(String canalId, String usuarioId) {
        return CompletableFuture.supplyAsync(() -> {
            // TODO: Implementar cuando se agregue campo "leido" a las tablas
            return 0;
        });
    }

    private MensajeRecibidoCanal convertirDTOAMensajeRecibido(DTOMensajeCanal dto, String usuarioId) {
        MensajeRecibidoCanal mensaje = new MensajeRecibidoCanal();
        
        mensaje.setIdMensaje(UUID.fromString(dto.getMensajeId()));
        mensaje.setIdRemitenteCanal(UUID.fromString(dto.getCanalId()));
        mensaje.setIdDestinatario(UUID.fromString(usuarioId)); // ✅ FIX: Establecer el destinatario
        mensaje.setTipo(dto.getTipo());
        mensaje.setFechaEnvio(dto.getFechaEnvio());

        // ✅ FIX: Comparación case-insensitive para soportar "TEXT", "texto", "AUDIO", "audio"
        String contenidoStr = "TEXT".equalsIgnoreCase(dto.getTipo())
            ? dto.getContenido()
            : dto.getFileId();

        if (contenidoStr != null) {
            mensaje.setContenido(contenidoStr.getBytes());
        }

        return mensaje;
    }
}
