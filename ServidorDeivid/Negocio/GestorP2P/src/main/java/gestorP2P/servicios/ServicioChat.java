package gestorP2P.servicios;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import conexion.interfaces.IRouterMensajes;
import conexion.interfaces.IGestorConexiones;
import dto.comunicacion.DTORequest;
import dto.comunicacion.DTOResponse;
import dto.p2p.DTOPeerDetails;
import gestorP2P.interfaces.IServicioP2P;

public class ServicioChat implements IServicioP2P {

    // Ya no es final porque se inyecta después en 'inicializar'
    private IGestorConexiones gestorConexiones;
    private final Gson gson;

    // 1. CONSTRUCTOR VACÍO (Soluciona el error de la imagen)
    public ServicioChat() {
        this.gson = new Gson();
    }

    @Override
    public String getNombre() { return "ServicioChat"; }

    // 2. INYECCIÓN Y REGISTRO DE RUTAS (Reemplaza al constructor antiguo)
    @Override
    public void inicializar(IGestorConexiones gestor, IRouterMensajes router) {
        this.gestorConexiones = gestor;

        // Registramos las rutas aquí mismo
        router.registrarAccion("mensajeChat", (payload, origenId) -> {
            if (payload == null) return null;

            JsonObject obj = payload.getAsJsonObject();
            String texto = obj.has("texto") ? obj.get("texto").getAsString() : "";
            String usuario = obj.has("usuario") ? obj.get("usuario").getAsString() : "Anónimo";

            System.out.println("\n---------------------------------------");
            System.out.println("[CHAT] " + usuario + " (" + origenId + "): " + texto);
            System.out.println("---------------------------------------");

            return new DTOResponse("mensajeChatAck", "success", "Leído", null);
        });
    }

    public void enviarMensajePublico(String miNombreUsuario, String texto) {
        if (gestorConexiones == null) return; // Protección

        JsonObject msg = new JsonObject();
        msg.addProperty("usuario", miNombreUsuario);
        msg.addProperty("texto", texto);

        DTORequest req = new DTORequest("mensajeChat", msg);
        gestorConexiones.broadcast(gson.toJson(req));
    }

    public void enviarMensajePrivado(String peerId, String miNombreUsuario, String texto) {
        if (gestorConexiones == null) return;

        JsonObject msg = new JsonObject();
        msg.addProperty("usuario", miNombreUsuario);
        msg.addProperty("texto", texto);

        DTORequest req = new DTORequest("mensajeChat", msg);

        DTOPeerDetails destino = gestorConexiones.obtenerDetallesPeers().stream()
                .filter(p -> p.getId().equals(peerId)).findFirst().orElse(null);

        if (destino != null) {
            gestorConexiones.enviarMensaje(destino, gson.toJson(req));
        }
    }

    @Override
    public void iniciar() {
        System.out.println("[Chat] Servicio listo.");
    }

    @Override
    public void detener() {
        // Nada que limpiar
    }
}