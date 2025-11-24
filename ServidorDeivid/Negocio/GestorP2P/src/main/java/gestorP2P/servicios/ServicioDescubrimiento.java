package gestorP2P.servicios;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import conexion.p2p.interfaces.IRouterMensajes;
import conexion.p2p.interfaces.IGestorConexiones;
import dto.comunicacion.DTOResponse;
import dto.comunicacion.DTORequest;
import dto.p2p.DTOPeerDetails;
import gestorP2P.interfaces.IServicioP2P;
import logger.LoggerCentral; // Importar LoggerCentral

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ServicioDescubrimiento implements IServicioP2P {

    private static final String TAG = "Descubrimiento"; // Tag para los logs

    private IGestorConexiones gestorConexiones;
    private final Gson gson;
    private Timer timer;

    // Constructor vacío
    public ServicioDescubrimiento() {
        this.gson = new Gson();
    }

    @Override
    public String getNombre() { return "ServicioDescubrimiento"; }

    @Override
    public void inicializar(IGestorConexiones gestor, IRouterMensajes router) {
        this.gestorConexiones = gestor;

        // RUTA 1: Alguien me pide mis peers
        router.registrarAccion("dameTusPeers", (payload, origenId) -> {
            // LoggerCentral.debug(TAG, "Solicitud de peers recibida de " + origenId);
            List<DTOPeerDetails> misPeers = gestorConexiones.obtenerDetallesPeers();
            JsonElement data = gson.toJsonTree(misPeers);
            return new DTOResponse("respuestaPeers", "success", "Aquí tienes mis peers", data);
        });

        // RUTA 2: Procesar respuesta explícita (si llegara fuera del callback)
        router.registrarAccion("respuestaPeers", (payload, origenId) -> {
            // LoggerCentral.debug(TAG, "Respuesta genérica de peers recibida.");
            return null;
        });
    }

    @Override
    public void iniciar() {
        if (timer != null) return;
        this.timer = new Timer();

        LoggerCentral.info(TAG, "Iniciando servicio de auto-descubrimiento (Gossip)...");

        // Tarea periódica de Gossip (cada 30 segundos)
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                hacerGossip();
            }
        }, 5000, 30000);
    }

    private void hacerGossip() {
        if (gestorConexiones == null) return;

        List<DTOPeerDetails> peers = gestorConexiones.obtenerDetallesPeers();
        if (peers.isEmpty()) return;

        // LoggerCentral.debug(TAG, "Ejecutando Gossip con " + peers.size() + " peers.");

        DTORequest req = new DTORequest("dameTusPeers", null);
        gestorConexiones.broadcast(gson.toJson(req));
    }

    @Override
    public void detener() {
        if (timer != null) {
            LoggerCentral.info(TAG, "Deteniendo servicio.");
            timer.cancel();
            timer = null;
        }
    }
}