package comunicacion; // Asegúrate que el package sea correcto

import com.google.gson.JsonElement;
import dto.comunicacion.DTOResponse;
import java.util.function.Consumer;

public interface IRouterMensajes {

    /**
     * Procesa un mensaje JSON crudo recibido por la red.
     */
    void procesarMensaje(String json, String peerOrigenId);

    /**
     * Registra una acción para manejar PETICIONES (Requests).
     * Retorna una respuesta inmediata.
     */
    void registrarAccion(String accion, IManejadorAccion manejador);

    /**
     * NUEVO: Registra un consumidor para manejar RESPUESTAS (Responses).
     * Se ejecuta cuando recibimos una respuesta a una acción que iniciamos.
     * Esto es lo que te faltaba.
     */
    void registrarManejadorRespuesta(String accion, Consumer<DTOResponse> manejador);

    @FunctionalInterface
    interface IManejadorAccion {
        DTOResponse ejecutar(JsonElement datos, String peerOrigenId);
    }
}