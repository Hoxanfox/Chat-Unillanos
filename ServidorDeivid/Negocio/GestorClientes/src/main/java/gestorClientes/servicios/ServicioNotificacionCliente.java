package gestorClientes.servicios;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import conexion.clientes.interfaces.IGestorConexionesCliente;
import conexion.clientes.interfaces.IRouterMensajesCliente;
import gestorClientes.interfaces.IServicioCliente;
import logger.LoggerCentral;
import observador.IObservador;

/**
 * Servicio del lado CS (Cliente-Servidor).
 * Su responsabilidad es recibir avisos del sistema (ej. P2P) y
 * notificar a los clientes conectados (Apps/Web) para que se actualicen.
 */
public class ServicioNotificacionCliente implements IServicioCliente, IObservador {

    private static final String TAG = "NotificadorClientes";
    private IGestorConexionesCliente gestorClientes;
    private final Gson gson;

    public ServicioNotificacionCliente() {
        this.gson = new Gson();
    }

    @Override
    public String getNombre() { return "ServicioNotificacionCliente"; }

    @Override
    public void inicializar(IGestorConexionesCliente gestor, IRouterMensajesCliente router) {
        this.gestorClientes = gestor;
        LoggerCentral.info(TAG, "Pasarela de eventos PUSH lista.");
    }

    /**
     * Método que recibe los eventos del núcleo (P2P, DB, etc.)
     * Implementa IObservador.
     */
    @Override
    public void actualizar(String tipoEvento, Object datos) {
        // Filtramos qué eventos merecen notificar al cliente
        // Por ejemplo: NUEVO_MENSAJE, NUEVO_USUARIO, etc.
        if (tipoEvento.startsWith("NUEVO_") || tipoEvento.equals("ACTUALIZACION_ESTADO")) {
            enviarSenalDeActualizacion(tipoEvento);
        }
    }

    /**
     * Envía el PUSH ligero (Signal) a todos los sockets conectados.
     */
    private void enviarSenalDeActualizacion(String recursoAfectado) {
        if (gestorClientes == null) return;

        try {
            // Payload Ligero: { "type": "SIGNAL_UPDATE", "resource": "NUEVO_MENSAJE" }
            JsonObject signal = new JsonObject();
            signal.addProperty("type", "SIGNAL_UPDATE");
            signal.addProperty("resource", recursoAfectado);

            String jsonPush = gson.toJson(signal);

            // LoggerCentral.debug(TAG, "Enviando PUSH masivo: " + recursoAfectado);

            // Broadcast a todos los clientes conectados al puerto 8000
            gestorClientes.broadcast(jsonPush);

        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error enviando push: " + e.getMessage());
        }
    }

    @Override public void iniciar() {}
    @Override public void detener() {}
}