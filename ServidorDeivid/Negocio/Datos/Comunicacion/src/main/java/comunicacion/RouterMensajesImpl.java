package comunicacion; // Asegúrate del package correcto

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import comunicacion.IRouterMensajes;
import conexion.IGestorConexiones;
import dto.comunicacion.DTORequest;
import dto.comunicacion.DTOResponse;
import dto.p2p.DTOPeerDetails;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class RouterMensajesImpl implements IRouterMensajes {

    private final Gson gson;

    // Mapa para peticiones (Request -> Response)
    private final Map<String, IManejadorAccion> rutasPeticiones;

    // NUEVO: Mapa para respuestas (Response -> void)
    private final Map<String, Consumer<DTOResponse>> rutasRespuestas;

    private final IGestorConexiones gestorConexiones;

    public RouterMensajesImpl(IGestorConexiones gestorConexiones) {
        this.gestorConexiones = gestorConexiones;
        this.rutasPeticiones = new HashMap<>();
        this.rutasRespuestas = new HashMap<>(); // Inicializamos el nuevo mapa
        this.gson = new GsonBuilder().serializeNulls().create();
    }

    @Override
    public void registrarAccion(String accion, IManejadorAccion manejador) {
        rutasPeticiones.put(accion.toLowerCase(), manejador);
    }

    // IMPLEMENTACIÓN DEL MÉTODO FALTANTE
    @Override
    public void registrarManejadorRespuesta(String accion, Consumer<DTOResponse> manejador) {
        rutasRespuestas.put(accion.toLowerCase(), manejador);
    }

    @Override
    public void procesarMensaje(String json, String peerOrigenId) {
        try {
            // 1. Intentamos como Request
            DTORequest request = gson.fromJson(json, DTORequest.class);
            // Verificamos si es Request válido (tiene action y NO tiene status)
            if (request != null && request.getAction() != null && !json.contains("\"status\"")) {
                manejarPeticion(request, peerOrigenId);
                return;
            }

            // 2. Intentamos como Response
            DTOResponse response = gson.fromJson(json, DTOResponse.class);
            if (response != null && response.getStatus() != null) {
                manejarRespuesta(response, peerOrigenId);
                return;
            }

            System.out.println("[Router] JSON ignorado (no es Request ni Response válido): " + json);

        } catch (JsonSyntaxException e) {
            System.err.println("[Router] Error JSON inválido de " + peerOrigenId);
        }
    }

    private void manejarPeticion(DTORequest request, String peerOrigenId) {
        String accion = request.getAction().toLowerCase();
        IManejadorAccion handler = rutasPeticiones.get(accion);

        if (handler != null) {
            // System.out.println("[Router] Ejecutando acción: " + accion);
            try {
                DTOResponse respuesta = handler.ejecutar(request.getPayload(), peerOrigenId);
                if (respuesta != null) {
                    enviarRespuesta(respuesta, peerOrigenId);
                }
            } catch (Exception e) {
                enviarRespuesta(new DTOResponse(request.getAction(), "error", "Error interno: " + e.getMessage(), null), peerOrigenId);
            }
        } else {
            System.out.println("[Router] Acción desconocida: " + accion);
            enviarRespuesta(new DTOResponse(request.getAction(), "error", "Acción no soportada", null), peerOrigenId);
        }
    }

    private void manejarRespuesta(DTOResponse response, String peerOrigenId) {
        String accion = response.getAction().toLowerCase();

        // Buscamos si alguien registró un manejador para esta respuesta (ej. AccionesP2P para "añadirPeer")
        Consumer<DTOResponse> handler = rutasRespuestas.get(accion);

        if (handler != null) {
            try {
                handler.accept(response);
            } catch (Exception e) {
                System.err.println("[Router] Error procesando respuesta '" + accion + "': " + e.getMessage());
            }
        } else {
            // Si nadie escucha, solo logueamos
            System.out.println("[Router] Respuesta recibida sin manejador (" + accion + "): " + response.getMessage());
        }
    }

    private void enviarRespuesta(DTOResponse respuesta, String peerDestinoId) {
        String jsonRespuesta = gson.toJson(respuesta);
        DTOPeerDetails destino = new DTOPeerDetails(peerDestinoId, null, 0, null, null);
        gestorConexiones.enviarMensaje(destino, jsonRespuesta);
    }
}