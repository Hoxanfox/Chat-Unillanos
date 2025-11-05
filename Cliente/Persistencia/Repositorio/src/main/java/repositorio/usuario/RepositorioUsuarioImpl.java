package repositorio.usuario;

import dominio.Usuario;
import repositorio.conexion.GestorConexionH2;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implementación del repositorio de usuarios con H2.
 * Operaciones CRUD completas.
 */
public class RepositorioUsuarioImpl implements IRepositorioUsuario {

    private final GestorConexionH2 gestorConexion;

    public RepositorioUsuarioImpl() {
        this.gestorConexion = GestorConexionH2.getInstancia();
    }

    @Override
    public void guardar(Usuario usuario) {
        String sql = """
            INSERT INTO usuarios (id_usuario, nombre, email, estado, foto, ip, fecha_registro, photoIdServidor)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = gestorConexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, usuario.getIdUsuario());
            stmt.setString(2, usuario.getNombre());
            stmt.setString(3, usuario.getEmail());
            stmt.setString(4, usuario.getEstado());

            if (usuario.getFoto() != null) {
                stmt.setBytes(5, usuario.getFoto());
            } else {
                stmt.setNull(5, Types.BLOB);
            }

            stmt.setString(6, usuario.getIp());
            stmt.setTimestamp(7, usuario.getFechaRegistro() != null ? Timestamp.valueOf(usuario.getFechaRegistro()) : null);
            stmt.setString(8, usuario.getPhotoIdServidor());

            stmt.executeUpdate();
            System.out.println("✅ [RepositorioUsuario]: Usuario guardado - ID: " + usuario.getIdUsuario());

        } catch (SQLException e) {
            System.err.println("❌ [RepositorioUsuario]: Error al guardar usuario: " + e.getMessage());
            throw new RuntimeException("Error al guardar usuario", e);
        }
    }

    @Override
    public Usuario obtenerPorId(UUID idUsuario) {
        String sql = "SELECT * FROM usuarios WHERE id_usuario = ?";

        try (Connection conn = gestorConexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, idUsuario);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapearResultSet(rs);
            }

            return null;

        } catch (SQLException e) {
            System.err.println("❌ [RepositorioUsuario]: Error al obtener usuario: " + e.getMessage());
            throw new RuntimeException("Error al consultar usuario", e);
        }
    }

    @Override
    public Usuario obtenerPorEmail(String email) {
        String sql = "SELECT * FROM usuarios WHERE email = ?";

        try (Connection conn = gestorConexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapearResultSet(rs);
            }

            return null;

        } catch (SQLException e) {
            System.err.println("❌ [RepositorioUsuario]: Error al obtener usuario: " + e.getMessage());
            throw new RuntimeException("Error al consultar usuario", e);
        }
    }

    @Override
    public void actualizar(Usuario usuario) {
        String sql = """
            UPDATE usuarios 
            SET nombre = ?, email = ?, estado = ?, foto = ?, ip = ?, photoIdServidor = ?
            WHERE id_usuario = ?
        """;

        try (Connection conn = gestorConexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, usuario.getNombre());
            stmt.setString(2, usuario.getEmail());
            stmt.setString(3, usuario.getEstado());

            if (usuario.getFoto() != null) {
                stmt.setBytes(4, usuario.getFoto());
            } else {
                stmt.setNull(4, Types.BLOB);
            }

            stmt.setString(5, usuario.getIp());
            stmt.setString(6, usuario.getPhotoIdServidor());
            stmt.setObject(7, usuario.getIdUsuario());

            int filasActualizadas = stmt.executeUpdate();
            System.out.println("✅ [RepositorioUsuario]: Usuario actualizado - Filas: " + filasActualizadas);

        } catch (SQLException e) {
            System.err.println("❌ [RepositorioUsuario]: Error al actualizar usuario: " + e.getMessage());
            throw new RuntimeException("Error al actualizar usuario", e);
        }
    }

    @Override
    public void eliminar(UUID idUsuario) {
        String sql = "DELETE FROM usuarios WHERE id_usuario = ?";

        try (Connection conn = gestorConexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, idUsuario);
            int filasEliminadas = stmt.executeUpdate();

            System.out.println("✅ [RepositorioUsuario]: Usuario eliminado - Filas: " + filasEliminadas);

        } catch (SQLException e) {
            System.err.println("❌ [RepositorioUsuario]: Error al eliminar usuario: " + e.getMessage());
            throw new RuntimeException("Error al eliminar usuario", e);
        }
    }

    @Override
    public List<Usuario> obtenerTodos() {
        String sql = "SELECT * FROM usuarios ORDER BY fecha_registro DESC";
        List<Usuario> usuarios = new ArrayList<>();

        try (Connection conn = gestorConexion.getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                usuarios.add(mapearResultSet(rs));
            }

            System.out.println("✅ [RepositorioUsuario]: Obtenidos " + usuarios.size() + " usuarios");
            return usuarios;

        } catch (SQLException e) {
            System.err.println("❌ [RepositorioUsuario]: Error al obtener todos los usuarios: " + e.getMessage());
            throw new RuntimeException("Error al consultar usuarios", e);
        }
    }

    @Override
    public boolean existePorEmail(String email) {
        String sql = "SELECT COUNT(*) FROM usuarios WHERE email = ?";

        try (Connection conn = gestorConexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

            return false;

        } catch (SQLException e) {
            System.err.println("❌ [RepositorioUsuario]: Error al verificar existencia: " + e.getMessage());
            throw new RuntimeException("Error al verificar usuario", e);
        }
    }

    @Override
    public void actualizarEstado(UUID idUsuario, String nuevoEstado) {
        String sql = "UPDATE usuarios SET estado = ? WHERE id_usuario = ?";

        try (Connection conn = gestorConexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nuevoEstado);
            stmt.setObject(2, idUsuario);

            int filasActualizadas = stmt.executeUpdate();

            if (filasActualizadas > 0) {
                System.out.println("✅ [RepositorioUsuario]: Estado actualizado a '" + nuevoEstado + "' para usuario: " + idUsuario);
            } else {
                System.out.println("⚠️ [RepositorioUsuario]: No se encontró usuario con ID: " + idUsuario);
            }

        } catch (SQLException e) {
            System.err.println("❌ [RepositorioUsuario]: Error al actualizar estado: " + e.getMessage());
            throw new RuntimeException("Error al actualizar estado del usuario", e);
        }
    }

    /**
     * Mapea un ResultSet a una entidad Usuario.
     */
    private Usuario mapearResultSet(ResultSet rs) throws SQLException {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario((UUID) rs.getObject("id_usuario"));
        usuario.setNombre(rs.getString("nombre"));
        usuario.setEmail(rs.getString("email"));
        usuario.setEstado(rs.getString("estado"));
        usuario.setFoto(rs.getBytes("foto"));
        usuario.setIp(rs.getString("ip"));

        Timestamp timestamp = rs.getTimestamp("fecha_registro");
        if (timestamp != null) {
            usuario.setFechaRegistro(timestamp.toLocalDateTime());
        }

        usuario.setPhotoIdServidor(rs.getString("photoIdServidor"));

        return usuario;
    }
}
