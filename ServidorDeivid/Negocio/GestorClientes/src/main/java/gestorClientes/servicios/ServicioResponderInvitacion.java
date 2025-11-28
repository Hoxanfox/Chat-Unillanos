package gestorClientes.servicios;

import com.google.gson.Gson;
import conexion.clientes.interfaces.IGestorConexionesCliente;
import conexion.clientes.interfaces.IRouterMensajesCliente;
import dominio.clienteServidor.Canal;
import dominio.clienteServidor.relaciones.CanalInvitacion;
import dominio.clienteServidor.relaciones.CanalMiembro;
import dto.comunicacion.DTOResponse;
import gestorClientes.interfaces.IServicioCliente;
import gestorP2P.servicios.ServicioNotificacionCambios;
import gestorP2P.servicios.ServicioSincronizacionDatos;
import logger.LoggerCentral;
import repositorio.clienteServidor.CanalInvitacionRepositorio;
import repositorio.clienteServidor.CanalMiembroRepositorio;
import repositorio.clienteServidor.CanalRepositorio;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio unificado para responder invitaciones a canales.
 * Maneja la acción "responderInvitacion" que permite aceptar o rechazar una invitación.
 */
public class ServicioResponderInvitacion implements IServicioCliente {

    private static final String TAG = "ServicioResponderInvitacion";

    // Colores ANSI para logs
    private static final String RESET = "\u001B[0m";
    private static final String VERDE = "\u001B[32m";
    private static final String AMARILLO = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";
    private static final String ROJO = "\u001B[31m";
    private static final String AZUL = "\u001B[34m";

    private IGestorConexionesCliente gestor;
    private final CanalInvitacionRepositorio invitacionRepositorio;
    private final CanalMiembroRepositorio miembroRepositorio;
    private final CanalRepositorio canalRepositorio;
    private final Gson gson;

    // Referencias a servicios
    private ServicioSincronizacionDatos servicioSyncP2P;
    private ServicioNotificacionCliente servicioNotificacion;
    private ServicioNotificacionCambios servicioNotificacionCambios; // ✅ NUEVO

    public ServicioResponderInvitacion() {
        this.invitacionRepositorio = new CanalInvitacionRepositorio();
        this.miembroRepositorio = new CanalMiembroRepositorio();
        this.canalRepositorio = new CanalRepositorio();
        this.gson = new Gson();
        LoggerCentral.info(TAG, AZUL + "Constructor: ServicioResponderInvitacion creado" + RESET);
    }

    /**
     * Inyecta el servicio de sincronización P2P.
     */
    public void setServicioSincronizacionP2P(ServicioSincronizacionDatos servicioSyncP2P) {
        this.servicioSyncP2P = servicioSyncP2P;
        LoggerCentral.info(TAG, VERDE + "Servicio de sincronización P2P configurado" + RESET);
    }

    /**
     * Inyecta el servicio de notificaciones CS.
     */
    public void setServicioNotificacion(ServicioNotificacionCliente servicioNotificacion) {
        this.servicioNotificacion = servicioNotificacion;
        LoggerCentral.info(TAG, VERDE + "Servicio de notificaciones CS configurado" + RESET);
    }

    /**
     * ✅ NUEVO: Inyecta el notificador de cambios central.
     */
    public void setServicioNotificacionCambios(ServicioNotificacionCambios servicio) {
        this.servicioNotificacionCambios = servicio;
        LoggerCentral.info(TAG, VERDE + "Servicio de notificación de cambios configurado" + RESET);
    }

    @Override
    public String getNombre() {
        return "ServicioResponderInvitacion";
    }

    @Override
    public void inicializar(IGestorConexionesCliente gestor, IRouterMensajesCliente router) {
        this.gestor = gestor;
        LoggerCentral.info(TAG, AZUL + "Inicializando ServicioResponderInvitacion..." + RESET);

        // ==================== RUTA: Responder Invitación ====================
        router.registrarAccion("responderInvitacion", (datos, idSesion) -> {
            try {
                LoggerCentral.info(TAG, CYAN + "📥 Recibida petición de responder invitación" + RESET);

                // 1. Validar autenticación
                String usuarioIdSesion = gestor.obtenerUsuarioDeSesion(idSesion);
                if (usuarioIdSesion == null) {
                    LoggerCentral.warn(TAG, AMARILLO + "Usuario no autenticado" + RESET);
                    return new DTOResponse("responderInvitacion", "error", "Usuario no autenticado", null);
                }

                // 2. Parsear datos
                Map<String, Object> payload = gson.fromJson(datos, Map.class);
                String channelId = payload.get("channelId") != null ? payload.get("channelId").toString() : null;
                Boolean accepted = payload.get("accepted") != null ? (Boolean) payload.get("accepted") : null;

                // 3. Validar datos
                if (channelId == null || channelId.trim().isEmpty()) {
                    LoggerCentral.warn(TAG, AMARILLO + "ID de canal inválido" + RESET);
                    return new DTOResponse("responderInvitacion", "error", "ID de canal requerido", null);
                }

                if (accepted == null) {
                    LoggerCentral.warn(TAG, AMARILLO + "Campo 'accepted' requerido" + RESET);
                    return new DTOResponse("responderInvitacion", "error", "Campo 'accepted' requerido", null);
                }

                UUID canalId = UUID.fromString(channelId);
                UUID usuarioId = UUID.fromString(usuarioIdSesion);

                LoggerCentral.info(TAG, CYAN + "📤 Procesando respuesta a invitación" + RESET);
                LoggerCentral.info(TAG, "   → Canal: " + canalId);
                LoggerCentral.info(TAG, "   → Usuario: " + usuarioId);
                LoggerCentral.info(TAG, "   → Acción: " + (accepted ? "ACEPTAR" : "RECHAZAR"));

                // 4. Verificar que el canal existe
                Canal canal = canalRepositorio.obtenerPorId(canalId);
                if (canal == null) {
                    LoggerCentral.warn(TAG, AMARILLO + "Canal no encontrado" + RESET);
                    return new DTOResponse("responderInvitacion", "error", "Canal no encontrado", null);
                }

                // 5. Buscar la invitación pendiente
                List<CanalInvitacion> invitaciones = invitacionRepositorio.obtenerInvitacionesPendientesPorUsuario(usuarioId);
                CanalInvitacion invitacionCanal = invitaciones.stream()
                        .filter(inv -> inv.getCanalId().equals(canalId))
                        .findFirst()
                        .orElse(null);

                if (invitacionCanal == null) {
                    LoggerCentral.warn(TAG, AMARILLO + "No existe invitación pendiente para este canal" + RESET);
                    return new DTOResponse("responderInvitacion", "error", "No existe invitación pendiente", null);
                }

                // 6. Procesar según la respuesta
                if (accepted) {
                    // ACEPTAR: Agregar como miembro y actualizar invitación
                    LoggerCentral.info(TAG, VERDE + "✅ Aceptando invitación..." + RESET);

                    // Agregar al usuario como miembro del canal
                    CanalMiembro nuevoMiembro = new CanalMiembro(canalId, usuarioId);
                    boolean guardadoMiembro = miembroRepositorio.guardar(nuevoMiembro);

                    if (!guardadoMiembro) {
                        LoggerCentral.error(TAG, ROJO + "❌ Error al agregar usuario como miembro" + RESET);
                        return new DTOResponse("responderInvitacion", "error", "Error al unirse al canal", null);
                    }

                    // Actualizar estado de la invitación a ACEPTADA
                    boolean actualizado = invitacionRepositorio.actualizarEstado(invitacionCanal.getIdUUID(), "ACEPTADA");
                    if (!actualizado) {
                        LoggerCentral.warn(TAG, AMARILLO + "⚠️ No se pudo actualizar estado de invitación" + RESET);
                    }

                    LoggerCentral.info(TAG, VERDE + "✅ Usuario agregado al canal exitosamente" + RESET);

                } else {
                    // RECHAZAR: Solo actualizar estado de la invitación
                    LoggerCentral.info(TAG, AMARILLO + "❌ Rechazando invitación..." + RESET);

                    boolean actualizado = invitacionRepositorio.actualizarEstado(invitacionCanal.getIdUUID(), "RECHAZADA");
                    if (!actualizado) {
                        LoggerCentral.warn(TAG, AMARILLO + "⚠️ No se pudo actualizar estado de invitación" + RESET);
                    }

                    LoggerCentral.info(TAG, VERDE + "✅ Invitación rechazada exitosamente" + RESET);
                }

                // 7. Notificar cambio a todos los clientes
                if (servicioNotificacion != null) {
                    servicioNotificacion.actualizar(accepted ? "INVITACION_ACEPTADA" : "INVITACION_RECHAZADA",
                        Map.of("canalId", canalId.toString(), "usuarioId", usuarioId.toString()));
                }

                // 8. Sincronizar con P2P
                if (servicioSyncP2P != null) {
                    LoggerCentral.info(TAG, CYAN + "🔄 Activando sincronización P2P..." + RESET);
                    try {
                        servicioSyncP2P.onBaseDeDatosCambio();
                        servicioSyncP2P.forzarSincronizacion();
                        LoggerCentral.info(TAG, VERDE + "✅ Sincronización P2P activada exitosamente" + RESET);
                    } catch (Exception e) {
                        LoggerCentral.error(TAG, ROJO + "❌ Error al forzar sincronización P2P: " + e.getMessage() + RESET);
                    }
                } else {
                    LoggerCentral.error(TAG, ROJO + "❌ CRÍTICO: Servicio P2P es NULL - NO SE SINCRONIZARÁ" + RESET);
                }

                // 9. Notificar cambios de invitación a través del ServicioNotificacionCambios
                if (servicioNotificacionCambios != null) {
                    servicioNotificacionCambios.notificarCambio(
                            ServicioNotificacionCambios.TipoEvento.CAMBIO_INVITACION_CANAL,
                            invitacionCanal
                    );
                    LoggerCentral.info(TAG, CYAN + "🔄 Notificación de cambio de invitación enviada para sync P2P" + RESET);
                } else {
                    LoggerCentral.warn(TAG, AMARILLO + "⚠️ Notificador de cambios es NULL - La sync P2P podría no activarse" + RESET);
                }

                // 10. Preparar respuesta (definir invitacionId a partir de la entidad)
                UUID invitacionId = invitacionCanal.getIdUUID();
                Map<String, Object> respuesta = new HashMap<>();
                respuesta.put("invitacionId", invitacionId.toString());
                respuesta.put("accion", accepted ? "ACEPTADA" : "RECHAZADA");
                respuesta.put("canalId", canalId.toString());
                respuesta.put("usuarioId", usuarioId.toString());

                LoggerCentral.info(TAG, VERDE + "✅ Invitación procesada exitosamente" + RESET);

                return new DTOResponse(
                    "responderInvitacion",
                    "success",
                    accepted ? "Invitación aceptada" : "Invitación rechazada",
                    gson.toJsonTree(respuesta));

            } catch (IllegalArgumentException e) {
                LoggerCentral.error(TAG, ROJO + "❌ Error: UUID inválido - " + e.getMessage() + RESET);
                return new DTOResponse("responderInvitacion", "error", "ID inválido: " + e.getMessage(), null);
            } catch (Exception e) {
                LoggerCentral.error(TAG, ROJO + "❌ Error en responderInvitacion: " + e.getMessage() + RESET);
                e.printStackTrace();
                return new DTOResponse("responderInvitacion", "error", "Error interno del servidor: " + e.getMessage(), null);
            }
        });

        LoggerCentral.info(TAG, VERDE + "✅ Servicio inicializado - Ruta 'responderInvitacion' registrada" + RESET);
    }

    @Override
    public void iniciar() {
        LoggerCentral.info(TAG, "Servicio de responder invitación iniciado");
    }

    @Override
    public void detener() {
        LoggerCentral.info(TAG, "Servicio de responder invitación detenido");
    }
}
