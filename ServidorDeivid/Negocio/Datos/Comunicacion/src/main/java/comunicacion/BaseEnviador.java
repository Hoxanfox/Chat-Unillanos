package comunicacion;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import conexion.GestorConexion;
import conexion.enums.TipoPool;
import dto.comunicacion.DTORequest;
import dto.comunicacion.DTOResponse;
import dto.gestionConexion.conexion.DTOSesion;
import logger.LoggerCentral;
import transporte.FabricaTransporte;
import dto.gestionConexion.transporte.DTOConexion;

import java.io.PrintWriter;
import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;
import java.util.Map;

/**
 * Clase base que contiene la lógica común para enviar peticiones/respuestas.
 */
public abstract class BaseEnviador implements IEnviadorPeticiones {

    protected final GestorConexion gestorConexion;
    protected final Gson gson;

    protected BaseEnviador() {
        this.gestorConexion = GestorConexion.getInstancia();
        this.gson = new GsonBuilder().serializeNulls().create();
        LoggerCentral.debug(this.getClass().getSimpleName() + ": instancia creada. Gson serializeNulls habilitado.");
    }

    // Implementación por defecto delega al pool específico de cada subclase
    @Override
    public void enviar(DTORequest request) {
        enviar(request, getDefaultPool());
    }

    // Cada subclase define su pool por defecto
    protected abstract TipoPool getDefaultPool();

    @Override
    public void enviar(DTORequest request, TipoPool tipoPool) {
        LoggerCentral.debug(this.getClass().getSimpleName() + ".enviar(request, " + tipoPool + ") - preparando para obtener sesión. Request resumen=" + (request != null ? gson.toJson(request) : "null"));
        DTOSesion sesion = null;
        boolean creadoLocalmente = false;

        // Intentar obtener ip/port objetivo desde el payload para preferir sesión por dirección
        String targetIp = null;
        Integer targetPort = null;
        try {
            Object data = request != null ? request.getPayload() : null;
            if (data != null) {
                Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
                Map<String, Object> map = gson.fromJson(gson.toJson(data), mapType);
                if (map != null) {
                    // Priorizar targetSocketInfo si está presente (formato "ip:port")
                    if (map.containsKey("targetSocketInfo")) {
                        try {
                            String socketInfo = String.valueOf(map.get("targetSocketInfo"));
                            if (socketInfo != null) {
                                String[] parts = socketInfo.split(":" );
                                if (parts.length >= 2) {
                                    targetIp = parts[0];
                                    try { targetPort = Integer.parseInt(parts[1]); } catch (Exception ignored) {}
                                }
                            }
                        } catch (Exception e) { LoggerCentral.debug("Error parseando targetSocketInfo del payload: " + e.getMessage()); }
                    }

                    // Si no se obtuvo targetSocketInfo, usar ip/port explícitos
                    if ((targetIp == null || targetPort == null)) {
                        if (map.containsKey("ip")) targetIp = String.valueOf(map.get("ip"));
                        if (map.containsKey("port")) {
                            try { targetPort = Integer.parseInt(String.valueOf(map.get("port"))); } catch (Exception ignored) {}
                        }
                    }
                }
            }
        } catch (Exception e) {
            LoggerCentral.debug("Error extrayendo ip/port desde request.payload: " + e.getMessage());
        }

        // Si conocemos destino, intentar obtener sesión específica primero
        if (targetIp != null && targetPort != null && targetPort > 0) {
            try {
                sesion = gestorConexion.obtenerSesionPorDireccion(targetIp, targetPort, 2000, tipoPool == TipoPool.PEERS);
                if (sesion != null && sesion.estaActiva()) {
                    LoggerCentral.debug("enviar: sesión específica obtenida para destino " + targetIp + ":" + targetPort + " -> " + sesion);
                } else {
                    sesion = null; // asegurarnos
                }
            } catch (Exception e) {
                LoggerCentral.debug("enviar: error obteniendo sesión por dirección -> " + e.getMessage());
                sesion = null;
            }
        }

        // Si no obtuvimos sesión específica, obtener cualquiera del pool como antes
        if (sesion == null) {
            if (tipoPool == TipoPool.PEERS) {
                sesion = gestorConexion.obtenerSesionPeer(2000);
            } else {
                sesion = gestorConexion.obtenerSesionCliente(2000);
            }
        }

        // Si no hay sesión en el pool, intentar crear conexión directa si el request contiene ip/port
        if (sesion == null || !sesion.estaActiva()) {
            LoggerCentral.debug("No hay sesión activa en pool " + tipoPool + "; intentando fallback a conexión directa si hay ip/port en el request");
            String ip = null;
            Integer port = null;
            try {
                Object data = request != null ? request.getPayload() : null;
                if (data != null) {
                    Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
                    Map<String, Object> map = gson.fromJson(gson.toJson(data), mapType);
                    if (map != null) {
                        // Priorizar targetSocketInfo para fallback destino
                        if (map.containsKey("targetSocketInfo")) {
                            try {
                                String socketInfo = String.valueOf(map.get("targetSocketInfo"));
                                if (socketInfo != null) {
                                    String[] parts = socketInfo.split(":" );
                                    if (parts.length >= 2) {
                                        if (ip == null || ip.isEmpty()) ip = parts[0];
                                        if (port == null) {
                                            try { port = Integer.parseInt(parts[1]); } catch (Exception ignored) {}
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                LoggerCentral.debug("Error parseando targetSocketInfo del payload (fallback): " + e.getMessage());
                            }
                        }

                        // Si no hay targetSocketInfo, usar ip/port explícitos
                        if ((ip == null || port == null)) {
                            if (map.containsKey("ip")) ip = String.valueOf(map.get("ip"));
                            if (map.containsKey("port")) {
                                try { port = Integer.parseInt(String.valueOf(map.get("port"))); } catch (Exception ignored) {}
                            }
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
                        sesion = FabricaTransporte.crearTransporte("TCP").conectar(datos);
                        if (sesion != null && sesion.estaActiva()) {
                            creadoLocalmente = true;
                            LoggerCentral.debug("Fallback: conexión directa creada y activa -> " + sesion);
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
            LoggerCentral.debug(this.getClass().getSimpleName() + ": envío completado correctamente y flush realizado.");
        } catch (Exception e) {
            try {
                if (sesion.getSocket() != null && sesion.getSocket().isClosed()) {
                    LoggerCentral.error("Error al enviar la petición: la conexión parece estar cerrada.");
                } else {
                    LoggerCentral.error("Error al enviar la petición: " + e.getMessage(), e);
                }
            } catch (Exception ex) {
                LoggerCentral.error("Error al comprobar el estado de la sesión: " + ex.getMessage(), ex);
            }
            throw new RuntimeException("Error enviando petición: " + e.getMessage(), e);
        } finally {
            if (tipoPool == TipoPool.PEERS) {
                gestorConexion.liberarSesionPeer(sesion);
                LoggerCentral.debug("Sesión liberada al pool PEERS: " + sesion + ((creadoLocalmente)?" (creadaLocalmente)":""));
            } else {
                gestorConexion.liberarSesionCliente(sesion);
                LoggerCentral.debug("Sesión liberada al pool CLIENTES: " + sesion + ((creadoLocalmente)?" (creadaLocalmente)":""));
            }
        }
    }

    @Override
    public boolean enviarA(String ip, int port, DTORequest request, TipoPool tipoPool) {
        LoggerCentral.debug(this.getClass().getSimpleName() + ".enviarA(ip=" + ip + ", port=" + port + ", pool=" + tipoPool + ") - buscando sesión específica. Request=" + (request != null ? gson.toJson(request) : "null"));
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
            LoggerCentral.debug(this.getClass().getSimpleName() + ".enviarA: envío OK a " + ip + ":" + port);
            return true;
        } catch (Exception e) {
            LoggerCentral.error("Error enviando petición a " + ip + ":" + port + ": " + e.getMessage(), e);
            return false;
        } finally {
            if (tipoPool == TipoPool.PEERS) {
                gestorConexion.liberarSesionPeer(sesion);
                LoggerCentral.debug("Sesión liberada al pool PEERS (enviarA): " + sesion);
            } else {
                gestorConexion.liberarSesionCliente(sesion);
                LoggerCentral.debug("Sesión liberada al pool CLIENTES (enviarA): " + sesion);
            }
        }
    }

    @Override
    public boolean enviarResponseA(String ip, int port, DTOResponse response, TipoPool tipoPool) {
        LoggerCentral.debug(this.getClass().getSimpleName() + ".enviarResponseA(ip=" + ip + ", port=" + port + ", pool=" + tipoPool + ") - preparando envío de DTOResponse");

        // Si el gestor de respuestas tiene una sesión actual asociada al hilo lector, intentar usarla directamente
        try {
            DTOSesion sesionActual = GestorRespuesta.obtenerSesionActual();
            if (sesionActual != null && sesionActual.estaActiva()) {
                try {
                    // Comprobar IP de la sesión (no comparar puerto remoto, ya que puede ser efímero)
                    String remoteHost = null;
                    if (sesionActual.getSocket() != null && sesionActual.getSocket().getInetAddress() != null) {
                        remoteHost = sesionActual.getSocket().getInetAddress().getHostAddress();
                    }
                    if (remoteHost != null && remoteHost.equals(ip)) {
                        LoggerCentral.debug("enviarResponseA: usando sesionActual del hilo lector para enviar response a " + ip + ":" + port + " -> " + sesionActual);
                        try {
                            java.io.PrintWriter out = sesionActual.getOut();
                            String jsonResponse = gson.toJson(response);
                            out.println(jsonResponse);
                            out.flush();
                            LoggerCentral.info("[" + tipoPool + "] >> Response enviada (sesionActual) a " + ip + ":" + port + " -> " + (jsonResponse!=null? (jsonResponse.length()>1000?jsonResponse.substring(0,1000)+"... [truncado]":jsonResponse):"null"));
                            return true;
                        } catch (Exception e) {
                            LoggerCentral.error("enviarResponseA: error enviando sobre sesionActual -> " + e.getMessage(), e);
                            // seguir al path normal (buscar en pool)
                        }
                    }
                } catch (Throwable t) {
                    LoggerCentral.debug("enviarResponseA: no se pudo usar sesionActual -> " + t.getMessage());
                }
            }
        } catch (Throwable ignored) {}

        DTOSesion sesion = gestorConexion.obtenerSesionPorDireccion(ip, port, 2000, tipoPool == TipoPool.PEERS);
        if (sesion == null) {
            LoggerCentral.warn("No se encontró sesión para " + ip + ":" + port + " en pool " + tipoPool + ". No se puede enviar response.");
            return false;
        }

        try {
            if (!sesion.estaActiva()) {
                LoggerCentral.warn("La sesión encontrada ya no está activa para " + ip + ":" + port + ".");
                return false;
            }
            LoggerCentral.debug("Sesión específica obtenida para response: " + sesion);
            PrintWriter out = sesion.getOut();
            String jsonResponse = gson.toJson(response);
            int len = jsonResponse != null ? jsonResponse.length() : 0;
            String toLog = jsonResponse != null ? (len > 1000 ? jsonResponse.substring(0, 1000) + "... [truncado]" : jsonResponse) : "null";
            out.println(jsonResponse);
            out.flush();
            LoggerCentral.info("[" + tipoPool + "] >> Response enviada a " + ip + ":" + port + " -> " + toLog);
            return true;
        } catch (Exception e) {
            LoggerCentral.error("Error enviando response a " + ip + ":" + port + ": " + e.getMessage(), e);
            return false;
        } finally {
            if (tipoPool == TipoPool.PEERS) {
                gestorConexion.liberarSesionPeer(sesion);
                LoggerCentral.debug("Sesión liberada al pool PEERS (enviarResponseA): " + sesion);
            } else {
                gestorConexion.liberarSesionCliente(sesion);
                LoggerCentral.debug("Sesión liberada al pool CLIENTES (enviarResponseA): " + sesion);
            }
        }
    }

}
