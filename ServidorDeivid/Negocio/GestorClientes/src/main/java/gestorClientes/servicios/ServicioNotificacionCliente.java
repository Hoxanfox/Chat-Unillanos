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

    // --- COLORES ANSI ---
    private static final String RESET = "\u001B[0m";
    private static final String AZUL = "\u001B[34m";
    private static final String VERDE = "\u001B[32m";
    private static final String AMARILLO = "\u001B[33m";

    private IGestorConexionesCliente gestorClientes;
    private final Gson gson;

    public ServicioNotificacionCliente() {
        this.gson = new Gson();
        LoggerCentral.info(TAG, VERDE + "✅ ServicioNotificacionCliente creado" + RESET);
    }

    @Override
    public String getNombre() { return "ServicioNotificacionCliente"; }

    @Override
    public void inicializar(IGestorConexionesCliente gestor, IRouterMensajesCliente router) {
        this.gestorClientes = gestor;
        LoggerCentral.info(TAG, VERDE + "✅ Pasarela de eventos PUSH lista." + RESET);
    }

    /**
     * Método que recibe los eventos del núcleo (P2P, DB, etc.)
     * Implementa IObservador.
     *
     * Envía SIGNAL_UPDATE a todos los clientes para que actualicen todo:
     * contactos, canales, mensajes, etc.
     */
    @Override
    public void actualizar(String tipoEvento, Object datos) {
        LoggerCentral.info(TAG, AZUL + "📥 Evento recibido: " + tipoEvento + " | Datos: " + datos + RESET);

        // ✅ AJUSTE: Siempre notificar a los clientes cuando termine la sincronización P2P
        if ("SINCRONIZACION_P2P_TERMINADA".equals(tipoEvento)) {
            boolean huboCambios = datos instanceof Boolean ? (Boolean) datos : false;

            if (huboCambios) {
                LoggerCentral.info(TAG, VERDE + "🔄 Sincronización P2P completada CON cambios. Notificando clientes..." + RESET);
            } else {
                LoggerCentral.info(TAG, AMARILLO + "🔄 Sincronización P2P completada SIN cambios. Notificando clientes igualmente..." + RESET);
            }

            // Siempre enviar señal de actualización específica de P2P
            enviarSenalDeActualizacion("SYNC_P2P_UPDATE");
            return;
        }

        // ✅ MEJORADO: Manejar evento de sincronización terminada
        if ("SINCRONIZACION_TERMINADA".equals(tipoEvento)) {
            boolean huboCambios = datos instanceof Boolean ? (Boolean) datos : false;

            // SIEMPRE notificar cuando termine la sincronización
            LoggerCentral.info(TAG, VERDE + "🔄 Sincronización terminada. Notificando clientes... (cambios: " + huboCambios + ")" + RESET);
            enviarSenalDeActualizacion("SYNC_UPDATE");
            return;
        }

        // Filtramos qué eventos merecen notificar al cliente
        // Por ejemplo: NUEVO_MENSAJE, NUEVO_USUARIO, etc.
        if (tipoEvento.startsWith("NUEVO_") || tipoEvento.equals("ACTUALIZACION_ESTADO")) {
            LoggerCentral.info(TAG, AZUL + "📡 Evento requiere notificación: " + tipoEvento + RESET);
            enviarSenalDeActualizacion(tipoEvento);
        } else {
            LoggerCentral.debug(TAG, "Evento " + tipoEvento + " no requiere notificación a clientes");
        }
    }

    /**
     * Envía el PUSH ligero (Signal) a todos los sockets conectados.
     * El cliente recibe esto y actualiza toda su información: contactos, canales, mensajes.
     */
    private void enviarSenalDeActualizacion(String recursoAfectado) {
        if (gestorClientes == null) {
            LoggerCentral.warn(TAG, AMARILLO + "⚠️ GestorClientes no disponible. No se puede enviar notificación." + RESET);
            return;
        }

        try {
            // Payload Ligero: { "type": "SIGNAL_UPDATE", "resource": "NUEVO_MENSAJE" }
            JsonObject signal = new JsonObject();
            signal.addProperty("type", "SIGNAL_UPDATE");
            signal.addProperty("resource", recursoAfectado);

            String jsonPush = gson.toJson(signal);

            LoggerCentral.info(TAG, VERDE + "📡 Enviando SIGNAL_UPDATE a todos los clientes: " + recursoAfectado + RESET);

            // Broadcast a todos los clientes conectados
            gestorClientes.broadcast(jsonPush);

            LoggerCentral.info(TAG, VERDE + "✅ SIGNAL_UPDATE enviado a todos los clientes" + RESET);

        } catch (Exception e) {
            LoggerCentral.error(TAG, "❌ Error enviando push: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override public void iniciar() {
        LoggerCentral.info(TAG, VERDE + "🚀 ServicioNotificacionCliente iniciado" + RESET);
    }

    @Override public void detener() {
        LoggerCentral.info(TAG, "⏹️ ServicioNotificacionCliente detenido");
    }
}
