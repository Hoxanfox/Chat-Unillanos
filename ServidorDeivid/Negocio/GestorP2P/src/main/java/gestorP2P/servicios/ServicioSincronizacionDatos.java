package gestorP2P.servicios;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import conexion.p2p.interfaces.IGestorConexiones;
import conexion.p2p.interfaces.IRouterMensajes;
import dominio.merkletree.IMerkleEntity;
import dto.comunicacion.DTOResponse;
import gestorP2P.interfaces.IServicioP2P;
import gestorP2P.servicios.sincronizacion.CoordinadorSincronizacion;
import gestorP2P.servicios.sincronizacion.Fase1ConstruccionArboles;
import gestorP2P.utils.GsonUtil;
import logger.LoggerCentral;
import observador.IObservador;
import observador.ISujeto;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Servicio de Sincronización de Datos P2P (Versión Refactorizada)
 *
 * Este servicio ahora es un FACADE que delega toda la lógica de sincronización
 * al CoordinadorSincronizacion. Su responsabilidad es:
 *
 * 1. Registrar las acciones P2P en el router
 * 2. Delegar el procesamiento al coordinador
 * 3. Notificar a observadores
 * 4. Proporcionar API pública para sincronización
 */
public class ServicioSincronizacionDatos implements IServicioP2P, IObservador, ISujeto {

    private static final String TAG = "SyncDatos";

    // Colores ANSI
    private static final String RESET = "\u001B[0m";
    private static final String VERDE = "\u001B[32m";
    private static final String AZUL = "\u001B[34m";
    private static final String CYAN = "\u001B[36m";
    private static final String ROJO = "\u001B[31m";

    private IGestorConexiones gestor;
    private final Gson gson;

    // Coordinador que maneja toda la lógica de sincronización
    private CoordinadorSincronizacion coordinador;

    // Servicios auxiliares
    private ServicioNotificacionCambios notificador;
    private ServicioTransferenciaArchivos servicioTransferenciaArchivos;

    // Patrón Observador
    private final List<IObservador> observadores = new CopyOnWriteArrayList<>();
    private IObservador servicioNotificacionCliente;

    public ServicioSincronizacionDatos() {
        this.gson = GsonUtil.crearGson();
    }

    @Override
    public String getNombre() {
        return "ServicioSincronizacionDatos";
    }

    /**
     * Configura el notificador de cambios.
     */
    public void setNotificador(ServicioNotificacionCambios notificador) {
        this.notificador = notificador;
        if (coordinador != null) {
            coordinador.configurarNotificaciones(notificador, servicioNotificacionCliente);
        }
    }

    /**
     * Configura el servicio de transferencia de archivos P2P.
     */
    public void setServicioTransferenciaArchivos(ServicioTransferenciaArchivos servicioTransferencia) {
        this.servicioTransferenciaArchivos = servicioTransferencia;
        if (coordinador != null) {
            coordinador.configurarTransferenciaArchivos(servicioTransferencia);
        }
        LoggerCentral.info(TAG, VERDE + "✓ Servicio de transferencia configurado" + RESET);
    }

    /**
     * Configura el servicio de notificación de clientes CS.
     */
    public void setServicioNotificacionCliente(IObservador servicioNotificacionCliente) {
        this.servicioNotificacionCliente = servicioNotificacionCliente;

        // Registrar como observador
        if (servicioNotificacionCliente != null) {
            registrarObservador(servicioNotificacionCliente);
            LoggerCentral.info(TAG, VERDE + "✓ ServicioNotificacionCliente registrado" + RESET);
        }

        // Configurar en coordinador
        if (coordinador != null) {
            coordinador.configurarNotificaciones(notificador, servicioNotificacionCliente);
        }
    }

    @Override
    public void inicializar(IGestorConexiones gestor, IRouterMensajes router) {
        LoggerCentral.info(TAG, AZUL + "=== Inicializando ServicioSincronizacionDatos ===" + RESET);
        this.gestor = gestor;

        // Crear coordinador
        this.coordinador = new CoordinadorSincronizacion(gestor, gson);

        // ✅ NUEVO: Configurar este servicio como padre para que el coordinador pueda
        // notificar a TODOS los observadores
        coordinador.setServicioPadre(this);

        // Configurar servicios si ya están disponibles
        if (notificador != null || servicioNotificacionCliente != null) {
            coordinador.configurarNotificaciones(notificador, servicioNotificacionCliente);
        }
        if (servicioTransferenciaArchivos != null) {
            coordinador.configurarTransferenciaArchivos(servicioTransferenciaArchivos);
        }

        // Registrar acciones P2P en el router
        registrarAccionesP2P(router);
        registrarManejadoresRespuesta(router);

        LoggerCentral.info(TAG, VERDE + "✓ ServicioSincronizacionDatos inicializado" + RESET);
    }

    /**
     * Registra las acciones P2P que este servicio puede responder.
     */
    private void registrarAccionesP2P(IRouterMensajes router) {
        // 1. CHECK ALL - Enviar todos los hashes
        router.registrarAccion("sync_check_all", (datos, origen) -> {
            LoggerCentral.debug(TAG, "sync_check_all desde: " + origen);
            coordinador.reconstruirArboles();
            JsonObject hashes = coordinador.getFase1().obtenerHashesRaiz();
            return new DTOResponse("sync_check_all", "success", "Hashes Locales", hashes);
        });

        // 2. GET IDs - Enviar lista de IDs para un tipo
        router.registrarAccion("sync_get_ids", (datos, origen) -> {
            String tipo = datos.getAsString();
            LoggerCentral.debug(TAG, "sync_get_ids para: " + tipo + " desde: " + origen);

            JsonArray ids = coordinador.getFase3().obtenerIDsLocales(tipo);

            JsonObject result = new JsonObject();
            result.addProperty("tipo", tipo);
            result.add("ids", ids);
            return new DTOResponse("sync_get_ids", "success", "IDs", result);
        });

        // 3. GET ENTITY - Enviar una entidad específica
        router.registrarAccion("sync_get_entity", (datos, origen) -> {
            JsonObject req = datos.getAsJsonObject();
            String tipo = req.get("tipo").getAsString();
            String id = req.get("id").getAsString();

            LoggerCentral.debug(TAG, String.format("sync_get_entity: %s ID: %s", tipo, id));

            IMerkleEntity entidad = coordinador.getFase1().buscarEntidad(tipo, id);
            if (entidad != null) {
                JsonObject env = new JsonObject();
                env.addProperty("tipo", tipo);
                env.add("data", gson.toJsonTree(entidad));
                return new DTOResponse("sync_get_entity", "success", "Found", env);
            }

            return new DTOResponse("sync_get_entity", "error", "Not found", null);
        });

        // 4. COMPARE ENTITY - Enviar entidad para comparación
        router.registrarAccion("sync_compare_entity", (datos, origen) -> {
            JsonObject req = datos.getAsJsonObject();
            String tipo = req.get("tipo").getAsString();
            String id = req.get("id").getAsString();

            LoggerCentral.debug(TAG, String.format("sync_compare_entity: %s ID: %s", tipo, id));

            IMerkleEntity entidad = coordinador.getFase1().buscarEntidad(tipo, id);
            if (entidad != null) {
                JsonObject env = new JsonObject();
                env.addProperty("tipo", tipo);
                env.add("data", gson.toJsonTree(entidad));
                return new DTOResponse("sync_compare_entity", "success", "Found", env);
            }

            return new DTOResponse("sync_compare_entity", "error", "Not found", null);
        });
    }

    /**
     * Registra los manejadores de respuestas P2P.
     */
    private void registrarManejadoresRespuesta(IRouterMensajes router) {
        // Respuesta a sync_check_all
        router.registrarManejadorRespuesta("sync_check_all", (resp) -> {
            if (resp.fueExitoso() && resp.getData() != null) {
                LoggerCentral.info(TAG, CYAN + "✓ Respuesta sync_check_all recibida" + RESET);
                coordinador.procesarDiferencias(resp.getData().getAsJsonObject());
            }
        });

        // Respuesta a sync_get_ids
        router.registrarManejadorRespuesta("sync_get_ids", (resp) -> {
            if (resp.fueExitoso()) {
                JsonObject res = resp.getData().getAsJsonObject();
                String tipo = res.get("tipo").getAsString();
                JsonArray ids = res.get("ids").getAsJsonArray();

                LoggerCentral.info(TAG, CYAN + String.format("✓ IDs recibidos: %d de %s", ids.size(), tipo) + RESET);
                coordinador.procesarIDsRecibidos(tipo, ids);
            }
        });

        // Respuesta a sync_get_entity
        router.registrarManejadorRespuesta("sync_get_entity", (resp) -> {
            if (resp.fueExitoso()) {
                JsonObject env = resp.getData().getAsJsonObject();
                String tipo = env.get("tipo").getAsString();
                JsonElement data = env.get("data");

                LoggerCentral.info(TAG, VERDE + "✓ Entidad recibida: " + tipo + RESET);
                coordinador.procesarEntidadRecibida(tipo, data);
            }
        });

        // ✅ MEJORADO: Respuesta a sync_compare_entity con deduplicación
        router.registrarManejadorRespuesta("sync_compare_entity", (resp) -> {
            if (resp.fueExitoso()) {
                JsonObject env = resp.getData().getAsJsonObject();
                String tipo = env.get("tipo").getAsString();
                JsonElement data = env.get("data");

                // ✅ NUEVO: Extraer el ID de la entidad para deduplicación
                String id = extraerIdDeEntidad(tipo, data);

                // ✅ NUEVO: Verificar si ya procesamos esta respuesta
                if (coordinador.getFase5().yaFueProcesado(tipo, id)) {
                    LoggerCentral.debug(TAG, "⏩ Respuesta duplicada ignorada: " + tipo + " ID: " + id);
                    return;
                }

                LoggerCentral.info(TAG, CYAN + "✓ Comparación de contenido para: " + tipo + RESET);
                coordinador.procesarComparacion(tipo, data);
            }
        });
    }

    /**
     * ✅ NUEVO: Extrae el ID de una entidad desde JSON.
     */
    private String extraerIdDeEntidad(String tipo, JsonElement data) {
        try {
            JsonObject obj = data.getAsJsonObject();

            // Diferentes campos según el tipo de entidad
            if (obj.has("id")) {
                return obj.get("id").getAsString();
            } else if (obj.has("idUsuario")) {
                return obj.get("idUsuario").getAsString();
            } else if (obj.has("idCanal")) {
                return obj.get("idCanal").getAsString();
            }

            // Si no tiene ID, usar un hash del contenido
            return Integer.toString(data.hashCode());
        } catch (Exception e) {
            LoggerCentral.warn(TAG, "No se pudo extraer ID, usando hashCode");
            return Integer.toString(data.hashCode());
        }
    }

    @Override
    public void iniciar() {
        LoggerCentral.info(TAG, AZUL + "=== Iniciando servicio de sincronización ===" + RESET);
        coordinador.reconstruirArboles();
        LoggerCentral.info(TAG, VERDE + "✓ Servicio iniciado correctamente" + RESET);
    }

    // API pública para disparar sincronización, notificando inicio a observadores
    public void iniciarSincronizacionConNotificacion() {
        LoggerCentral.info(TAG, AZUL + "=== Solicitud de sincronización con notificación ===" + RESET);
        // Notificar a la UI y otros observadores que se inicia la sincronización
        notificarObservadores("SINCRONIZACION_P2P_INICIADA", null);
        if (coordinador != null) {
            coordinador.iniciarSincronizacion();
        }
    }

    @Override
    public void detener() {
        LoggerCentral.info(TAG, ROJO + "Deteniendo servicio de sincronización..." + RESET);
    }

    /**
     * API Pública: Fuerza una sincronización manual.
     */
    public void forzarSincronizacion() {
        if (coordinador == null) {
            LoggerCentral.error(TAG, ROJO + "Coordinador no inicializado" + RESET);
            return;
        }
        LoggerCentral.warn(TAG, "Forzando sincronización manual...");
        // ✅ Usar siempre el flujo con notificación para que la UI reciba
        // SINCRONIZACION_P2P_INICIADA
        iniciarSincronizacionConNotificacion();
    }

    /**
     * API Pública: Sincroniza mensajes (llamado por ServicioChat).
     */
    public void sincronizarMensajes() {
        if (coordinador == null) {
            LoggerCentral.debug(TAG, "Coordinador no disponible. Sincronización diferida.");
            return;
        }
        LoggerCentral.info(TAG, VERDE + "📨 Activando sincronización por nuevo mensaje..." + RESET);
        coordinador.marcarCambios();
        // ✅ Usar también el flujo con notificación
        iniciarSincronizacionConNotificacion();
    }

    /**
     * API Pública: Reconstruye árboles Merkle cuando cambia la BD.
     */
    public void onBaseDeDatosCambio() {
        if (coordinador == null) {
            return;
        }
        LoggerCentral.info(TAG, AZUL + "BD cambió. Reconstruyendo árboles..." + RESET);
        new Thread(() -> coordinador.reconstruirArboles()).start();
    }

    @Override
    public void actualizar(String tipo, Object datos) {
        // ✅ EVENTO 1: Nuevo peer conectado → Iniciar sincronización (Cold Sync)
        if ("PEER_CONECTADO".equals(tipo)) {
            LoggerCentral.info(TAG, VERDE + "=== Peer conectado: " + datos + " ===" + RESET);
            if (coordinador != null) {
                // ✅ OPTIMIZADO: Usar árboles en memoria, no reconstruir
                iniciarSincronizacionConNotificacion();
            }
            return;
        }

        // ✅ EVENTO 2: Usuario creado/actualizado → Reconstruir SOLO usuarios
        if ("USUARIO_CREADO".equals(tipo) || "USUARIO_ACTUALIZADO".equals(tipo)) {
            LoggerCentral.info(TAG, AZUL + "👤 Usuario modificado. Activando sincronización..." + RESET);
            if (coordinador != null) {
                coordinador.marcarCambios();
                // ✅ OPTIMIZADO: Solo reconstruir árbol de usuarios
                coordinador.reconstruirArbol(Fase1ConstruccionArboles.TIPO_USUARIO);
                iniciarSincronizacionConNotificacion();
            }
            return;
        }

        // ✅ EVENTO 3: Nuevo mensaje → Reconstruir SOLO mensajes
        if ("NUEVO_MENSAJE".equals(tipo)) {
            LoggerCentral.info(TAG, CYAN + "📨 Nuevo mensaje. Sincronizando..." + RESET);
            if (coordinador != null) {
                coordinador.marcarCambios();
                coordinador.reconstruirArbol(Fase1ConstruccionArboles.TIPO_MENSAJE);
                iniciarSincronizacionConNotificacion();
            }
            return;
        }

        // ✅ EVENTO 4: Nuevo canal → Reconstruir SOLO canales
        if ("NUEVO_CANAL".equals(tipo)) {
            LoggerCentral.info(TAG, CYAN + "📢 Nuevo canal. Sincronizando..." + RESET);
            if (coordinador != null) {
                coordinador.marcarCambios();
                coordinador.reconstruirArbol(Fase1ConstruccionArboles.TIPO_CANAL);
                iniciarSincronizacionConNotificacion();
            }
            return;
        }

        // ✅ EVENTO 5: Cambio en invitación → Reconstruir SOLO invitaciones
        if ("CAMBIO_INVITACION_CANAL".equals(tipo)) {
            LoggerCentral.info(TAG, AZUL + "✉️ Cambio en invitación. Sincronizando..." + RESET);
            if (coordinador != null) {
                coordinador.marcarCambios();
                coordinador.reconstruirArbol(Fase1ConstruccionArboles.TIPO_CANAL_INVITACION);
                iniciarSincronizacionConNotificacion();
            }
            return;
        }

        // ✅ EVENTO 6: Cambio genérico de BD → Reconstrucción completa (Fallback)
        if ("BD_CAMBIO".equals(tipo)) {
            LoggerCentral.info(TAG, CYAN + "💾 Cambio genérico en BD. Reconstruyendo todo..." + RESET);
            if (coordinador != null) {
                coordinador.marcarCambios();
                coordinador.reconstruirArboles();
                iniciarSincronizacionConNotificacion();
            }
            return;
        }

        // Evento desconocido
        LoggerCentral.debug(TAG, "Evento no manejado: " + tipo);
    }

    // ===== PATRÓN OBSERVADOR =====

    @Override
    public void registrarObservador(IObservador observador) {
        if (!observadores.contains(observador)) {
            observadores.add(observador);
            LoggerCentral.info(TAG, "✅ Observador registrado");
        }
    }

    @Override
    public void removerObservador(IObservador observador) {
        if (observadores.remove(observador)) {
            LoggerCentral.info(TAG, "Observador removido");
        }
    }

    @Override
    public void notificarObservadores(String tipoDeDato, Object datos) {
        for (IObservador obs : observadores) {
            try {
                obs.actualizar(tipoDeDato, datos);
            } catch (Exception e) {
                LoggerCentral.error(TAG, "Error notificando: " + e.getMessage());
            }
        }
    }

    /**
     * Obtiene el coordinador (útil para testing).
     */
    public CoordinadorSincronizacion getCoordinador() {
        return coordinador;
    }

    /**
     * ✅ NUEVO: Expone el ServicioNotificacionCambios para inyección en servicios
     * CS.
     * Esto permite que servicios como ServicioInvitarMiembro activen sincronización
     * automática después de persistir datos.
     */
    public ServicioNotificacionCambios getServicioNotificacionCambios() {
        return notificador;
    }
}
