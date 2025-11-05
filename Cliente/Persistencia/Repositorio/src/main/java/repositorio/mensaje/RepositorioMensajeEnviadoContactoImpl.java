package repositorio.mensaje;

import dominio.MensajeEnviadoContacto;
import repositorio.conexion.GestorConexionH2;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implementación del repositorio de mensajes enviados a contactos.
 */
public class RepositorioMensajeEnviadoContactoImpl implements IRepositorioMensajeEnviadoContacto {

    private final GestorConexionH2 gestorConexion;

    public RepositorioMensajeEnviadoContactoImpl() {
        this.gestorConexion = GestorConexionH2.getInstancia();
    }

    @Override
    public void guardar(MensajeEnviadoContacto mensaje) {
        String sql = """
            INSERT INTO mensaje_enviado_contacto 
            (id_mensaje_enviado_contacto, contenido, fecha_envio, tipo, id_remitente, id_destinatario_usuario)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = gestorConexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, mensaje.getIdMensajeEnviadoContacto());
            stmt.setBytes(2, mensaje.getContenido());
            stmt.setTimestamp(3, mensaje.getFechaEnvio() != null ? Timestamp.valueOf(mensaje.getFechaEnvio()) : null);
            stmt.setString(4, mensaje.getTipo());
            stmt.setObject(5, mensaje.getIdRemitente());
            stmt.setObject(6, mensaje.getIdDestinatarioUsuario());

            stmt.executeUpdate();
            System.out.println("✅ [RepositorioMensajeEnviado]: Mensaje guardado");

        } catch (SQLException e) {
            System.err.println("❌ [RepositorioMensajeEnviado]: Error: " + e.getMessage());
            throw new RuntimeException("Error al guardar mensaje", e);
        }
    }

    @Override
    public MensajeEnviadoContacto obtenerPorId(UUID idMensaje) {
        String sql = "SELECT * FROM mensaje_enviado_contacto WHERE id_mensaje_enviado_contacto = ?";

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
    public List<MensajeEnviadoContacto> obtenerPorRemitente(UUID idRemitente) {
        String sql = "SELECT * FROM mensaje_enviado_contacto WHERE id_remitente = ? ORDER BY fecha_envio DESC";
        List<MensajeEnviadoContacto> mensajes = new ArrayList<>();

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
    public List<MensajeEnviadoContacto> obtenerPorDestinatario(UUID idDestinatario) {
        String sql = "SELECT * FROM mensaje_enviado_contacto WHERE id_destinatario_usuario = ? ORDER BY fecha_envio DESC";
        List<MensajeEnviadoContacto> mensajes = new ArrayList<>();

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
    public List<MensajeEnviadoContacto> obtenerConversacion(UUID idRemitente, UUID idDestinatario) {
        String sql = """
            SELECT * FROM mensaje_enviado_contacto 
            WHERE id_remitente = ? AND id_destinatario_usuario = ? 
            ORDER BY fecha_envio ASC
        """;
        List<MensajeEnviadoContacto> mensajes = new ArrayList<>();

        try (Connection conn = gestorConexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, idRemitente);
            stmt.setObject(2, idDestinatario);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                mensajes.add(mapearResultSet(rs));
            }
            
            System.out.println("✅ Obtenidos " + mensajes.size() + " mensajes de conversación");
            return mensajes;

        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar conversación", e);
        }
    }

    @Override
    public void eliminar(UUID idMensaje) {
        String sql = "DELETE FROM mensaje_enviado_contacto WHERE id_mensaje_enviado_contacto = ?";

        try (Connection conn = gestorConexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, idMensaje);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar mensaje", e);
        }
    }

    private MensajeEnviadoContacto mapearResultSet(ResultSet rs) throws SQLException {
        MensajeEnviadoContacto mensaje = new MensajeEnviadoContacto();
        mensaje.setIdMensajeEnviadoContacto((UUID) rs.getObject("id_mensaje_enviado_contacto"));
        mensaje.setContenido(rs.getBytes("contenido"));
        
        Timestamp timestamp = rs.getTimestamp("fecha_envio");
        if (timestamp != null) {
            mensaje.setFechaEnvio(timestamp.toLocalDateTime());
        }
        
        mensaje.setTipo(rs.getString("tipo"));
        mensaje.setIdRemitente((UUID) rs.getObject("id_remitente"));
        mensaje.setIdDestinatarioUsuario((UUID) rs.getObject("id_destinatario_usuario"));
        
        return mensaje;
    }
}
