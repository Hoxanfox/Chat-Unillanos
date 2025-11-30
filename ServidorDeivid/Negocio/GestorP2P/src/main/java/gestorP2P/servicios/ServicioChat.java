package gestorP2P.servicios;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import conexion.p2p.interfaces.IRouterMensajes;
import conexion.p2p.interfaces.IGestorConexiones;
import dominio.clienteServidor.Mensaje;
import dto.comunicacion.DTORequest;
import dto.comunicacion.DTOResponse;
import dto.p2p.DTOPeerDetails;
import gestorP2P.interfaces.IServicioP2P;
import gestorP2P.utils.GsonUtil;
import logger.LoggerCentral;
import repositorio.clienteServidor.MensajeRepositorio;

import java.time.Instant;
import java.util.UUID;

public class ServicioChat implements IServicioP2P {

    private static final String TAG = "ServicioChat";
    private static final String CYAN = "\u001B[36m";
    private static final String RESET = "\u001B[0m";
    private static final String VERDE = "\u001B[32m";

    private IGestorConexiones gestorConexiones;
    private final Gson gson;
    private final MensajeRepositorio repositorio;

    // Referencia al servicio de sincronización para activar sync en lugar de notificar
    private ServicioSincronizacionDatos servicioSync;

    public ServicioChat() {
        this.gson = GsonUtil.crearGson();
        this.repositorio = new MensajeRepositorio();
    }

    /**
     * Inyecta el servicio de sincronización para activar sync automático.
     */
    public void setServicioSync(ServicioSincronizacionDatos sync) {
        this.servicioSync = sync;
    }

    @Override
    public String getNombre() { return "ServicioChat"; }

    @Override
    public void inicializar(IGestorConexiones gestor, IRouterMensajes router) {
        this.gestorConexiones = gestor;

        // =================================================================
        // RECEPCIÓN DE MENSAJES
        // =================================================================
        router.registrarAccion("mensajeChat", (payload, origenId) -> {
            if (payload == null) return null;

            try {
                JsonObject obj = payload.getAsJsonObject();
                String texto = obj.has("texto") ? obj.get("texto").getAsString() : "";
                String usuario = obj.has("usuario") ? obj.get("usuario").getAsString() : "Anónimo";

                Mensaje m = new Mensaje();
                if (obj.has("id")) {
                    m.setId(UUID.fromString(obj.get("id").getAsString()));
                } else {
                    m.setId(UUID.randomUUID());
                }
                m.setContenido(texto);
                m.setFechaEnvio(Instant.now());

                // Guardar
                boolean guardado = repositorio.guardar(m);

                // Activar sincronización automática si está disponible
                if (guardado && servicioSync != null) {
                    servicioSync.sincronizarMensajes();
                }

                String logMsg = String.format("%sMensaje de %s:%s %s", CYAN, usuario, RESET, texto);
                LoggerCentral.info(TAG, logMsg);

                return new DTOResponse("mensajeChatAck", "success", "Recibido", null);

            } catch (Exception e) {
                LoggerCentral.error(TAG, "Error procesando mensaje: " + e.getMessage());
                return new DTOResponse("mensajeChat", "error", "Malformado", null);
            }
        });
    }

    // =================================================================
    // ENVÍO DE MENSAJES
    // =================================================================

    public void enviarMensajePublico(String miNombreUsuario, String texto) {
        if (gestorConexiones == null) {
            LoggerCentral.error(TAG, "No hay conexión de red.");
            return;
        }

        // Crear el mensaje con ID único PRIMERO
        Mensaje m = new Mensaje();
        m.setId(UUID.randomUUID()); // Generar ID antes de todo
        m.setContenido(texto);
        m.setFechaEnvio(Instant.now());

        // Guardar en BD local
        boolean guardado = repositorio.guardar(m);

        LoggerCentral.debug(TAG, "Mensaje guardado localmente: " + guardado + " | ID: " + m.getId());

        // Activar sincronización automática si está disponible
        if (guardado && servicioSync != null) {
            servicioSync.sincronizarMensajes();
        }

        // Preparar JSON con todos los datos necesarios
        JsonObject msg = new JsonObject();
        msg.addProperty("id", m.getId().toString());
        msg.addProperty("usuario", miNombreUsuario != null ? miNombreUsuario : "Anónimo");
        msg.addProperty("texto", texto != null ? texto : "");

        DTORequest req = new DTORequest("mensajeChat", msg);
        String jsonMensaje = gson.toJson(req);

        LoggerCentral.info(TAG, CYAN + "Enviando mensaje público: " + RESET + texto);
        LoggerCentral.debug(TAG, "JSON enviado: " + jsonMensaje);

        gestorConexiones.broadcast(jsonMensaje);
    }

    public void enviarMensajePrivado(String peerId, String miNombreUsuario, String texto) {
        if (gestorConexiones == null) {
            LoggerCentral.error(TAG, "No hay conexión de red.");
            return;
        }

        // Crear mensaje con ID único
        Mensaje m = new Mensaje();
        m.setId(UUID.randomUUID());
        m.setContenido(texto);
        m.setFechaEnvio(Instant.now());

        boolean guardado = repositorio.guardar(m);

        LoggerCentral.debug(TAG, "Mensaje privado guardado: " + guardado + " | ID: " + m.getId());

        // Activar sincronización automática si está disponible
        if (guardado && servicioSync != null) {
            servicioSync.sincronizarMensajes();
        }

        // Preparar JSON
        JsonObject msg = new JsonObject();
        msg.addProperty("id", m.getId().toString());
        msg.addProperty("usuario", miNombreUsuario != null ? miNombreUsuario : "Anónimo");
        msg.addProperty("texto", texto != null ? texto : "");

        DTORequest req = new DTORequest("mensajeChat", msg);

        DTOPeerDetails destino = gestorConexiones.obtenerDetallesPeers().stream()
                .filter(p -> p.getId().equals(peerId)).findFirst().orElse(null);

        if (destino != null) {
            gestorConexiones.enviarMensaje(destino, gson.toJson(req));
            LoggerCentral.info(TAG, CYAN + "Enviado privado a " + peerId + ": " + RESET + texto);
        } else {
            LoggerCentral.error(TAG, "Peer destino no encontrado: " + peerId);
        }
    }

    @Override
    public void iniciar() {
        LoggerCentral.info(TAG, "Servicio de Chat listo.");
    }

    @Override
    public void detener() {}
}