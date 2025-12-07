package conexion.clientes.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import conexion.clientes.interfaces.IGestorConexionesCliente;
import conexion.clientes.interfaces.IRouterMensajesCliente;
import dto.comunicacion.DTORequest;
import dto.comunicacion.DTOResponse;

import java.util.HashMap;
import java.util.Map;

public class RouterMensajesClienteImpl implements IRouterMensajesCliente {

    private final Gson gson;
    private final Map<String, IManejadorAccion> rutas;
    private final IGestorConexionesCliente gestorClientes;

    public RouterMensajesClienteImpl(IGestorConexionesCliente gestorClientes) {
        this.gestorClientes = gestorClientes;
        this.rutas = new HashMap<>();
        this.gson = new GsonBuilder().serializeNulls().create();
    }

    @Override
    public void registrarAccion(String accion, IManejadorAccion manejador) {
        rutas.put(accion.toLowerCase(), manejador);
    }

    @Override
    public void procesarMensaje(String json, String idSesion) {
        try {
            DTORequest request = gson.fromJson(json, DTORequest.class);

            if (request != null && request.getAction() != null) {
                manejarPeticion(request, idSesion);
            } else {
                System.err.println("[RouterCliente] JSON inválido o sin acción: " + idSesion);
            }
        } catch (JsonSyntaxException e) {
            System.err.println("[RouterCliente] Error JSON de " + idSesion + ": " + e.getMessage());
        }
    }

    private void manejarPeticion(DTORequest request, String idSesion) {
        String accion = request.getAction().toLowerCase();
        IManejadorAccion handler = rutas.get(accion);

        if (handler != null) {
            try {
                DTOResponse respuesta = handler.ejecutar(request.getPayload(), idSesion);
                if (respuesta != null) {
                    String jsonRes = gson.toJson(respuesta);
                    gestorClientes.enviarMensaje(idSesion, jsonRes);
                }
            } catch (Exception e) {
                e.printStackTrace(); // Debug
                enviarError(idSesion, request.getAction(), "Error interno: " + e.getMessage());
            }
        } else {
            enviarError(idSesion, request.getAction(), "Acción desconocida");
        }
    }

    private void enviarError(String idSesion, String accion, String msg) {
        DTOResponse err = new DTOResponse(accion, "error", msg, null);
        gestorClientes.enviarMensaje(idSesion, gson.toJson(err));
    }
}