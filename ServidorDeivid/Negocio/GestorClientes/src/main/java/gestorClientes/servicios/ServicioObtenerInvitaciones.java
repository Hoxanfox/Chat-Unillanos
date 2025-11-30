package gestorClientes.servicios;

import com.google.gson.Gson;
import conexion.clientes.interfaces.IGestorConexionesCliente;
import conexion.clientes.interfaces.IRouterMensajesCliente;
import dominio.clienteServidor.Canal;
import dominio.clienteServidor.Usuario;
import dominio.clienteServidor.relaciones.CanalInvitacion;
import dto.comunicacion.DTOResponse;
import gestorClientes.interfaces.IServicioCliente;
import logger.LoggerCentral;
import repositorio.clienteServidor.CanalInvitacionRepositorio;
import repositorio.clienteServidor.CanalRepositorio;
import repositorio.clienteServidor.UsuarioRepositorio;

import java.util.*;

/**
 * Servicio para obtener las invitaciones pendientes de un usuario.
 * Maneja la acción "obtenerInvitaciones" que devuelve la lista de invitaciones a canales.
 */
public class ServicioObtenerInvitaciones implements IServicioCliente {

    private static final String TAG = "ServicioObtenerInvitaciones";

    // Colores ANSI para logs
    private static final String RESET = "\u001B[0m";
    private static final String ROJO = "\u001B[31m";

    private IGestorConexionesCliente gestor;
    private final CanalInvitacionRepositorio invitacionRepositorio;
    private final CanalRepositorio canalRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final Gson gson;

    public ServicioObtenerInvitaciones() {
        this.invitacionRepositorio = new CanalInvitacionRepositorio();
        this.canalRepositorio = new CanalRepositorio();
        this.usuarioRepositorio = new UsuarioRepositorio();
        this.gson = new Gson();
        LoggerCentral.info(TAG, ROJO + "Constructor: ServicioObtenerInvitaciones creado" + RESET);
    }

    @Override
    public String getNombre() {
        return "ServicioObtenerInvitaciones";
    }

    @Override
    public void inicializar(IGestorConexionesCliente gestor, IRouterMensajesCliente router) {
        this.gestor = gestor;
        LoggerCentral.info(TAG, ROJO + "Inicializando ServicioObtenerInvitaciones..." + RESET);

        // ==================== RUTA: Obtener Invitaciones ====================
        router.registrarAccion("obtenerInvitaciones", (datos, idSesion) -> {
            try {
                LoggerCentral.info(TAG, ROJO + "📥 Recibida petición de obtener invitaciones" + RESET);

                // 1. Validar autenticación
                String usuarioIdSesion = gestor.obtenerUsuarioDeSesion(idSesion);
                if (usuarioIdSesion == null) {
                    LoggerCentral.warn(TAG, ROJO + "Usuario no autenticado" + RESET);
                    return new DTOResponse("obtenerInvitaciones", "error", "Usuario no autenticado", null);
                }

                // 2. Parsear datos para obtener el usuarioId del payload
                Map<String, Object> payload = gson.fromJson(datos, Map.class);
                String usuarioId = payload.get("usuarioId") != null ? payload.get("usuarioId").toString() : null;

                if (usuarioId == null || usuarioId.trim().isEmpty()) {
                    LoggerCentral.warn(TAG, ROJO + "ID de usuario inválido" + RESET);
                    return new DTOResponse("obtenerInvitaciones", "error", "ID de usuario requerido", null);
                }

                // 3. Verificar que el usuario del payload coincide con el de la sesión
                if (!usuarioId.equals(usuarioIdSesion)) {
                    LoggerCentral.warn(TAG, ROJO + "Usuario no autorizado" + RESET);
                    return new DTOResponse("obtenerInvitaciones", "error", "No autorizado", null);
                }

                UUID usuarioUUID = UUID.fromString(usuarioId);

                LoggerCentral.info(TAG, ROJO + "📤 Obteniendo invitaciones para usuario: " + usuarioId + RESET);

                // 4. Obtener invitaciones pendientes del usuario
                List<CanalInvitacion> invitaciones = invitacionRepositorio.obtenerInvitacionesPendientesPorUsuario(usuarioUUID);

                LoggerCentral.info(TAG, ROJO + "✅ Encontradas " + invitaciones.size() + " invitaciones pendientes" + RESET);

                // 5. Construir la respuesta con información detallada de cada invitación
                List<Map<String, Object>> invitacionesDTO = new ArrayList<>();

                LoggerCentral.info(TAG, ROJO + "🔄 Procesando " + invitaciones.size() + " invitaciones..." + RESET);

                for (CanalInvitacion invitacion : invitaciones) {
                    LoggerCentral.info(TAG, ROJO + "  → Procesando invitación: " + invitacion.getId() + RESET);

                    // Obtener información del canal
                    Canal canal = canalRepositorio.obtenerPorId(invitacion.getCanalId());
                    if (canal == null) {
                        LoggerCentral.warn(TAG, ROJO + "  ⚠️ Canal no encontrado: " + invitacion.getCanalId() + RESET);
                        continue;
                    }
                    LoggerCentral.info(TAG, ROJO + "  ✅ Canal encontrado: " + canal.getNombre() + RESET);

                    // Obtener información del invitador
                    Usuario invitador = usuarioRepositorio.buscarPorId(invitacion.getInvitadorId().toString());
                    if (invitador == null) {
                        LoggerCentral.warn(TAG, ROJO + "  ⚠️ Invitador no encontrado: " + invitacion.getInvitadorId() + RESET);
                        continue;
                    }
                    LoggerCentral.info(TAG, ROJO + "  ✅ Invitador encontrado: " + invitador.getNombre() + RESET);

                    // Construir DTO de la invitación
                    Map<String, Object> invitacionDTO = new HashMap<>();
                    invitacionDTO.put("invitacionId", invitacion.getId().toString());
                    invitacionDTO.put("channelId", canal.getId().toString());
                    invitacionDTO.put("channelName", canal.getNombre());
                    invitacionDTO.put("estado", invitacion.getEstado());
                    invitacionDTO.put("fechaCreacion", invitacion.getFechaCreacion().toString());

                    // Información del invitador
                    Map<String, String> invitadorDTO = new HashMap<>();
                    invitadorDTO.put("userId", invitador.getId());
                    invitadorDTO.put("username", invitador.getNombre());
                    invitadorDTO.put("userPhoto", invitador.getFoto() != null ? invitador.getFoto() : "");

                    invitacionDTO.put("invitador", invitadorDTO);

                    invitacionesDTO.add(invitacionDTO);
                }

                LoggerCentral.info(TAG, ROJO + "✅ Devolviendo " + invitacionesDTO.size() + " invitaciones al cliente" + RESET);

                // 6. Preparar respuesta
                Map<String, Object> respuesta = new HashMap<>();
                respuesta.put("invitaciones", invitacionesDTO);
                respuesta.put("total", invitacionesDTO.size());

                return new DTOResponse("obtenerInvitaciones", "success", "Invitaciones obtenidas", gson.toJsonTree(respuesta));

            } catch (IllegalArgumentException e) {
                LoggerCentral.error(TAG, ROJO + "❌ Error: UUID inválido - " + e.getMessage() + RESET);
                return new DTOResponse("obtenerInvitaciones", "error", "ID inválido: " + e.getMessage(), null);
            } catch (Exception e) {
                LoggerCentral.error(TAG, ROJO + "❌ Error en obtenerInvitaciones: " + e.getMessage() + RESET);
                e.printStackTrace();
                return new DTOResponse("obtenerInvitaciones", "error", "Error interno del servidor: " + e.getMessage(), null);
            }
        });

        LoggerCentral.info(TAG, ROJO + "✅ Servicio inicializado - Ruta 'obtenerInvitaciones' registrada" + RESET);
    }

    @Override
    public void iniciar() {
        LoggerCentral.info(TAG, ROJO + "Servicio de obtener invitaciones iniciado" + RESET);
    }

    @Override
    public void detener() {
        LoggerCentral.info(TAG, ROJO + "Servicio de obtener invitaciones detenido" + RESET);
    }
}
