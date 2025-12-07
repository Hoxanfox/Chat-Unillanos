package conexion.clientes.interfaces;

import com.google.gson.JsonElement;
import dto.comunicacion.DTOResponse;
import java.util.function.Consumer;

public interface IRouterMensajesCliente {
    // El peerOrigenId aquí será el idSesion del cliente
    void procesarMensaje(String json, String idSesion);

    void registrarAccion(String accion, IManejadorAccion manejador);

    @FunctionalInterface
    interface IManejadorAccion {
        DTOResponse ejecutar(JsonElement datos, String idSesion);
    }
}