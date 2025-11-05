package repositorio.canal;

import dominio.Canal;
import dto.canales.DTOMiembroCanal;
import repositorio.conexion.GestorConexionH2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RepositorioCanalImpl implements IRepositorioCanal {

    private final GestorConexionH2 gestorConexion;

    public RepositorioCanalImpl() {
        this.gestorConexion = GestorConexionH2.getInstancia();
    }

    @Override
    public CompletableFuture<Boolean> guardar(Canal canal) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT INTO canales (id_canal, nombre, id_administrador) VALUES (?, ?, ?)";
            try (Connection conn = gestorConexion.getConexion();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, canal.getIdCanal().toString());
                pstmt.setString(2, canal.getNombre());
                pstmt.setString(3, canal.getIdAdministrador().toString());

                int filasAfectadas = pstmt.executeUpdate();
                return filasAfectadas > 0;
            } catch (SQLException e) {
                System.err.println("Error al guardar el canal: " + e.getMessage());
                throw new RuntimeException("Fallo en la operación de base de datos al guardar", e);
            }
        });
    }

    @Override
    public CompletableFuture<Canal> buscarPorId(String id) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT id_canal, nombre, id_administrador FROM canales WHERE id_canal = ?";
            try (Connection conn = gestorConexion.getConexion();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, id);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    Canal canal = new Canal(
                            UUID.fromString(rs.getString("id_canal")),
                            rs.getString("nombre"),
                            UUID.fromString(rs.getString("id_administrador"))
                    );

                    // Cargar miembros desde la tabla de enlace canal_usuario
                    List<UUID> miembros = new ArrayList<>();
                    String sqlMiembros = "SELECT id_usuario FROM canal_usuario WHERE id_canal = ?";
                    try (PreparedStatement pstmt2 = conn.prepareStatement(sqlMiembros)) {
                        pstmt2.setString(1, id);
                        ResultSet rs2 = pstmt2.executeQuery();
                        while (rs2.next()) {
                            String idUsuario = rs2.getString("id_usuario");
                            if (idUsuario != null) {
                                miembros.add(UUID.fromString(idUsuario));
                            }
                        }
                    } catch (SQLException e) {
                        System.err.println("Error cargando miembros del canal: " + e.getMessage());
                    }

                    canal.setMiembros(miembros);
                    return canal;
                }
                return null;
            } catch (SQLException e) {
                System.err.println("Error al buscar el canal por ID: " + e.getMessage());
                throw new RuntimeException("Fallo en la operación de base de datos al buscar por ID", e);
            }
        });
    }

    @Override
    public CompletableFuture<List<Canal>> obtenerTodos() {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT id_canal, nombre, id_administrador FROM canales";
            List<Canal> canales = new ArrayList<>();
            try (Connection conn = gestorConexion.getConexion();
                 PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {
                    Canal canal = new Canal(
                            UUID.fromString(rs.getString("id_canal")),
                            rs.getString("nombre"),
                            UUID.fromString(rs.getString("id_administrador"))
                    );

                    // Cargar miembros para cada canal
                    List<UUID> miembros = new ArrayList<>();
                    String sqlMiembros = "SELECT id_usuario FROM canal_usuario WHERE id_canal = ?";
                    try (PreparedStatement pstmt2 = conn.prepareStatement(sqlMiembros)) {
                        pstmt2.setString(1, canal.getIdCanal().toString());
                        ResultSet rs2 = pstmt2.executeQuery();
                        while (rs2.next()) {
                            String idUsuario = rs2.getString("id_usuario");
                            if (idUsuario != null) {
                                miembros.add(UUID.fromString(idUsuario));
                            }
                        }
                    } catch (SQLException e) {
                        System.err.println("Error cargando miembros del canal: " + e.getMessage());
                    }

                    canal.setMiembros(miembros);
                    canales.add(canal);
                }
                return canales;
            } catch (SQLException e) {
                System.err.println("Error al obtener todos los canales: " + e.getMessage());
                throw new RuntimeException("Fallo en la operación de base de datos al obtener todos", e);
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> actualizar(Canal canal) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE canales SET nombre = ?, id_administrador = ? WHERE id_canal = ?";
            try (Connection conn = gestorConexion.getConexion();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, canal.getNombre());
                pstmt.setString(2, canal.getIdAdministrador().toString());
                pstmt.setString(3, canal.getIdCanal().toString());

                int filasAfectadas = pstmt.executeUpdate();
                return filasAfectadas > 0;
            } catch (SQLException e) {
                System.err.println("Error al actualizar el canal: " + e.getMessage());
                throw new RuntimeException("Fallo en la operación de base de datos al actualizar", e);
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> eliminar(String id) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM canales WHERE id_canal = ?";
            try (Connection conn = gestorConexion.getConexion();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, id);

                int filasAfectadas = pstmt.executeUpdate();
                return filasAfectadas > 0;
            } catch (SQLException e) {
                System.err.println("Error al eliminar el canal: " + e.getMessage());
                throw new RuntimeException("Fallo en la operación de base de datos al eliminar", e);
            }
        });
    }

    /**
     * Agrega un miembro a un canal específico.
     *
     * @param canalId El UUID del canal.
     * @param usuarioId El UUID del usuario a agregar.
     * @return Un CompletableFuture que se completará con `true` si el miembro fue agregado exitosamente,
     *         `false` si ya existía o si la operación falló.
     */
    @Override
    public CompletableFuture<Boolean> agregarMiembroACanal(String canalId, String usuarioId) {
        return CompletableFuture.supplyAsync(() -> {
            String sqlCheck = "SELECT COUNT(1) AS cnt FROM canal_usuario WHERE id_canal = ? AND id_usuario = ?";
            String sqlInsert = "INSERT INTO canal_usuario (id_canal, id_usuario) VALUES (?, ?)";
            try (Connection conn = gestorConexion.getConexion()) {
                // Verificar existencia
                try (PreparedStatement pstmt = conn.prepareStatement(sqlCheck)) {
                    pstmt.setString(1, canalId);
                    pstmt.setString(2, usuarioId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next() && rs.getInt("cnt") > 0) {
                            return false; // ya existe
                        }
                    }
                }

                // Insertar nuevo miembro
                try (PreparedStatement pstmt2 = conn.prepareStatement(sqlInsert)) {
                    pstmt2.setString(1, canalId);
                    pstmt2.setString(2, usuarioId);
                    int filas = pstmt2.executeUpdate();
                    return filas > 0;
                }

            } catch (SQLException e) {
                System.err.println("Error agregando miembro al canal: " + e.getMessage());
                throw new RuntimeException("Fallo en la operación de base de datos al agregar miembro", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> sincronizarCanales(List<Canal> canalesDelServidor) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = gestorConexion.getConexion()) {
                conn.setAutoCommit(false); // Iniciar transacción

                try {
                    for (Canal canal : canalesDelServidor) {
                        // Verificar si el canal ya existe
                        String sqlCheck = "SELECT COUNT(1) AS cnt FROM canales WHERE id_canal = ?";
                        boolean existe = false;

                        try (PreparedStatement pstmt = conn.prepareStatement(sqlCheck)) {
                            pstmt.setString(1, canal.getIdCanal().toString());
                            try (ResultSet rs = pstmt.executeQuery()) {
                                if (rs.next() && rs.getInt("cnt") > 0) {
                                    existe = true;
                                }
                            }
                        }

                        if (existe) {
                            // Actualizar canal existente
                            String sqlUpdate = "UPDATE canales SET nombre = ?, id_administrador = ? WHERE id_canal = ?";
                            try (PreparedStatement pstmt = conn.prepareStatement(sqlUpdate)) {
                                pstmt.setString(1, canal.getNombre());
                                pstmt.setString(2, canal.getIdAdministrador() != null ? canal.getIdAdministrador().toString() : null);
                                pstmt.setString(3, canal.getIdCanal().toString());
                                pstmt.executeUpdate();
                            }
                        } else {
                            // Insertar nuevo canal
                            String sqlInsert = "INSERT INTO canales (id_canal, nombre, id_administrador) VALUES (?, ?, ?)";
                            try (PreparedStatement pstmt = conn.prepareStatement(sqlInsert)) {
                                pstmt.setString(1, canal.getIdCanal().toString());
                                pstmt.setString(2, canal.getNombre());
                                pstmt.setString(3, canal.getIdAdministrador() != null ? canal.getIdAdministrador().toString() : null);
                                pstmt.executeUpdate();
                            }
                        }

                        // Sincronizar miembros del canal si hay información disponible
                        if (canal.getMiembros() != null && !canal.getMiembros().isEmpty()) {
                            // Eliminar miembros antiguos
                            String sqlDeleteMiembros = "DELETE FROM canal_usuario WHERE id_canal = ?";
                            try (PreparedStatement pstmt = conn.prepareStatement(sqlDeleteMiembros)) {
                                pstmt.setString(1, canal.getIdCanal().toString());
                                pstmt.executeUpdate();
                            }

                            // Insertar miembros actualizados
                            String sqlInsertMiembro = "INSERT INTO canal_usuario (id_canal, id_usuario) VALUES (?, ?)";
                            try (PreparedStatement pstmt = conn.prepareStatement(sqlInsertMiembro)) {
                                for (UUID miembroId : canal.getMiembros()) {
                                    pstmt.setString(1, canal.getIdCanal().toString());
                                    pstmt.setString(2, miembroId.toString());
                                    pstmt.addBatch();
                                }
                                pstmt.executeBatch();
                            }
                        }
                    }

                    conn.commit(); // Confirmar transacción
                    System.out.println("Sincronización de canales completada exitosamente.");

                } catch (SQLException e) {
                    conn.rollback(); // Revertir transacción en caso de error
                    System.err.println("Error durante la sincronización, transacción revertida: " + e.getMessage());
                    throw new RuntimeException("Fallo al sincronizar canales", e);
                } finally {
                    conn.setAutoCommit(true); // Restaurar auto-commit
                }

            } catch (SQLException e) {
                System.err.println("Error al conectar con la base de datos: " + e.getMessage());
                throw new RuntimeException("Fallo en la conexión durante la sincronización", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> sincronizarMiembros(String canalId, List<DTOMiembroCanal> miembros) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = gestorConexion.getConexion()) {
                conn.setAutoCommit(false); // Iniciar transacción

                try {
                    // Eliminar miembros antiguos del canal
                    String sqlDelete = "DELETE FROM canal_usuario WHERE id_canal = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(sqlDelete)) {
                        pstmt.setString(1, canalId);
                        pstmt.executeUpdate();
                    }

                    // Insertar los miembros actualizados
                    if (miembros != null && !miembros.isEmpty()) {
                        String sqlInsert = "INSERT INTO canal_usuario (id_canal, id_usuario, rol, fecha_union) VALUES (?, ?, ?, ?)";
                        try (PreparedStatement pstmt = conn.prepareStatement(sqlInsert)) {
                            for (DTOMiembroCanal miembro : miembros) {
                                pstmt.setString(1, canalId);
                                pstmt.setString(2, miembro.getUsuarioId());
                                pstmt.setString(3, miembro.getRol() != null ? miembro.getRol() : "miembro");

                                // Manejar fecha de unión
                                if (miembro.getFechaUnion() != null) {
                                    try {
                                        pstmt.setString(4, miembro.getFechaUnion());
                                    } catch (Exception e) {
                                        pstmt.setString(4, null);
                                    }
                                } else {
                                    pstmt.setString(4, null);
                                }

                                pstmt.addBatch();
                            }
                            pstmt.executeBatch();
                        }
                    }

                    conn.commit(); // Confirmar transacción
                    System.out.println("Sincronización de miembros del canal " + canalId + " completada exitosamente.");

                } catch (SQLException e) {
                    conn.rollback(); // Revertir transacción en caso de error
                    System.err.println("Error durante la sincronización de miembros, transacción revertida: " + e.getMessage());
                    throw new RuntimeException("Fallo al sincronizar miembros del canal", e);
                } finally {
                    conn.setAutoCommit(true); // Restaurar auto-commit
                }

            } catch (SQLException e) {
                System.err.println("Error al conectar con la base de datos: " + e.getMessage());
                throw new RuntimeException("Fallo en la conexión durante la sincronización de miembros", e);
            }
        });
    }
}
