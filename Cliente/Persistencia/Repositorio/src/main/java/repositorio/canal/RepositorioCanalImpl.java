package repositorio.canal;

import dominio.Canal;
import repositorio.conexion.GestorConexionH2;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implementación del repositorio de canales con H2.
 * Operaciones CRUD completas.
 */
public class RepositorioCanalImpl implements IRepositorioCanal {

    private final GestorConexionH2 gestorConexion;

    public RepositorioCanalImpl() {
        this.gestorConexion = GestorConexionH2.getInstancia();
    }

    @Override
    public void guardar(Canal canal) {
        String sql = """
            INSERT INTO canales (id_canal, nombre, id_administrador)
            VALUES (?, ?, ?)
        """;

        try (Connection conn = gestorConexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, canal.getIdCanal());
            stmt.setString(2, canal.getNombre());
            stmt.setObject(3, canal.getIdAdministrador());

            stmt.executeUpdate();
            System.out.println("✅ [RepositorioCanal]: Canal guardado - ID: " + canal.getIdCanal());

        } catch (SQLException e) {
            System.err.println("❌ [RepositorioCanal]: Error al guardar canal: " + e.getMessage());
            throw new RuntimeException("Error al guardar canal", e);
        }
    }

    @Override
    public Canal obtenerPorId(UUID idCanal) {
        String sql = "SELECT * FROM canales WHERE id_canal = ?";

        try (Connection conn = gestorConexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, idCanal);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapearResultSet(rs);
            }

            return null;

        } catch (SQLException e) {
            System.err.println("❌ [RepositorioCanal]: Error al obtener canal: " + e.getMessage());
            throw new RuntimeException("Error al consultar canal", e);
        }
    }

    @Override
    public Canal obtenerPorNombre(String nombre) {
        String sql = "SELECT * FROM canales WHERE nombre = ?";

        try (Connection conn = gestorConexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nombre);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapearResultSet(rs);
            }

            return null;

        } catch (SQLException e) {
            System.err.println("❌ [RepositorioCanal]: Error al obtener canal: " + e.getMessage());
            throw new RuntimeException("Error al consultar canal", e);
        }
    }

    @Override
    public void actualizar(Canal canal) {
        String sql = """
            UPDATE canales 
            SET nombre = ?, id_administrador = ?
            WHERE id_canal = ?
        """;

        try (Connection conn = gestorConexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, canal.getNombre());
            stmt.setObject(2, canal.getIdAdministrador());
            stmt.setObject(3, canal.getIdCanal());

            int filasActualizadas = stmt.executeUpdate();
            System.out.println("✅ [RepositorioCanal]: Canal actualizado - Filas: " + filasActualizadas);

        } catch (SQLException e) {
            System.err.println("❌ [RepositorioCanal]: Error al actualizar canal: " + e.getMessage());
            throw new RuntimeException("Error al actualizar canal", e);
        }
    }

    @Override
    public void eliminar(UUID idCanal) {
        String sql = "DELETE FROM canales WHERE id_canal = ?";

        try (Connection conn = gestorConexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, idCanal);
            int filasEliminadas = stmt.executeUpdate();
            
            System.out.println("✅ [RepositorioCanal]: Canal eliminado - Filas: " + filasEliminadas);

        } catch (SQLException e) {
            System.err.println("❌ [RepositorioCanal]: Error al eliminar canal: " + e.getMessage());
            throw new RuntimeException("Error al eliminar canal", e);
        }
    }

    @Override
    public List<Canal> obtenerTodos() {
        String sql = "SELECT * FROM canales ORDER BY nombre";
        List<Canal> canales = new ArrayList<>();

        try (Connection conn = gestorConexion.getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                canales.add(mapearResultSet(rs));
            }

            System.out.println("✅ [RepositorioCanal]: Obtenidos " + canales.size() + " canales");
            return canales;

        } catch (SQLException e) {
            System.err.println("❌ [RepositorioCanal]: Error al obtener canales: " + e.getMessage());
            throw new RuntimeException("Error al consultar canales", e);
        }
    }

    @Override
    public List<Canal> obtenerPorAdministrador(UUID idAdministrador) {
        String sql = "SELECT * FROM canales WHERE id_administrador = ? ORDER BY nombre";
        List<Canal> canales = new ArrayList<>();

        try (Connection conn = gestorConexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, idAdministrador);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                canales.add(mapearResultSet(rs));
            }

            System.out.println("✅ [RepositorioCanal]: Obtenidos " + canales.size() + " canales del administrador");
            return canales;

        } catch (SQLException e) {
            System.err.println("❌ [RepositorioCanal]: Error al obtener canales: " + e.getMessage());
            throw new RuntimeException("Error al consultar canales", e);
        }
    }

    /**
     * Mapea un ResultSet a una entidad Canal.
     */
    private Canal mapearResultSet(ResultSet rs) throws SQLException {
        Canal canal = new Canal();
        canal.setIdCanal((UUID) rs.getObject("id_canal"));
        canal.setNombre(rs.getString("nombre"));
        canal.setIdAdministrador((UUID) rs.getObject("id_administrador"));
        return canal;
    }
}
