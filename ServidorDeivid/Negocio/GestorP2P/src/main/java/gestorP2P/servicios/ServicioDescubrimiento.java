package gestorP2P.servicios;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import conexion.interfaces.IRouterMensajes;
import conexion.interfaces.IGestorConexiones;
import dto.comunicacion.DTOResponse;
import dto.comunicacion.DTORequest;
import dto.p2p.DTOPeerDetails;
import gestorP2P.interfaces.IServicioP2P;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ServicioDescubrimiento implements IServicioP2P {

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
            List<DTOPeerDetails> misPeers = gestorConexiones.obtenerDetallesPeers();
            JsonElement data = gson.toJsonTree(misPeers);
            return new DTOResponse("respuestaPeers", "success", "Aquí tienes mis peers", data);
        });

        // RUTA 2: (Opcional) Procesar respuesta explícita si no usas el callback directo
        router.registrarAccion("respuestaPeers", (payload, origenId) -> {
            // Lógica si fuera necesaria
            return null;
        });
    }

    @Override
    public void iniciar() {
        if (timer != null) return;
        this.timer = new Timer();

        // Tarea periódica de Gossip
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                hacerGossip();
            }
        }, 5000, 30000);

        System.out.println("[Descubrimiento] Auto-descubrimiento iniciado.");
    }

    private void hacerGossip() {
        if (gestorConexiones == null) return;

        List<DTOPeerDetails> peers = gestorConexiones.obtenerDetallesPeers();
        if (peers.isEmpty()) return;

        DTORequest req = new DTORequest("dameTusPeers", null);
        gestorConexiones.broadcast(gson.toJson(req));
    }

    @Override
    public void detener() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}