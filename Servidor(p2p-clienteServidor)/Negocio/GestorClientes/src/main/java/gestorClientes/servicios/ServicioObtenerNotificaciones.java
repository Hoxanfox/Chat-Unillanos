package gestorClientes.servicios;

import com.google.gson.Gson;
import conexion.clientes.interfaces.IGestorConexionesCliente;
import conexion.clientes.interfaces.IRouterMensajesCliente;
import dto.comunicacion.DTOResponse;
import gestorClientes.interfaces.IServicioCliente;
import logger.LoggerCentral;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Servicio que maneja la acción "obtenerNotificaciones".
 *
 * NOTA: Este servidor NO implementa un sistema de notificaciones genéricas persistentes.
 * Las invitaciones a canales se manejan por separado con "obtenerInvitaciones".
 *
 * Este servicio existe únicamente para evitar errores en el cliente y devuelve
 * siempre una lista vacía de notificaciones.
 */
public class ServicioObtenerNotificaciones implements IServicioCliente {

    private static final String TAG = "ServicioObtenerNotificaciones";

    // Colores ANSI para logs
    private static final String RESET = "\u001B[0m";
    private static final String VERDE = "\u001B[32m";
    private static final String AMARILLO = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";
    private static final String AZUL = "\u001B[34m";

    private IGestorConexionesCliente gestor;
    private final Gson gson;

    public ServicioObtenerNotificaciones() {
        this.gson = new Gson();
        LoggerCentral.info(TAG, AZUL + "Constructor: ServicioObtenerNotificaciones creado" + RESET);
        LoggerCentral.warn(TAG, AMARILLO + "⚠️ NOTA: Este servidor NO tiene sistema de notificaciones genéricas" + RESET);
        LoggerCentral.info(TAG, CYAN + "ℹ️ Para invitaciones a canales, usar 'obtenerInvitaciones'" + RESET);
    }

    @Override
    public String getNombre() {
        return "ServicioObtenerNotificaciones";
    }

    @Override
    public void inicializar(IGestorConexionesCliente gestor, IRouterMensajesCliente router) {
        this.gestor = gestor;
        LoggerCentral.info(TAG, AZUL + "Inicializando ServicioObtenerNotificaciones..." + RESET);

        // ==================== RUTA: Obtener Notificaciones ====================
        router.registrarAccion("obtenerNotificaciones", (datos, idSesion) -> {
            LoggerCentral.info(TAG, CYAN + "📥 Recibida petición de obtener notificaciones" + RESET);
            LoggerCentral.info(TAG, CYAN + "ℹ️ Este servidor solo maneja invitaciones a canales via 'obtenerInvitaciones'" + RESET);

            // Devolver respuesta vacía pero válida
            // El cliente espera: { "notificaciones": [], "totalNoLeidas": 0, "totalNotificaciones": 0 }
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("notificaciones", new ArrayList<>());
            respuesta.put("totalNoLeidas", 0);
            respuesta.put("totalNotificaciones", 0);

            LoggerCentral.info(TAG, VERDE + "✅ Respuesta enviada: 0 notificaciones (correcto - usar obtenerInvitaciones para invitaciones a canales)" + RESET);

            return new DTOResponse(
                "obtenerNotificaciones",
                "success",
                "Sin notificaciones. Use 'obtenerInvitaciones' para invitaciones a canales",
                gson.toJsonTree(respuesta)
            );
        });

        LoggerCentral.info(TAG, VERDE + "✅ Servicio inicializado - Ruta 'obtenerNotificaciones' registrada" + RESET);
    }

    @Override
    public void iniciar() {
        LoggerCentral.info(TAG, "Servicio de obtener notificaciones iniciado");
    }

    @Override
    public void detener() {
        LoggerCentral.info(TAG, "Servicio de obtener notificaciones detenido");
    }
}
