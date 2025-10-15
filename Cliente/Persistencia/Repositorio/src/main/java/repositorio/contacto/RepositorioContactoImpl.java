package repositorio.contacto;

import dominio.Contacto;
import repositorio.conexion.GestorConexionH2;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implementación del repositorio de contactos con H2.
 */
public class RepositorioContactoImpl implements IRepositorioContacto {

    private final GestorConexionH2 gestorConexion;

    public RepositorioContactoImpl() {
        this.gestorConexion = GestorConexionH2.getInstancia();
    }

    @Override
    public void guardar(Contacto contacto) {
        String sql = "INSERT INTO contactos (id_contacto, nombre, estado) VALUES (?, ?, ?)";

        try (Connection conn = gestorConexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, contacto.getIdContacto());
            stmt.setString(2, contacto.getNombre());
            stmt.setBoolean(3, contacto.isEstado());

            stmt.executeUpdate();
            System.out.println("✅ [RepositorioContacto]: Contacto guardado - ID: " + contacto.getIdContacto());

        } catch (SQLException e) {
            System.err.println("❌ [RepositorioContacto]: Error al guardar: " + e.getMessage());
            throw new RuntimeException("Error al guardar contacto", e);
        }
    }

    @Override
    public Contacto obtenerPorId(UUID idContacto) {
        String sql = "SELECT * FROM contactos WHERE id_contacto = ?";

        try (Connection conn = gestorConexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, idContacto);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapearResultSet(rs);
            }
            return null;

        } catch (SQLException e) {
            System.err.println("❌ [RepositorioContacto]: Error al obtener: " + e.getMessage());
            throw new RuntimeException("Error al consultar contacto", e);
        }
    }

    @Override
    public void actualizar(Contacto contacto) {
        String sql = "UPDATE contactos SET nombre = ?, estado = ? WHERE id_contacto = ?";

        try (Connection conn = gestorConexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, contacto.getNombre());
            stmt.setBoolean(2, contacto.isEstado());
            stmt.setObject(3, contacto.getIdContacto());

            stmt.executeUpdate();
            System.out.println("✅ [RepositorioContacto]: Contacto actualizado");

        } catch (SQLException e) {
            System.err.println("❌ [RepositorioContacto]: Error al actualizar: " + e.getMessage());
            throw new RuntimeException("Error al actualizar contacto", e);
        }
    }

    @Override
    public void eliminar(UUID idContacto) {
        String sql = "DELETE FROM contactos WHERE id_contacto = ?";

        try (Connection conn = gestorConexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, idContacto);
            stmt.executeUpdate();
            System.out.println("✅ [RepositorioContacto]: Contacto eliminado");

        } catch (SQLException e) {
            System.err.println("❌ [RepositorioContacto]: Error al eliminar: " + e.getMessage());
            throw new RuntimeException("Error al eliminar contacto", e);
        }
    }

    @Override
    public List<Contacto> obtenerTodos() {
        String sql = "SELECT * FROM contactos ORDER BY nombre";
        List<Contacto> contactos = new ArrayList<>();

        try (Connection conn = gestorConexion.getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                contactos.add(mapearResultSet(rs));
            }

            System.out.println("✅ [RepositorioContacto]: Obtenidos " + contactos.size() + " contactos");
            return contactos;

        } catch (SQLException e) {
            System.err.println("❌ [RepositorioContacto]: Error al obtener todos: " + e.getMessage());
            throw new RuntimeException("Error al consultar contactos", e);
        }
    }

    @Override
    public List<Contacto> obtenerActivos() {
        String sql = "SELECT * FROM contactos WHERE estado = TRUE ORDER BY nombre";
        List<Contacto> contactos = new ArrayList<>();

        try (Connection conn = gestorConexion.getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                contactos.add(mapearResultSet(rs));
            }

            return contactos;

        } catch (SQLException e) {
            System.err.println("❌ [RepositorioContacto]: Error al obtener activos: " + e.getMessage());
            throw new RuntimeException("Error al consultar contactos activos", e);
        }
    }

    private Contacto mapearResultSet(ResultSet rs) throws SQLException {
        Contacto contacto = new Contacto();
        contacto.setIdContacto((UUID) rs.getObject("id_contacto"));
        contacto.setNombre(rs.getString("nombre"));
        contacto.setEstado(rs.getBoolean("estado"));
        return contacto;
    }
}
