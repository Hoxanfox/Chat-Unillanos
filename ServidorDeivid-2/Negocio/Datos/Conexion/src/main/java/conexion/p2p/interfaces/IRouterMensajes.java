package conexion.p2p.interfaces;

import com.google.gson.JsonElement;
import dto.comunicacion.DTOResponse; // Nota el cambio de paquete
import java.util.function.Consumer;

public interface IRouterMensajes {
    void procesarMensaje(String json, String peerOrigenId);
    void registrarAccion(String accion, IManejadorAccion manejador);
    void registrarManejadorRespuesta(String accion, Consumer<DTOResponse> manejador);

    @FunctionalInterface
    interface IManejadorAccion {
        DTOResponse ejecutar(JsonElement datos, String peerOrigenId);
    }
}