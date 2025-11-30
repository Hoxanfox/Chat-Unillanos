package gestorClientes.servicios;

import com.google.gson.Gson;
import conexion.clientes.interfaces.IGestorConexionesCliente;
import conexion.clientes.interfaces.IRouterMensajesCliente;
import dominio.clienteServidor.relaciones.CanalInvitacion;
import dto.canales.DTORechazarInvitacion;
import dto.comunicacion.DTOResponse;
import gestorClientes.interfaces.IServicioCliente;
import gestorP2P.servicios.ServicioNotificacionCambios;
import gestorP2P.servicios.ServicioSincronizacionDatos;
import logger.LoggerCentral;
import repositorio.clienteServidor.CanalInvitacionRepositorio;
import repositorio.clienteServidor.CanalRepositorio;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio para gestionar el rechazo de invitaciones a canales.
 * Maneja la acción "rechazarInvitacion" que permite a un usuario rechazar una invitación a un canal.
 * Actualiza el estado en BD y sincroniza con la red P2P.
 */
public class ServicioRechazarInvitacion implements IServicioCliente {

    private static final String TAG = "ServicioRechazarInvitacion";

    // Colores ANSI para logs
    private static final String RESET = "\u001B[0m";
    private static final String ROJO = "\u001B[31m";

    private IGestorConexionesCliente gestor;
    private final CanalInvitacionRepositorio invitacionRepositorio;
    private final CanalRepositorio canalRepositorio;
    private final Gson gson;

    // Referencias a servicios
    private ServicioNotificacionCliente servicioNotificacion;
    private ServicioNotificacionCambios servicioNotificacionCambios;
    private ServicioSincronizacionDatos servicioSyncP2P; // ✅ NUEVO

    public ServicioRechazarInvitacion() {
        this.invitacionRepositorio = new CanalInvitacionRepositorio();
        this.canalRepositorio = new CanalRepositorio();
        this.gson = new Gson();
        LoggerCentral.info(TAG, ROJO + "Constructor: ServicioRechazarInvitacion creado" + RESET);
    }

    /**
     * Inyecta el servicio de notificaciones CS.
     */
    public void setServicioNotificacion(ServicioNotificacionCliente servicioNotificacion) {
        this.servicioNotificacion = servicioNotificacion;
        LoggerCentral.info(TAG, ROJO + "Servicio de notificaciones CS configurado" + RESET);
    }

    /** ✅ NUEVO: Inyecta el servicio de sincronización P2P. */
    public void setServicioSincronizacionP2P(ServicioSincronizacionDatos servicioSyncP2P) {
        this.servicioSyncP2P = servicioSyncP2P;
        LoggerCentral.info(TAG, ROJO + "Servicio de sincronización P2P configurado" + RESET);
    }

    /**
     * ✅ NUEVO: Inyecta el notificador de cambios central.
     */
    public void setServicioNotificacionCambios(ServicioNotificacionCambios servicio) {
        this.servicioNotificacionCambios = servicio;
        LoggerCentral.info(TAG, ROJO + "Servicio de notificación de cambios configurado" + RESET);
    }

    @Override
    public String getNombre() {
        return "ServicioRechazarInvitacion";
    }

    @Override
    public void inicializar(IGestorConexionesCliente gestor, IRouterMensajesCliente router) {
        this.gestor = gestor;
        LoggerCentral.info(TAG, ROJO + "Inicializando ServicioRechazarInvitacion..." + RESET);

        // ==================== RUTA: Rechazar Invitación ====================
        router.registrarAccion("rechazarInvitacion", (datos, idSesion) -> {
            try {
                LoggerCentral.info(TAG, ROJO + "📥 Recibida petición de rechazar invitación" + RESET);

                // 1. Validar autenticación
                String usuarioIdSesion = gestor.obtenerUsuarioDeSesion(idSesion);
                if (usuarioIdSesion == null) {
                    LoggerCentral.warn(TAG, ROJO + "Usuario no autenticado" + RESET);
                    return new DTOResponse("rechazarInvitacion", "error", "Usuario no autenticado", null);
                }

                // 2. Parsear datos
                DTORechazarInvitacion dto = gson.fromJson(datos, DTORechazarInvitacion.class);

                // 3. Validar datos
                if (dto.getCanalId() == null || dto.getCanalId().trim().isEmpty()) {
                    LoggerCentral.warn(TAG, ROJO + "ID de canal inválido" + RESET);
                    return new DTOResponse("rechazarInvitacion", "error", "ID de canal requerido", null);
                }

                if (dto.getUsuarioId() == null || dto.getUsuarioId().trim().isEmpty()) {
                    LoggerCentral.warn(TAG, ROJO + "ID de usuario inválido" + RESET);
                    return new DTOResponse("rechazarInvitacion", "error", "ID de usuario requerido", null);
                }

                UUID canalId = UUID.fromString(dto.getCanalId());
                UUID usuarioId = UUID.fromString(dto.getUsuarioId());

                // 4. Verificar que el usuario del DTO coincide con el de la sesión
                if (!usuarioId.toString().equals(usuarioIdSesion)) {
                    LoggerCentral.warn(TAG, ROJO + "Usuario no coincide con la sesión" + RESET);
                    return new DTOResponse("rechazarInvitacion", "error", "No autorizado", null);
                }

                LoggerCentral.info(TAG, ROJO + "📤 Procesando rechazo de invitación" + RESET);
                LoggerCentral.info(TAG, ROJO + "   → Canal: " + canalId + RESET);
                LoggerCentral.info(TAG, ROJO + "   → Usuario: " + usuarioId + RESET);

                // 5. Verificar que existe una invitación pendiente
                List<CanalInvitacion> invitaciones = invitacionRepositorio.obtenerInvitacionesPendientesPorUsuario(usuarioId);
                CanalInvitacion invitacionCanal = invitaciones.stream()
                        .filter(inv -> inv.getCanalId().equals(canalId))
                        .findFirst()
                        .orElse(null);

                if (invitacionCanal == null) {
                    LoggerCentral.warn(TAG, ROJO + "No existe invitación pendiente para este canal" + RESET);
                    return new DTOResponse("rechazarInvitacion", "error", "No existe invitación pendiente", null);
                }

                // 6. Actualizar el estado de la invitación a "RECHAZADA"
                boolean actualizado = invitacionRepositorio.actualizarEstado(invitacionCanal.getIdUUID(), "RECHAZADA");

                if (!actualizado) {
                    LoggerCentral.error(TAG, ROJO + "❌ Error al actualizar el estado de la invitación" + RESET);
                    return new DTOResponse("rechazarInvitacion", "error", "Error al rechazar invitación", null);
                }

                LoggerCentral.info(TAG, ROJO + "✅ Estado de invitación actualizado a RECHAZADA" + RESET);

                // 7. Notificar sobre el rechazo (opcional)
                if (servicioNotificacion != null) {
                    Map<String, Object> notificacionData = new HashMap<>();
                    notificacionData.put("invitacionId", invitacionCanal.getId());
                    notificacionData.put("canalId", canalId.toString());
                    notificacionData.put("usuarioId", usuarioId.toString());

                    servicioNotificacion.actualizar("INVITACION_RECHAZADA", notificacionData);
                    LoggerCentral.info(TAG, ROJO + "✅ SIGNAL_UPDATE enviado para invitación rechazada" + RESET);
                }

                // 8. Activar sincronización P2P
                if (servicioSyncP2P != null) {
                    LoggerCentral.info(TAG, ROJO + "🔄 Activando sincronización P2P..." + RESET);
                    servicioSyncP2P.onBaseDeDatosCambio();
                    servicioSyncP2P.forzarSincronizacion();
                    LoggerCentral.info(TAG, ROJO + "✅ Sincronización P2P activada" + RESET);
                } else {
                    LoggerCentral.warn(TAG, ROJO + "⚠️ Servicio P2P no disponible, sincronización omitida" + RESET);
                }

                // 9. Notificar al sistema de cambios para activar P2P
                if (servicioNotificacionCambios != null) {
                    servicioNotificacionCambios.notificarCambio(
                            ServicioNotificacionCambios.TipoEvento.CAMBIO_INVITACION_CANAL,
                            invitacionCanal
                    );
                    LoggerCentral.info(TAG, ROJO + "🔄 Notificación de cambio de invitación enviada para sync P2P" + RESET);
                } else {
                    LoggerCentral.warn(TAG, ROJO + "⚠️ Notificador de cambios es NULL - La sync P2P podría no activarse" + RESET);
                }

                // 10. Preparar respuesta (definir invitacionId correctamente)
                UUID invitacionId = invitacionCanal.getIdUUID();
                Map<String, Object> respuesta = new HashMap<>();
                respuesta.put("invitacionId", invitacionId.toString());
                respuesta.put("canalId", canalId.toString());
                respuesta.put("estado", "RECHAZADA");
                respuesta.put("mensaje", "Invitación rechazada exitosamente");

                LoggerCentral.info(TAG, ROJO + "✅ Invitación rechazada exitosamente" + RESET);

                return new DTOResponse("rechazarInvitacion", "success", "Invitación rechazada exitosamente", gson.toJsonTree(respuesta));

            } catch (IllegalArgumentException e) {
                LoggerCentral.error(TAG, ROJO + "❌ Error: UUID inválido - " + e.getMessage() + RESET);
                return new DTOResponse("rechazarInvitacion", "error", "ID inválido: " + e.getMessage(), null);
            } catch (Exception e) {
                LoggerCentral.error(TAG, ROJO + "❌ Error en rechazarInvitacion: " + e.getMessage() + RESET);
                e.printStackTrace();
                return new DTOResponse("rechazarInvitacion", "error", "Error interno del servidor: " + e.getMessage(), null);
            }
        });

        LoggerCentral.info(TAG, ROJO + "✅ Servicio inicializado - Ruta 'rechazarInvitacion' registrada" + RESET);
    }

    @Override
    public void iniciar() {
        LoggerCentral.info(TAG, ROJO + "Servicio de rechazar invitación iniciado" + RESET);
    }

    @Override
    public void detener() {
        LoggerCentral.info(TAG, ROJO + "Servicio de rechazar invitación detenido" + RESET);
    }
}
