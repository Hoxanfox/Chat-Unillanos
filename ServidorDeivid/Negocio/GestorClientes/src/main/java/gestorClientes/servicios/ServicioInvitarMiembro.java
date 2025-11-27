package gestorClientes.servicios;

import com.google.gson.Gson;
import conexion.clientes.interfaces.IGestorConexionesCliente;
import conexion.clientes.interfaces.IRouterMensajesCliente;
import dominio.clienteServidor.Canal;
import dominio.clienteServidor.relaciones.CanalInvitacion;
import dto.canales.DTOInvitarMiembro;
import dto.comunicacion.DTOResponse;
import gestorClientes.interfaces.IServicioCliente;
import gestorP2P.servicios.ServicioSincronizacionDatos;
import logger.LoggerCentral;
import repositorio.clienteServidor.CanalInvitacionRepositorio;
import repositorio.clienteServidor.CanalRepositorio;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio para gestionar invitaciones a canales.
 * Maneja la acción "invitarmiembro" que permite a un admin invitar usuarios a un canal.
 * Persiste la invitación en BD y sincroniza con la red P2P.
 */
public class ServicioInvitarMiembro implements IServicioCliente {

    private static final String TAG = "ServicioInvitarMiembro";

    // Colores ANSI para logs
    private static final String RESET = "\u001B[0m";
    private static final String VERDE = "\u001B[32m";
    private static final String AMARILLO = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";
    private static final String ROJO = "\u001B[31m";
    private static final String AZUL = "\u001B[34m";

    private IGestorConexionesCliente gestor;
    private final CanalInvitacionRepositorio invitacionRepositorio;
    private final CanalRepositorio canalRepositorio;
    private final Gson gson;

    // Referencias a servicios
    private ServicioNotificacionCliente servicioNotificacion;
    private ServicioSincronizacionDatos servicioSyncP2P;
    private ServicioNotificarInvitacionCanal servicioNotificarInvitacion;

    public ServicioInvitarMiembro() {
        this.invitacionRepositorio = new CanalInvitacionRepositorio();
        this.canalRepositorio = new CanalRepositorio();
        this.gson = new Gson();
        LoggerCentral.info(TAG, AZUL + "Constructor: ServicioInvitarMiembro creado" + RESET);
    }

    /**
     * Inyecta el servicio de notificaciones CS.
     */
    public void setServicioNotificacion(ServicioNotificacionCliente servicioNotificacion) {
        this.servicioNotificacion = servicioNotificacion;
        LoggerCentral.info(TAG, VERDE + "Servicio de notificaciones CS configurado" + RESET);
    }

    /**
     * Inyecta el servicio de sincronización P2P.
     */
    public void setServicioSincronizacionP2P(ServicioSincronizacionDatos servicioSyncP2P) {
        this.servicioSyncP2P = servicioSyncP2P;
        LoggerCentral.info(TAG, VERDE + "Servicio de sincronización P2P configurado" + RESET);
    }

    /**
     * Inyecta el servicio de notificación de invitaciones a canal.
     */
    public void setServicioNotificarInvitacion(ServicioNotificarInvitacionCanal servicioNotificarInvitacion) {
        this.servicioNotificarInvitacion = servicioNotificarInvitacion;
        LoggerCentral.info(TAG, VERDE + "Servicio de notificación de invitaciones configurado" + RESET);
    }

    @Override
    public String getNombre() {
        return "ServicioInvitarMiembro";
    }

    @Override
    public void inicializar(IGestorConexionesCliente gestor, IRouterMensajesCliente router) {
        this.gestor = gestor;
        LoggerCentral.info(TAG, AZUL + "Inicializando ServicioInvitarMiembro..." + RESET);

        // ==================== RUTA: Invitar Miembro ====================
        router.registrarAccion("invitarmiembro", (datos, idSesion) -> {
            try {
                LoggerCentral.info(TAG, CYAN + "📥 Recibida petición de invitar miembro" + RESET);

                // 1. Validar autenticación
                String adminId = gestor.obtenerUsuarioDeSesion(idSesion);
                if (adminId == null) {
                    LoggerCentral.warn(TAG, AMARILLO + "Usuario no autenticado" + RESET);
                    return new DTOResponse("invitarmiembro", "error", "Usuario no autenticado", null);
                }

                // 2. Parsear datos
                DTOInvitarMiembro dto = gson.fromJson(datos, DTOInvitarMiembro.class);

                // 3. Validar datos
                if (dto.getCanalId() == null || dto.getCanalId().trim().isEmpty()) {
                    LoggerCentral.warn(TAG, AMARILLO + "ID de canal inválido" + RESET);
                    return new DTOResponse("invitarmiembro", "error", "ID de canal requerido", null);
                }

                if (dto.getContactoId() == null || dto.getContactoId().trim().isEmpty()) {
                    LoggerCentral.warn(TAG, AMARILLO + "ID de contacto inválido" + RESET);
                    return new DTOResponse("invitarmiembro", "error", "ID de contacto requerido", null);
                }

                UUID canalId = UUID.fromString(dto.getCanalId());
                UUID contactoId = UUID.fromString(dto.getContactoId());
                UUID adminUUID = UUID.fromString(adminId);

                LoggerCentral.info(TAG, CYAN + "📤 Procesando invitación" + RESET);
                LoggerCentral.info(TAG, "   → Canal: " + canalId);
                LoggerCentral.info(TAG, "   → Invitador (Admin): " + adminId);
                LoggerCentral.info(TAG, "   → Invitado (Contacto): " + contactoId);

                // 4. Verificar que el canal existe
                Canal canal = canalRepositorio.obtenerPorId(canalId);
                if (canal == null) {
                    LoggerCentral.warn(TAG, AMARILLO + "Canal no encontrado" + RESET);
                    return new DTOResponse("invitarmiembro", "error", "Canal no encontrado", null);
                }

                // 5. Verificar que el invitador es miembro del canal (opcional: verificar si es admin)
                // Por simplicidad, asumimos que cualquier miembro puede invitar
                // En una implementación más robusta, verificarías permisos de administrador

                // 6. Verificar que el invitado no es ya miembro del canal
                // Esta verificación puede hacerse en el repositorio o aquí
                // Por ahora asumimos que el cliente lo valida

                // 7. Verificar si ya existe una invitación pendiente
                if (invitacionRepositorio.existeInvitacionPendiente(canalId, contactoId)) {
                    LoggerCentral.warn(TAG, AMARILLO + "Ya existe una invitación pendiente para este usuario" + RESET);
                    return new DTOResponse("invitarmiembro", "error", "Ya existe una invitación pendiente", null);
                }

                // 8. Crear la invitación
                CanalInvitacion invitacion = new CanalInvitacion(canalId, adminUUID, contactoId);

                // 9. Guardar en BD
                boolean guardado = invitacionRepositorio.guardar(invitacion);

                if (!guardado) {
                    LoggerCentral.error(TAG, ROJO + "❌ Error al guardar invitación en BD" + RESET);
                    return new DTOResponse("invitarmiembro", "error", "Error al guardar invitación", null);
                }

                LoggerCentral.info(TAG, VERDE + "✅ Invitación guardada en BD - ID: " + invitacion.getId() + RESET);

                // 10. ✅ Enviar notificación push detallada al usuario invitado
                if (servicioNotificarInvitacion != null) {
                    servicioNotificarInvitacion.notificarInvitacion(
                        canalId.toString(),
                        contactoId.toString(),
                        adminId
                    );
                    LoggerCentral.info(TAG, VERDE + "✅ Notificación push de invitación enviada" + RESET);
                } else {
                    LoggerCentral.warn(TAG, AMARILLO + "⚠️ Servicio de notificación de invitaciones no disponible" + RESET);
                }

                // 11. ✅ Notificar al usuario invitado (SIGNAL_UPDATE genérico)
                if (servicioNotificacion != null) {
                    // Notificar específicamente al usuario invitado sobre la nueva invitación
                    Map<String, Object> notificacionData = new HashMap<>();
                    notificacionData.put("invitacionId", invitacion.getId().toString());
                    notificacionData.put("canalId", canalId.toString());
                    notificacionData.put("canalNombre", canal.getNombre());
                    notificacionData.put("invitadorId", adminId);

                    servicioNotificacion.actualizar("NUEVA_INVITACION", notificacionData);
                    LoggerCentral.info(TAG, VERDE + "✅ SIGNAL_UPDATE enviado para nueva invitación" + RESET);
                }

                // 12. ✅ Activar sincronización P2P
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
                    LoggerCentral.warn(TAG, AMARILLO + "⚠️ La invitación se guardó pero NO se sincronizó con otros nodos" + RESET);
                }

                // 13. Preparar respuesta
                Map<String, Object> respuesta = new HashMap<>();
                respuesta.put("invitacionId", invitacion.getId().toString());
                respuesta.put("canalId", canalId.toString());
                respuesta.put("invitadoId", contactoId.toString());
                respuesta.put("estado", invitacion.getEstado());
                respuesta.put("fechaCreacion", invitacion.getFechaCreacion().toString());

                LoggerCentral.info(TAG, VERDE + "✅ Invitación creada exitosamente" + RESET);

                return new DTOResponse("invitarmiembro", "success", "Invitación enviada exitosamente", gson.toJsonTree(respuesta));

            } catch (IllegalArgumentException e) {
                LoggerCentral.error(TAG, ROJO + "❌ Error: UUID inválido - " + e.getMessage() + RESET);
                return new DTOResponse("invitarmiembro", "error", "ID inválido: " + e.getMessage(), null);
            } catch (Exception e) {
                LoggerCentral.error(TAG, ROJO + "❌ Error en invitarmiembro: " + e.getMessage() + RESET);
                e.printStackTrace();
                return new DTOResponse("invitarmiembro", "error", "Error interno del servidor: " + e.getMessage(), null);
            }
        });

        LoggerCentral.info(TAG, VERDE + "✅ Servicio inicializado - Ruta 'invitarmiembro' registrada" + RESET);
    }

    @Override
    public void iniciar() {
        LoggerCentral.info(TAG, "Servicio de invitar miembro iniciado");
    }

    @Override
    public void detener() {
        LoggerCentral.info(TAG, "Servicio de invitar miembro detenido");
    }
}
