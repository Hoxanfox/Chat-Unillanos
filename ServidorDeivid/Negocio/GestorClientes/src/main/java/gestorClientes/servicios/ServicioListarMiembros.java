package gestorClientes.servicios;

import com.google.gson.Gson;
import conexion.clientes.interfaces.IGestorConexionesCliente;
import conexion.clientes.interfaces.IRouterMensajesCliente;
import dominio.clienteServidor.Usuario;
import dominio.clienteServidor.relaciones.CanalMiembro;
import dto.canales.DTOListarMiembros;
import dto.canales.DTOMiembroCanal;
import dto.comunicacion.DTOResponse;
import gestorClientes.interfaces.IServicioCliente;
import logger.LoggerCentral;
import repositorio.clienteServidor.CanalMiembroRepositorio;
import repositorio.clienteServidor.UsuarioRepositorio;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Servicio del servidor para listar los miembros de un canal.
 * Simplemente consulta la BD y retorna la lista de miembros con su información.
 */
public class ServicioListarMiembros implements IServicioCliente {

    private static final String TAG = "ListarMiembros";

    // Colores ANSI para logs
    private static final String RESET = "\u001B[0m";
    private static final String VERDE = "\u001B[32m";
    private static final String AMARILLO = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";
    private static final String ROJO = "\u001B[31m";
    private static final String AZUL = "\u001B[34m";

    private final CanalMiembroRepositorio miembroRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final Gson gson;

    public ServicioListarMiembros() {
        this.miembroRepositorio = new CanalMiembroRepositorio();
        this.usuarioRepositorio = new UsuarioRepositorio();
        this.gson = new Gson();
        LoggerCentral.info(TAG, AZUL + "Constructor: ServicioListarMiembros creado" + RESET);
    }

    @Override
    public String getNombre() {
        return "ServicioListarMiembros";
    }

    @Override
    public void inicializar(IGestorConexionesCliente gestor, IRouterMensajesCliente router) {
        LoggerCentral.info(TAG, AZUL + "Inicializando ServicioListarMiembros..." + RESET);

        // ==================== RUTA: Listar Miembros del Canal ====================
        router.registrarAccion("listarMiembros", (datos, idSesion) -> {
            try {
                LoggerCentral.info(TAG, CYAN + "📥 Recibida petición de listar miembros" + RESET);

                // Validar autenticación
                String userId = gestor.obtenerUsuarioDeSesion(idSesion);
                if (userId == null) {
                    LoggerCentral.warn(TAG, AMARILLO + "Usuario no autenticado" + RESET);
                    return new DTOResponse("listarMiembros", "error", "Usuario no autenticado", null);
                }

                // Parsear datos
                DTOListarMiembros dto = gson.fromJson(datos, DTOListarMiembros.class);

                // Validar datos
                if (dto.getCanalId() == null || dto.getCanalId().isEmpty()) {
                    LoggerCentral.warn(TAG, AMARILLO + "CanalId no proporcionado" + RESET);
                    return new DTOResponse("listarMiembros", "error", "CanalId es requerido", null);
                }

                LoggerCentral.info(TAG, CYAN + "📋 Listando miembros del canal: " + dto.getCanalId() + RESET);
                LoggerCentral.info(TAG, "   → Solicitante: " + dto.getSolicitanteId());

                UUID canalIdUUID;
                try {
                    canalIdUUID = UUID.fromString(dto.getCanalId());
                } catch (IllegalArgumentException e) {
                    LoggerCentral.warn(TAG, ROJO + "CanalId inválido: " + dto.getCanalId() + RESET);
                    return new DTOResponse("listarMiembros", "error", "CanalId inválido", null);
                }

                // Obtener todos los miembros del canal
                List<CanalMiembro> relaciones = miembroRepositorio.obtenerTodosParaSync();
                List<DTOMiembroCanal> miembros = new ArrayList<>();

                LoggerCentral.info(TAG, "   → Total relaciones en BD: " + relaciones.size());

                // Filtrar por canal y obtener información de cada usuario
                for (CanalMiembro relacion : relaciones) {
                    if (relacion.getCanalId().equals(canalIdUUID)) {
                        Usuario usuario = usuarioRepositorio.buscarPorId(relacion.getUsuarioId());

                        if (usuario != null) {
                            DTOMiembroCanal miembroDTO = new DTOMiembroCanal(
                                usuario.getId(),
                                usuario.getNombre(),
                                usuario.getEmail(),
                                usuario.getFoto(),
                                usuario.getEstado() != null ? usuario.getEstado().name() : "OFFLINE"
                            );
                            miembros.add(miembroDTO);
                            LoggerCentral.info(TAG, "   ✅ Miembro: " + usuario.getNombre() + " (" + usuario.getId() + ")");
                        } else {
                            LoggerCentral.warn(TAG, AMARILLO + "   ⚠️ Usuario no encontrado: " + relacion.getUsuarioId() + RESET);
                        }
                    }
                }

                LoggerCentral.info(TAG, VERDE + "✅ Devolviendo " + miembros.size() + " miembros del canal" + RESET);

                return new DTOResponse("listarMiembros", "success", "Miembros obtenidos", gson.toJsonTree(miembros));

            } catch (Exception e) {
                LoggerCentral.error(TAG, ROJO + "❌ Error en listarMiembros: " + e.getMessage() + RESET);
                e.printStackTrace();
                return new DTOResponse("listarMiembros", "error", "Error interno del servidor", null);
            }
        });

        LoggerCentral.info(TAG, VERDE + "✅ ServicioListarMiembros inicializado" + RESET);
        LoggerCentral.info(TAG, "   → Rutas registradas: listarMiembros");
    }

    @Override
    public void iniciar() {
        LoggerCentral.info(TAG, VERDE + "Servicio de listar miembros iniciado" + RESET);
    }

    @Override
    public void detener() {
        LoggerCentral.info(TAG, AMARILLO + "Servicio de listar miembros detenido" + RESET);
    }
}
