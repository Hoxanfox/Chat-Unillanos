package gestorClientes.servicios;

import com.google.gson.Gson;
import conexion.clientes.interfaces.IGestorConexionesCliente;
import conexion.clientes.interfaces.IRouterMensajesCliente;
import dominio.clienteServidor.Canal;
import dominio.clienteServidor.relaciones.CanalInvitacion;
import dominio.clienteServidor.relaciones.CanalMiembro;
import dto.canales.DTOUnirseCanal;
import dto.comunicacion.DTOResponse;
import gestorClientes.interfaces.IServicioCliente;
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
 * Servicio para gestionar la aceptación de invitaciones a canales.
 * Maneja la acción "unirseCanal" que permite a un usuario aceptar una invitación y unirse a un canal.
 * Persiste el nuevo miembro en BD y sincroniza con la red P2P.
 */
public class ServicioUnirseCanal implements IServicioCliente {

    private static final String TAG = "ServicioUnirseCanal";

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
    private ServicioNotificacionCliente servicioNotificacion;
    private ServicioSincronizacionDatos servicioSyncP2P;

    public ServicioUnirseCanal() {
        this.invitacionRepositorio = new CanalInvitacionRepositorio();
        this.miembroRepositorio = new CanalMiembroRepositorio();
        this.canalRepositorio = new CanalRepositorio();
        this.gson = new Gson();
        LoggerCentral.info(TAG, AZUL + "Constructor: ServicioUnirseCanal creado" + RESET);
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

    @Override
    public String getNombre() {
        return "ServicioUnirseCanal";
    }

    @Override
    public void inicializar(IGestorConexionesCliente gestor, IRouterMensajesCliente router) {
        this.gestor = gestor;
        LoggerCentral.info(TAG, AZUL + "Inicializando ServicioUnirseCanal..." + RESET);

        // ==================== RUTA: Unirse a Canal (Aceptar Invitación) ====================
        router.registrarAccion("unirseCanal", (datos, idSesion) -> {
            try {
                LoggerCentral.info(TAG, CYAN + "📥 Recibida petición de unirse a canal" + RESET);

                // 1. Validar autenticación
                String usuarioIdSesion = gestor.obtenerUsuarioDeSesion(idSesion);
                if (usuarioIdSesion == null) {
                    LoggerCentral.warn(TAG, AMARILLO + "Usuario no autenticado" + RESET);
                    return new DTOResponse("unirseCanal", "error", "Usuario no autenticado", null);
                }

                // 2. Parsear datos
                DTOUnirseCanal dto = gson.fromJson(datos, DTOUnirseCanal.class);

                // 3. Validar datos
                if (dto.getCanalId() == null || dto.getCanalId().trim().isEmpty()) {
                    LoggerCentral.warn(TAG, AMARILLO + "ID de canal inválido" + RESET);
                    return new DTOResponse("unirseCanal", "error", "ID de canal requerido", null);
                }

                if (dto.getUsuarioId() == null || dto.getUsuarioId().trim().isEmpty()) {
                    LoggerCentral.warn(TAG, AMARILLO + "ID de usuario inválido" + RESET);
                    return new DTOResponse("unirseCanal", "error", "ID de usuario requerido", null);
                }

                UUID canalId = UUID.fromString(dto.getCanalId());
                UUID usuarioId = UUID.fromString(dto.getUsuarioId());

                // 4. Verificar que el usuario del DTO coincide con el de la sesión
                if (!usuarioId.toString().equals(usuarioIdSesion)) {
                    LoggerCentral.warn(TAG, AMARILLO + "Usuario no coincide con la sesión" + RESET);
                    return new DTOResponse("unirseCanal", "error", "No autorizado", null);
                }

                LoggerCentral.info(TAG, CYAN + "📤 Procesando unión a canal" + RESET);
                LoggerCentral.info(TAG, "   → Canal: " + canalId);
                LoggerCentral.info(TAG, "   → Usuario: " + usuarioId);

                // 5. Verificar que el canal existe
                Canal canal = canalRepositorio.obtenerPorId(canalId);
                if (canal == null) {
                    LoggerCentral.warn(TAG, AMARILLO + "Canal no encontrado" + RESET);
                    return new DTOResponse("unirseCanal", "error", "Canal no encontrado", null);
                }

                // 6. Verificar que existe una invitación pendiente
                List<CanalInvitacion> invitaciones = invitacionRepositorio.obtenerInvitacionesPendientesPorUsuario(usuarioId);
                CanalInvitacion invitacionCanal = invitaciones.stream()
                        .filter(inv -> inv.getCanalId().equals(canalId))
                        .findFirst()
                        .orElse(null);

                if (invitacionCanal == null) {
                    LoggerCentral.warn(TAG, AMARILLO + "No existe invitación pendiente para este canal" + RESET);
                    return new DTOResponse("unirseCanal", "error", "No existe invitación pendiente", null);
                }

                // 7. Verificar que el usuario no es ya miembro del canal
                // (Esta verificación es opcional, ya que el INSERT IGNORE lo maneja)

                // 8. Agregar al usuario como miembro del canal
                CanalMiembro nuevoMiembro = new CanalMiembro(canalId, usuarioId);
                boolean guardadoMiembro = miembroRepositorio.guardar(nuevoMiembro);

                if (!guardadoMiembro) {
                    LoggerCentral.error(TAG, ROJO + "❌ Error al agregar usuario como miembro del canal" + RESET);
                    return new DTOResponse("unirseCanal", "error", "Error al unirse al canal", null);
                }

                LoggerCentral.info(TAG, VERDE + "✅ Usuario agregado como miembro del canal" + RESET);

                // 9. Actualizar el estado de la invitación a "ACEPTADA"
                boolean actualizado = invitacionRepositorio.actualizarEstado(invitacionCanal.getIdUUID(), "ACEPTADA");
                if (actualizado) {
                    LoggerCentral.info(TAG, VERDE + "✅ Estado de invitación actualizado a ACEPTADA" + RESET);
                } else {
                    LoggerCentral.warn(TAG, AMARILLO + "⚠️ No se pudo actualizar el estado de la invitación" + RESET);
                }

                // 10. ✅ Notificar a todos los miembros del canal sobre el nuevo miembro (SIGNAL_UPDATE)
                if (servicioNotificacion != null) {
                    Map<String, Object> notificacionData = new HashMap<>();
                    notificacionData.put("canalId", canalId.toString());
                    notificacionData.put("nuevoMiembroId", usuarioId.toString());
                    notificacionData.put("canalNombre", canal.getNombre());

                    servicioNotificacion.actualizar("NUEVO_MIEMBRO_CANAL", notificacionData);
                    LoggerCentral.info(TAG, VERDE + "✅ SIGNAL_UPDATE enviado para nuevo miembro" + RESET);
                }

                // 11. ✅ Activar sincronización P2P
                if (servicioSyncP2P != null) {
                    LoggerCentral.info(TAG, CYAN + "🔄 Activando sincronización P2P..." + RESET);
                    servicioSyncP2P.onBaseDeDatosCambio();
                    servicioSyncP2P.forzarSincronizacion();
                    LoggerCentral.info(TAG, VERDE + "✅ Sincronización P2P activada" + RESET);
                } else {
                    LoggerCentral.warn(TAG, AMARILLO + "⚠️ Servicio P2P no disponible, sincronización omitida" + RESET);
                }

                // 12. Preparar respuesta
                Map<String, Object> respuesta = new HashMap<>();
                respuesta.put("canalId", canalId.toString());
                respuesta.put("usuarioId", usuarioId.toString());
                respuesta.put("canalNombre", canal.getNombre());
                respuesta.put("mensaje", "Te has unido exitosamente al canal");

                LoggerCentral.info(TAG, VERDE + "✅ Usuario unido al canal exitosamente" + RESET);

                return new DTOResponse("unirseCanal", "success", "Te has unido exitosamente al canal", gson.toJsonTree(respuesta));

            } catch (IllegalArgumentException e) {
                LoggerCentral.error(TAG, ROJO + "❌ Error: UUID inválido - " + e.getMessage() + RESET);
                return new DTOResponse("unirseCanal", "error", "ID inválido: " + e.getMessage(), null);
            } catch (Exception e) {
                LoggerCentral.error(TAG, ROJO + "❌ Error en unirseCanal: " + e.getMessage() + RESET);
                e.printStackTrace();
                return new DTOResponse("unirseCanal", "error", "Error interno del servidor: " + e.getMessage(), null);
            }
        });

        LoggerCentral.info(TAG, VERDE + "✅ Servicio inicializado - Ruta 'unirseCanal' registrada" + RESET);
    }

    @Override
    public void iniciar() {
        LoggerCentral.info(TAG, "Servicio de unirse a canal iniciado");
    }

    @Override
    public void detener() {
        LoggerCentral.info(TAG, "Servicio de unirse a canal detenido");
    }
}

