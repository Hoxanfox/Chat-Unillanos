package repositorio.mensaje;

import dominio.MensajeRecibidoContacto;
import repositorio.conexion.GestorConexionH2;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implementación del repositorio de mensajes recibidos de contactos.
 */
public class RepositorioMensajeRecibidoContactoImpl implements IRepositorioMensajeRecibidoContacto {

    private final GestorConexionH2 gestorConexion;

    public RepositorioMensajeRecibidoContactoImpl() {
        this.gestorConexion = GestorConexionH2.getInstancia();
    }

    @Override
    public void guardar(MensajeRecibidoContacto mensaje) {
        String sql = """
            INSERT INTO mensaje_recibido_contacto 
            (id_mensaje, contenido, fecha_envio, tipo, id_destinatario, id_remitente_usuario)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = gestorConexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, mensaje.getIdMensaje());
            stmt.setBytes(2, mensaje.getContenido());
            stmt.setTimestamp(3, mensaje.getFechaEnvio() != null ? Timestamp.valueOf(mensaje.getFechaEnvio()) : null);
            stmt.setString(4, mensaje.getTipo());
            stmt.setObject(5, mensaje.getIdDestinatario());
            stmt.setObject(6, mensaje.getIdRemitenteUsuario());

            stmt.executeUpdate();
            System.out.println("✅ [RepositorioMensajeRecibido]: Mensaje guardado");

        } catch (SQLException e) {
            System.err.println("❌ [RepositorioMensajeRecibido]: Error: " + e.getMessage());
            throw new RuntimeException("Error al guardar mensaje", e);
        }
    }

    @Override
    public MensajeRecibidoContacto obtenerPorId(UUID idMensaje) {
        String sql = "SELECT * FROM mensaje_recibido_contacto WHERE id_mensaje = ?";

        try (Connection conn = gestorConexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, idMensaje);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapearResultSet(rs);
            }
            return null;

        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar mensaje", e);
        }
    }

    @Override
    public List<MensajeRecibidoContacto> obtenerPorDestinatario(UUID idDestinatario) {
        String sql = "SELECT * FROM mensaje_recibido_contacto WHERE id_destinatario = ? ORDER BY fecha_envio DESC";
        List<MensajeRecibidoContacto> mensajes = new ArrayList<>();

        try (Connection conn = gestorConexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, idDestinatario);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                mensajes.add(mapearResultSet(rs));
            }
            return mensajes;

        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar mensajes", e);
        }
    }

    @Override
    public List<MensajeRecibidoContacto> obtenerPorRemitente(UUID idRemitente) {
        String sql = "SELECT * FROM mensaje_recibido_contacto WHERE id_remitente_usuario = ? ORDER BY fecha_envio DESC";
        List<MensajeRecibidoContacto> mensajes = new ArrayList<>();

        try (Connection conn = gestorConexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, idRemitente);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                mensajes.add(mapearResultSet(rs));
            }
            return mensajes;

        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar mensajes", e);
        }
    }

    @Override
    public List<MensajeRecibidoContacto> obtenerConversacion(UUID idRemitente, UUID idDestinatario) {
        String sql = """
            SELECT * FROM mensaje_recibido_contacto 
            WHERE id_remitente_usuario = ? AND id_destinatario = ? 
            ORDER BY fecha_envio ASC
        """;
        List<MensajeRecibidoContacto> mensajes = new ArrayList<>();

        try (Connection conn = gestorConexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, idRemitente);
            stmt.setObject(2, idDestinatario);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                mensajes.add(mapearResultSet(rs));
            }
            
            System.out.println("✅ Obtenidos " + mensajes.size() + " mensajes recibidos");
            return mensajes;

        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar conversación", e);
        }
    }

    @Override
    public void eliminar(UUID idMensaje) {
        String sql = "DELETE FROM mensaje_recibido_contacto WHERE id_mensaje = ?";

        try (Connection conn = gestorConexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, idMensaje);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar mensaje", e);
        }
    }

    private MensajeRecibidoContacto mapearResultSet(ResultSet rs) throws SQLException {
        MensajeRecibidoContacto mensaje = new MensajeRecibidoContacto();
        mensaje.setIdMensaje((UUID) rs.getObject("id_mensaje"));
        mensaje.setContenido(rs.getBytes("contenido"));
        
        Timestamp timestamp = rs.getTimestamp("fecha_envio");
        if (timestamp != null) {
            mensaje.setFechaEnvio(timestamp.toLocalDateTime());
        }
        
        mensaje.setTipo(rs.getString("tipo"));
        mensaje.setIdDestinatario((UUID) rs.getObject("id_destinatario"));
        mensaje.setIdRemitenteUsuario((UUID) rs.getObject("id_remitente_usuario"));
        
        return mensaje;
    }
}
