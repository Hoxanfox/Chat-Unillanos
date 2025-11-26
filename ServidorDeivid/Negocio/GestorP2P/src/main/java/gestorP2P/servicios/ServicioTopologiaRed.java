package gestorP2P.servicios;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import conexion.p2p.interfaces.IRouterMensajes;
import conexion.p2p.interfaces.IGestorConexiones;
import dto.comunicacion.DTOResponse;
import dto.comunicacion.DTORequest;
import dto.topologia.DTOTopologiaRed;
import dto.cliente.DTOSesionCliente;
import dto.p2p.DTOPeerDetails;
import gestorP2P.interfaces.IServicioP2P;
import gestorP2P.utils.GsonUtil;
import logger.LoggerCentral;
import observador.ISujeto;
import observador.IObservador;
import configuracion.Configuracion; // ✅ NUEVO

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Servicio que sincroniza la topología de red entre peers.
 *
 * Funcionalidades:
 * - Envía la topología local a todos los peers cada 5 segundos
 * - Recibe topologías de otros peers y las almacena
 * - Notifica cambios en la topología a observadores (para la UI)
 * - Se activa cuando hay cambios en clientes conectados
 */
public class ServicioTopologiaRed implements IServicioP2P, ISujeto {

    private static final String TAG = "TopologiaRed";
    private static final long INTERVALO_SINCRONIZACION_MS = 5000; // 5 segundos

    private IGestorConexiones gestorConexiones;
    private final Gson gson;
    private final Configuracion config; // ✅ NUEVO

    // Topología local (del servidor actual)
    private DTOTopologiaRed topologiaLocal;

    // Topologías remotas (de otros peers)
    private final Map<String, DTOTopologiaRed> topologiasRemotas = new ConcurrentHashMap<>();

    // Observadores (para notificar a la UI)
    private final List<IObservador> observadores = new CopyOnWriteArrayList<>();

    // Timer para envío periódico
    private Timer timer;

    // ✅ Supplier para obtener clientes (se inyecta desde fuera)
    private Supplier<List<DTOSesionCliente>> proveedorClientes;

    // Información local del peer
    private String idLocal = "LOCAL";
    private int puertoLocal = 0;

    private boolean activo = false;

    public ServicioTopologiaRed() {
        this.gson = GsonUtil.crearGson();
        this.config = Configuracion.getInstance(); // ✅ NUEVO: Obtener configuración
        LoggerCentral.info(TAG, "ServicioTopologiaRed creado");
    }

    @Override
    public String getNombre() {
        return "ServicioTopologiaRed";
    }

    @Override
    public void inicializar(IGestorConexiones gestor, IRouterMensajes router) {
        this.gestorConexiones = gestor;

        LoggerCentral.info(TAG, "Inicializando ServicioTopologiaRed...");

        // RUTA 1: Recibir actualizaciones de topología de otros peers
        router.registrarAccion("actualizarTopologia", (payload, origenId) -> {
            LoggerCentral.debug(TAG, "📥 Topología recibida de: " + origenId);
            recibirTopologiaRemota(origenId, payload);
            return new DTOResponse("topologiaRecibida", "success", "Topología actualizada", null);
        });

        // RUTA 2: Solicitar topología de un peer específico
        router.registrarAccion("solicitarTopologia", (payload, origenId) -> {
            LoggerCentral.debug(TAG, "📥 Solicitud de topología de: " + origenId);
            DTOTopologiaRed topo = construirTopologiaLocal();
            JsonElement data = gson.toJsonTree(topo);
            return new DTOResponse("respuestaTopologia", "success", "Aquí está mi topología", data);
        });

        LoggerCentral.info(TAG, "✅ Rutas de topología registradas");
    }

    @Override
    public void iniciar() {
        if (timer != null) {
            LoggerCentral.warn(TAG, "Timer ya estaba activo");
            return;
        }

        this.activo = true;
        this.timer = new Timer("TopologiaSync-Timer", true);

        LoggerCentral.info(TAG, "🚀 Iniciando sincronización periódica de topología (cada 5 segundos)...");

        // Primera ejecución después de 3 segundos, luego cada 5 segundos
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                enviarTopologiaATodos();
            }
        }, 3000, INTERVALO_SINCRONIZACION_MS);

        LoggerCentral.info(TAG, "✅ ServicioTopologiaRed iniciado correctamente");
    }

    /**
     * ✅ Inyecta el proveedor de clientes (Supplier) para obtener sesiones activas.
     * Esto evita dependencias circulares entre módulos.
     */
    public void setProveedorClientes(Supplier<List<DTOSesionCliente>> proveedor) {
        this.proveedorClientes = proveedor;
        LoggerCentral.info(TAG, "✅ Proveedor de clientes inyectado en ServicioTopologiaRed");
    }

    /**
     * Configura la información local del peer usando la configuración
     */
    public void configurarInfoLocal(String idLocal, int puertoLocal) {
        this.idLocal = idLocal;
        this.puertoLocal = puertoLocal;
        LoggerCentral.debug(TAG, "Info local configurada: " + idLocal + ":" + puertoLocal);
    }

    /**
     * Construye la topología local actual
     */
    private DTOTopologiaRed construirTopologiaLocal() {
        DTOTopologiaRed topo = new DTOTopologiaRed();
        topo.setIdPeer(idLocal);

        // ✅ CORREGIDO: Usar configuración real en lugar de localhost hardcodeado
        String host = config.getPeerHost();
        int puerto = puertoLocal > 0 ? puertoLocal : config.getPeerPuerto();

        topo.setIpPeer(host);
        topo.setPuertoPeer(puerto);
        topo.setEstadoPeer("ONLINE");

        // Obtener clientes conectados del proveedor
        if (proveedorClientes != null) {
            try {
                List<DTOSesionCliente> clientes = proveedorClientes.get();
                topo.setClientesConectados(clientes != null ? clientes : new ArrayList<>());
                LoggerCentral.debug(TAG, "Topología local: " + topo.getNumeroClientes() + " clientes conectados");
            } catch (Exception e) {
                LoggerCentral.error(TAG, "Error obteniendo clientes: " + e.getMessage());
                topo.setClientesConectados(new ArrayList<>());
            }
        } else {
            topo.setClientesConectados(new ArrayList<>());
            LoggerCentral.debug(TAG, "Topología local: 0 clientes (proveedor no configurado)");
        }

        return topo;
    }

    /**
     * Envía la topología local a todos los peers conectados
     */
    private void enviarTopologiaATodos() {
        if (!activo || gestorConexiones == null) {
            return;
        }

        try {
            // Construir topología actualizada
            topologiaLocal = construirTopologiaLocal();

            // Obtener peers conectados
            List<DTOPeerDetails> peers = gestorConexiones.obtenerDetallesPeers();

            if (peers == null || peers.isEmpty()) {
                LoggerCentral.debug(TAG, "No hay peers conectados, omitiendo envío");
                return;
            }

            LoggerCentral.info(TAG, "📡 Enviando topología a " + peers.size() +
                " peers (" + topologiaLocal.getNumeroClientes() + " clientes locales)");

            // Crear mensaje con la topología
            DTORequest request = new DTORequest("actualizarTopologia", gson.toJsonTree(topologiaLocal));
            String mensaje = gson.toJson(request);

            // Enviar a todos los peers
            gestorConexiones.broadcast(mensaje);

            // Notificar a observadores sobre actualización
            notificarObservadores("TOPOLOGIA_ACTUALIZADA", obtenerTopologiaCompleta());

        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error en envío de topología: " + e.getMessage());
        }
    }

    /**
     * Fuerza el envío inmediato de la topología (útil cuando cambia algo)
     */
    public void forzarActualizacion() {
        LoggerCentral.info(TAG, "🔄 Forzando actualización inmediata de topología");
        enviarTopologiaATodos();
    }

    /**
     * Procesa una topología recibida de un peer remoto
     */
    private void recibirTopologiaRemota(String idPeer, JsonElement payload) {
        try {
            if (payload == null) {
                LoggerCentral.warn(TAG, "Payload de topología nulo de: " + idPeer);
                return;
            }

            DTOTopologiaRed topoRemota = gson.fromJson(payload, DTOTopologiaRed.class);

            if (topoRemota == null) {
                LoggerCentral.warn(TAG, "No se pudo deserializar topología de: " + idPeer);
                return;
            }

            // Guardar/actualizar topología del peer remoto
            topologiasRemotas.put(idPeer, topoRemota);

            LoggerCentral.info(TAG, "📥 Topología actualizada de " + idPeer +
                ": " + topoRemota.getNumeroClientes() + " clientes");

            // Notificar a observadores
            notificarObservadores("TOPOLOGIA_REMOTA_RECIBIDA", obtenerTopologiaCompleta());

        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error procesando topología de " + idPeer + ": " + e.getMessage());
        }
    }

    /**
     * Obtiene la topología completa (local + remotas)
     */
    public Map<String, DTOTopologiaRed> obtenerTopologiaCompleta() {
        Map<String, DTOTopologiaRed> topologiaTotal = new HashMap<>(topologiasRemotas);

        if (topologiaLocal != null) {
            topologiaTotal.put(topologiaLocal.getIdPeer(), topologiaLocal);
        }

        return topologiaTotal;
    }

    /**
     * Limpia la topología de un peer desconectado
     */
    public void limpiarPeerDesconectado(String idPeer) {
        if (topologiasRemotas.remove(idPeer) != null) {
            LoggerCentral.info(TAG, "🗑️ Topología de peer desconectado eliminada: " + idPeer);
            notificarObservadores("PEER_DESCONECTADO", idPeer);
            notificarObservadores("TOPOLOGIA_ACTUALIZADA", obtenerTopologiaCompleta());
        }
    }

    @Override
    public void detener() {
        LoggerCentral.info(TAG, "Deteniendo ServicioTopologiaRed...");
        activo = false;

        // Cancelar timer
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        topologiasRemotas.clear();
        LoggerCentral.info(TAG, "✅ ServicioTopologiaRed detenido");
    }

    // ===== IMPLEMENTACIÓN ISujeto =====

    @Override
    public void registrarObservador(IObservador observador) {
        if (!observadores.contains(observador)) {
            observadores.add(observador);
            LoggerCentral.debug(TAG, "Observador registrado");
        }
    }

    @Override
    public void removerObservador(IObservador observador) {
        observadores.remove(observador);
        LoggerCentral.debug(TAG, "Observador eliminado");
    }

    @Override
    public void notificarObservadores(String tipoDeDato, Object datos) {
        for (IObservador obs : observadores) {
            try {
                obs.actualizar(tipoDeDato, datos);
            } catch (Exception e) {
                LoggerCentral.error(TAG, "Error notificando observador: " + e.getMessage());
            }
        }
    }
}
