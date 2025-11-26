package gestorClientes.servicios;

import com.google.gson.Gson;
import conexion.clientes.interfaces.IGestorConexionesCliente;
import conexion.clientes.interfaces.IRouterMensajesCliente;
import dominio.clienteServidor.Canal;
import dominio.clienteServidor.Usuario;
import dominio.clienteServidor.relaciones.CanalMiembro;
import dto.canales.DTOCrearCanal;
import dto.comunicacion.DTOResponse;
import gestorClientes.interfaces.IServicioCliente;
import gestorP2P.servicios.ServicioSincronizacionDatos;
import logger.LoggerCentral;
import repositorio.clienteServidor.CanalMiembroRepositorio;
import repositorio.clienteServidor.CanalRepositorio;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio para crear nuevos canales en el servidor.
 * Persiste en BD y activa sincronización P2P.
 */
public class ServicioCrearCanal implements IServicioCliente {

    private static final String TAG = "ServicioCrearCanal";

    // Colores ANSI para logs
    private static final String RESET = "\u001B[0m";
    private static final String VERDE = "\u001B[32m";
    private static final String AMARILLO = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";
    private static final String ROJO = "\u001B[31m";
    private static final String AZUL = "\u001B[34m";

    private IGestorConexionesCliente gestor;
    private final CanalRepositorio canalRepositorio;
    private final CanalMiembroRepositorio miembroRepositorio;
    private final Gson gson;

    // Referencias a servicios
    private ServicioNotificacionCliente servicioNotificacion;
    private ServicioSincronizacionDatos servicioSyncP2P;

    public ServicioCrearCanal() {
        this.canalRepositorio = new CanalRepositorio();
        this.miembroRepositorio = new CanalMiembroRepositorio();
        this.gson = new Gson();
        LoggerCentral.info(TAG, AZUL + "Constructor: ServicioCrearCanal creado" + RESET);
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
        return "ServicioCrearCanal";
    }

    @Override
    public void inicializar(IGestorConexionesCliente gestor, IRouterMensajesCliente router) {
        this.gestor = gestor;
        LoggerCentral.info(TAG, AZUL + "Inicializando ServicioCrearCanal..." + RESET);

        // ==================== RUTA: Crear Canal ====================
        router.registrarAccion("crearcanal", (datos, idSesion) -> {
            try {
                LoggerCentral.info(TAG, CYAN + "📥 Recibida petición de crear canal" + RESET);

                // Validar autenticación
                String userId = gestor.obtenerUsuarioDeSesion(idSesion);
                if (userId == null) {
                    LoggerCentral.warn(TAG, AMARILLO + "Usuario no autenticado" + RESET);
                    return new DTOResponse("crearcanal", "error", "Usuario no autenticado", null);
                }

                // Parsear datos
                DTOCrearCanal dto = gson.fromJson(datos, DTOCrearCanal.class);

                // Validar datos
                if (dto.getNombre() == null || dto.getNombre().trim().isEmpty()) {
                    LoggerCentral.warn(TAG, AMARILLO + "Nombre de canal inválido" + RESET);
                    return new DTOResponse("crearcanal", "error", "Nombre de canal requerido", null);
                }

                // Validar tipo de canal
                String tipoCanal = dto.getTipo();
                if (tipoCanal == null || (!tipoCanal.equals("PRIVADO") && !tipoCanal.equals("PUBLICO") && !tipoCanal.equals("GRUPO"))) {
                    tipoCanal = "PUBLICO"; // Valor por defecto
                }

                // Si el tipo es "GRUPO", convertirlo a "PUBLICO" (compatibilidad con cliente)
                if (tipoCanal.equals("GRUPO")) {
                    tipoCanal = "PUBLICO";
                }

                LoggerCentral.info(TAG, CYAN + "📤 Creando nuevo canal" + RESET);
                LoggerCentral.info(TAG, "   → Nombre: " + dto.getNombre());
                LoggerCentral.info(TAG, "   → Tipo: " + tipoCanal);
                LoggerCentral.info(TAG, "   → Creador: " + userId);

                // Crear canal en BD
                UUID canalId = UUID.randomUUID();
                Usuario creador = new Usuario();
                creador.setId(UUID.fromString(userId));

                Canal canal = new Canal(
                    canalId,
                    null, // peerPadre
                    creador,
                    Canal.Tipo.valueOf(tipoCanal),
                    dto.getNombre(),
                    Instant.now()
                );

                // Guardar canal en BD
                boolean guardadoCanal = canalRepositorio.guardar(canal);

                if (!guardadoCanal) {
                    LoggerCentral.error(TAG, ROJO + "❌ Error al guardar canal en BD" + RESET);
                    return new DTOResponse("crearcanal", "error", "Error al guardar canal", null);
                }

                LoggerCentral.info(TAG, VERDE + "✅ Canal guardado en BD - ID: " + canalId + RESET);

                // Agregar al creador como miembro del canal
                CanalMiembro miembro = new CanalMiembro(canalId, UUID.fromString(userId));
                boolean guardadoMiembro = miembroRepositorio.guardar(miembro);

                if (!guardadoMiembro) {
                    LoggerCentral.warn(TAG, AMARILLO + "⚠️ Advertencia: No se pudo agregar creador como miembro" + RESET);
                }

                LoggerCentral.info(TAG, VERDE + "✅ Creador agregado como miembro del canal" + RESET);

                // ✅ 1. SIGNAL_UPDATE a clientes CS
                if (servicioNotificacion != null) {
                    servicioNotificacion.actualizar("NUEVO_CANAL", null);
                    LoggerCentral.info(TAG, VERDE + "✅ SIGNAL_UPDATE enviado a clientes CS" + RESET);
                }

                // ✅ 2. Activar sincronización P2P
                if (servicioSyncP2P != null) {
                    LoggerCentral.info(TAG, CYAN + "🔄 Activando sincronización P2P..." + RESET);
                    servicioSyncP2P.onBaseDeDatosCambio();
                    servicioSyncP2P.forzarSincronizacion();
                    LoggerCentral.info(TAG, VERDE + "✅ Sincronización P2P activada" + RESET);
                } else {
                    LoggerCentral.warn(TAG, AMARILLO + "⚠️ Servicio P2P no disponible, sincronización omitida" + RESET);
                }

                // Preparar respuesta
                Map<String, Object> respuesta = new HashMap<>();
                respuesta.put("id", canalId.toString());
                respuesta.put("channelId", canalId.toString()); // Compatibilidad con cliente
                respuesta.put("nombre", canal.getNombre());
                respuesta.put("channelName", canal.getNombre()); // Compatibilidad con cliente
                respuesta.put("tipo", canal.getTipo().name());
                respuesta.put("creadorId", userId);
                respuesta.put("fechaCreacion", canal.getFechaCreacion().toString());

                // Información del creador (owner)
                Map<String, Object> owner = new HashMap<>();
                owner.put("userId", userId);
                owner.put("id", userId);
                respuesta.put("owner", owner);

                LoggerCentral.info(TAG, VERDE + "✅ Canal creado exitosamente: " + canal.getNombre() + RESET);

                return new DTOResponse("crearcanal", "success", "Canal creado exitosamente", gson.toJsonTree(respuesta));

            } catch (Exception e) {
                LoggerCentral.error(TAG, ROJO + "❌ Error en crearcanal: " + e.getMessage() + RESET);
                e.printStackTrace();
                return new DTOResponse("crearcanal", "error", "Error interno del servidor: " + e.getMessage(), null);
            }
        });

        LoggerCentral.info(TAG, VERDE + "✅ Servicio inicializado - Ruta 'crearcanal' registrada" + RESET);
    }

    @Override
    public void iniciar() {
        LoggerCentral.info(TAG, "Servicio de crear canal iniciado");
    }

    @Override
    public void detener() {
        LoggerCentral.info(TAG, "Servicio de crear canal detenido");
    }
}

