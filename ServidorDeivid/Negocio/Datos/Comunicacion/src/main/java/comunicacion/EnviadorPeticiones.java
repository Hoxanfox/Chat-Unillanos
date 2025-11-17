package comunicacion;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import conexion.GestorConexion;
import conexion.TipoPool;
import dto.comunicacion.DTORequest;
import dto.gestionConexion.conexion.DTOSesion;
import logger.LoggerCentral;
import transporte.FabricaTransporte;
import dto.gestionConexion.transporte.DTOConexion;

import java.io.PrintWriter;
import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;
import java.util.Map;

/**
 * Implementación del componente encargado de enviar peticiones al servidor.
 */
public class EnviadorPeticiones implements IEnviadorPeticiones {

    private final GestorConexion gestorConexion;
    private final Gson gson;

    public EnviadorPeticiones() {
        this.gestorConexion = GestorConexion.getInstancia();
        // ✅ CORRECCIÓN: Configurar Gson para serializar campos nulos
        // Esto es necesario para que peerRemitenteId y peerDestinoId se incluyan aunque sean null
        this.gson = new GsonBuilder()
                .serializeNulls()
                .create();
        // Nuevo log de depuración
        LoggerCentral.debug("EnviadorPeticiones: instancia creada. Gson serializeNulls habilitado.");
    }

    @Override
    public void enviar(DTORequest request) {
        // Comportamiento previo: por defecto usar el pool de CLIENTES
        LoggerCentral.debug("EnviadorPeticiones.enviar(request) llamado. Delegando a enviar(request, TipoPool.CLIENTES). request=" + (request != null ? gson.toJson(request) : "null"));
        enviar(request, TipoPool.CLIENTES);
    }

    /**
     * Enviar una petición usando el pool seleccionado (CLIENTES o PEERS).
     * Lanzará RuntimeException si no hay sesión disponible en el pool y no puede establecer una conexión directa.
     */
    public void enviar(DTORequest request, TipoPool tipoPool) {
        LoggerCentral.debug("EnviadorPeticiones.enviar(request, " + tipoPool + ") - preparando para obtener sesión. Request resumen=" + (request != null ? gson.toJson(request) : "null"));
        DTOSesion sesion;
        boolean creadoLocalmente = false;
        if (tipoPool == TipoPool.PEERS) {
            sesion = gestorConexion.obtenerSesionPeer(2000);
        } else {
            sesion = gestorConexion.obtenerSesionCliente(2000);
        }

        // Si no hay sesión en el pool, intentar crear conexión directa si el request contiene ip/port
        if (sesion == null || !sesion.estaActiva()) {
            LoggerCentral.debug("No hay sesión activa en pool " + tipoPool + "; intentando fallback a conexión directa si hay ip/port en el request");
            // Intentar obtener ip/port desde request.payload (antes se usaba request.getData(), que no existe)
            String ip = null;
            Integer port = null;
            try {
                Object data = request != null ? request.getPayload() : null;
                if (data != null) {
                    Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
                    Map<String, Object> map = gson.fromJson(gson.toJson(data), mapType);
                    if (map != null) {
                        if (map.containsKey("ip")) ip = String.valueOf(map.get("ip"));
                        if (map.containsKey("port")) {
                            try { port = Integer.parseInt(String.valueOf(map.get("port"))); } catch (Exception ignored) {}
                        }
                    }
                }
            } catch (Exception e) {
                LoggerCentral.debug("Error extrayendo ip/port desde request.payload: " + e.getMessage());
            }

            if (ip != null && port != null && port > 0) {
                final int maxRetries = 3;
                final long sleepMs = 500;
                for (int attempt = 1; attempt <= maxRetries; attempt++) {
                    try {
                        LoggerCentral.debug("Fallback: intento " + attempt + " de " + maxRetries + " para crear conexión directa a " + ip + ":" + port);
                        DTOConexion datos = new DTOConexion(ip, port);
                        sesion = FabricaTransporte.crearTransporte(null).conectar(datos);
                        if (sesion != null && sesion.estaActiva()) {
                            creadoLocalmente = true;
                            LoggerCentral.debug("Fallback: conexión directa creada y activa -> " + sesion);
                            // Añadir la sesión al pool correspondiente para reuso futuro
                            if (tipoPool == TipoPool.PEERS) {
                                gestorConexion.agregarSesionPeer(sesion);
                                LoggerCentral.debug("Fallback: sesión añadida al pool PEERS -> " + sesion);
                            } else {
                                gestorConexion.agregarSesionCliente(sesion);
                                LoggerCentral.debug("Fallback: sesión añadida al pool CLIENTES -> " + sesion);
                            }
                            break; // éxito
                        } else {
                            LoggerCentral.warn("Fallback: no se pudo establecer conexión directa a " + ip + ":" + port + " (intento " + attempt + ")");
                        }
                    } catch (Exception e) {
                        LoggerCentral.error("Fallback: error creando conexión directa a " + ip + ":" + port + " en intento " + attempt + " -> " + e.getMessage(), e);
                    }

                    try {
                        Thread.sleep(sleepMs * attempt); // backoff lineal
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } else {
                LoggerCentral.debug("Fallback: request no contiene ip/port válidos para conectar directamente. ip=" + ip + " port=" + port);
            }
        }

        if (sesion == null || !sesion.estaActiva()) {
            String msg = "No se puede enviar la petición, no hay sesión activa disponible en el pool " + tipoPool + ".";
            LoggerCentral.warn(msg);
            // Lanzar excepción para que el llamador pueda manejar el fallo y no asumir que se envió
            throw new RuntimeException(msg);
        }

        try {
            LoggerCentral.debug("Sesión obtenida desde pool " + tipoPool + ": " + sesion);
            PrintWriter out = sesion.getOut();
            String jsonRequest = gson.toJson(request);
            int len = jsonRequest != null ? jsonRequest.length() : 0;
            LoggerCentral.debug("Serializado request a JSON (longitud=" + len + ")");
            out.println(jsonRequest);
            out.flush(); // asegurar que el mensaje se envía inmediatamente
            String toLog = jsonRequest != null ? (len > 1000 ? jsonRequest.substring(0, 1000) + "... [truncado]" : jsonRequest) : "null";
            LoggerCentral.info("[" + tipoPool + "] >> Petición enviada: " + toLog);
            LoggerCentral.debug("EnviadorPeticiones: envío completado correctamente y flush realizado.");
        } catch (Exception e) {
            // Es una buena práctica verificar si el socket se cerró para dar un mensaje más útil.
            try {
                if (sesion.getSocket() != null && sesion.getSocket().isClosed()) {
                    LoggerCentral.error("Error al enviar la petición: la conexión parece estar cerrada.");
                } else {
                    LoggerCentral.error("Error al enviar la petición: " + e.getMessage(), e);
                }
            } catch (Exception ex) {
                LoggerCentral.error("Error al comprobar el estado de la sesión: " + ex.getMessage(), ex);
            }
            // Re-lanzar para que el llamador pueda gestionarlo
            throw new RuntimeException("Error enviando petición: " + e.getMessage(), e);
        } finally {
            // Liberar la sesión en el pool correspondiente
            if (tipoPool == TipoPool.PEERS) {
                gestorConexion.liberarSesionPeer(sesion);
                LoggerCentral.debug("Sesión liberada al pool PEERS: " + sesion + (creadoLocalmente?" (creadaLocalmente)":""));
            } else {
                gestorConexion.liberarSesionCliente(sesion);
                LoggerCentral.debug("Sesión liberada al pool CLIENTES: " + sesion + (creadoLocalmente?" (creadaLocalmente)":""));
            }
        }
    }

    /**
     * Enviar una petición específicamente a la sesión que coincida con la IP y puerto indicados (si existe en el pool).
     * Devuelve true si se envió correctamente, false en caso de timeout o error.
     */
    public boolean enviarA(String ip, int port, DTORequest request, TipoPool tipoPool) {
        LoggerCentral.debug("EnviadorPeticiones.enviarA(ip=" + ip + ", port=" + port + ", pool=" + tipoPool + ") - buscando sesión específica. Request=" + (request != null ? gson.toJson(request) : "null"));
        DTOSesion sesion = gestorConexion.obtenerSesionPorDireccion(ip, port, 2000, tipoPool == TipoPool.PEERS);
        if (sesion == null) {
            LoggerCentral.warn("No se encontró sesión para " + ip + ":" + port + " en pool " + tipoPool + ".");
            return false;
        }

        try {
            if (!sesion.estaActiva()) {
                LoggerCentral.warn("La sesión encontrada ya no está activa para " + ip + ":" + port + ".");
                return false;
            }
            LoggerCentral.debug("Sesión específica obtenida: " + sesion);
            PrintWriter out = sesion.getOut();
            String jsonRequest = gson.toJson(request);
            int len = jsonRequest != null ? jsonRequest.length() : 0;
            String toLog = jsonRequest != null ? (len > 1000 ? jsonRequest.substring(0, 1000) + "... [truncado]" : jsonRequest) : "null";
            out.println(jsonRequest);
            out.flush();
            LoggerCentral.info("[" + tipoPool + "] >> Petición enviada a " + ip + ":" + port + " -> " + toLog);
            LoggerCentral.debug("EnviadorPeticiones.enviarA: envío OK a " + ip + ":" + port);
            return true;
        } catch (Exception e) {
            LoggerCentral.error("Error enviando petición a " + ip + ":" + port + ": " + e.getMessage(), e);
            return false;
        } finally {
            // Liberar la sesión de vuelta al pool
            if (tipoPool == TipoPool.PEERS) {
                gestorConexion.liberarSesionPeer(sesion);
                LoggerCentral.debug("Sesión liberada al pool PEERS (enviarA): " + sesion);
            } else {
                gestorConexion.liberarSesionCliente(sesion);
                LoggerCentral.debug("Sesión liberada al pool CLIENTES (enviarA): " + sesion);
            }
        }
    }
}
