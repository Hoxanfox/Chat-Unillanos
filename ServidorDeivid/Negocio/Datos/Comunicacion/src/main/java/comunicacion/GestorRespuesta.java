package comunicacion;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import conexion.GestorConexion;
import conexion.enums.TipoPool;
import dto.comunicacion.DTOResponse;
import dto.gestionConexion.conexion.DTOSesion;
import logger.LoggerCentral;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import com.google.gson.reflect.TypeToken;

/**
 * Implementación del componente que escucha y gestiona las respuestas del servidor.
 * AHORA implementado de forma que puede ser subclasificado para crear gestores por pool (PEERS/CLIENTES).
 */
public class GestorRespuesta implements IGestorRespuesta {

    private static GestorRespuesta instancia; // Instancia única del Singleton por compatibilidad
    protected final GestorConexion gestorConexion;
    protected final Map<String, Consumer<DTOResponse>> manejadores;
    protected final Gson gson;
    private Thread hiloEscucha;

    // ThreadLocal que contiene la sesion actualmente manejada por el hilo lector;
    // permite a otros componentes (ej. BaseEnviador) reutilizar exactamente la misma sesion
    // para enviar respuestas sin extraerla del pool.
    private static final ThreadLocal<DTOSesion> sesionActual = new ThreadLocal<>();

    // Exponer getters/setters package-private para que BaseEnviador pueda leerlo
    public static DTOSesion obtenerSesionActual() { return sesionActual.get(); }
    static void establecerSesionActual(DTOSesion s) { sesionActual.set(s); }
    static void limpiarSesionActual() { sesionActual.remove(); }

    // El constructor ahora es protected para permitir subclases con sus propias tablas de manejadores
    protected GestorRespuesta() {
        this.gestorConexion = GestorConexion.getInstancia();
        this.manejadores = new ConcurrentHashMap<>();
        this.gson = new Gson();
        LoggerCentral.debug("GestorRespuesta: instancia creada.");
        // NOTA: GestorRespuesta no debe contener lógica de negocio (no registrar manejadores específicos).
        // Los manejadores deben ser registrados por la capa de negocio (por ejemplo GestorP2PImpl).
    }

    /**
     * Método estático para obtener la única instancia de la clase.
     * @return la instancia del GestorRespuesta.
     */
    public static synchronized GestorRespuesta getInstancia() {
        if (instancia == null) {
            instancia = new GestorRespuesta();
        }
        return instancia;
    }

    @Override
    public void iniciarEscucha() {
        // Comportamiento previo: por defecto escuchar en el pool de CLIENTES
        // Por defecto en este módulo P2P escuchamos PEERS
        iniciarEscucha(TipoPool.PEERS);
    }

    /**
     * Inicia la escucha de respuestas usando el pool indicado (CLIENTES o PEERS).
     */
    public void iniciarEscucha(TipoPool tipoPool) {
        if (hiloEscucha != null && hiloEscucha.isAlive()) {
            LoggerCentral.info("El gestor de respuestas ya está escuchando.");
            return;
        }

        LoggerCentral.debug("GestorRespuesta.iniciarEscucha(pool=" + tipoPool + ") - iniciando hilo de escucha. Manejadores registrados=" + manejadores.size());

        hiloEscucha = new Thread(() -> {
            // backoff y control de logs para reintentos cuando no hay sesiones disponibles
            long backoffMs = 1000;
            final long maxBackoffMs = 10000;
            int retryCount = 0;
            long lastLogMs = 0;

            while (!Thread.currentThread().isInterrupted()) {
                // Obtener una sesión del pool seleccionado (reintentar hasta que haya una)
                DTOSesion sesion = null;
                try {
                    if (tipoPool == TipoPool.PEERS) {
                        sesion = gestorConexion.obtenerSesionPeer(5000);
                    } else {
                        sesion = gestorConexion.obtenerSesionCliente(5000);
                    }
                } catch (Exception e) {
                    LoggerCentral.error("GestorRespuesta: error obteniendo sesion del pool " + tipoPool + ": " + e.getMessage(), e);
                }

                if (sesion == null || !sesion.estaActiva()) {
                    // Control de logs: warn la primera vez, luego informar cada 5s para evitar spam en el log
                    retryCount++;
                    long now = System.currentTimeMillis();
                    if (retryCount == 1) {
                        LoggerCentral.warn("No se puede iniciar la escucha en pool " + tipoPool + ": no hay sesión activa disponible. Reintentando en " + (backoffMs / 1000) + "s...");
                    } else if (now - lastLogMs >= 5000) {
                        LoggerCentral.info("Aún esperando sesiones en pool " + tipoPool + " (reintentos=" + retryCount + "). Próximo intento en " + (backoffMs / 1000) + "s...");
                        lastLogMs = now;
                    } else {
                        LoggerCentral.debug("GestorRespuesta: sin sesión disponible en pool " + tipoPool + " (reintentos=" + retryCount + ")");
                    }

                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }

                    // aumentar backoff de forma exponencial hasta un máximo
                    backoffMs = Math.min(maxBackoffMs, backoffMs * 2);
                    continue; // reintentar obtener sesión
                } else {
                    // sesión obtenida: resetear contador/backoff
                    retryCount = 0;
                    backoffMs = 1000;
                    lastLogMs = 0;
                }

                LoggerCentral.debug("GestorRespuesta: sesión obtenida para escucha en pool " + tipoPool + " -> " + sesion);

                // Intentar marcar esta sesión para este lector; si ya tiene lector activo, reinsertarla y continuar
                boolean marcado = false;
                try {
                    marcado = sesion.intentarAsignarLector();
                } catch (Exception e) {
                    LoggerCentral.debug("GestorRespuesta: no se pudo marcar sesion con lector -> " + e.getMessage());
                    marcado = false;
                }

                if (!marcado) {
                    LoggerCentral.debug("GestorRespuesta: la sesión ya tiene un lector activo, reinsertando y buscando otra -> " + sesion);
                    try {
                        if (tipoPool == TipoPool.PEERS) gestorConexion.liberarSesionPeer(sesion);
                        else gestorConexion.liberarSesionCliente(sesion);
                    } catch (Exception ex) {
                        LoggerCentral.error("Error reinsertando sesión en pool cuando ya tiene lector: " + ex.getMessage(), ex);
                    }
                    // pequeña espera para evitar tight-loop
                    try { Thread.sleep(50); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                    continue; // volver a pedir otra sesión
                }

                try (BufferedReader in = sesion.getIn()) {
                    LoggerCentral.info("Gestor de respuestas iniciado en pool " + tipoPool + ". Esperando mensajes...");
                    String respuestaServidor;
                    while (!Thread.currentThread().isInterrupted() && (respuestaServidor = in.readLine()) != null) {
                        // Log del JSON crudo recibido (truncado si es muy largo)
                        String rawForLog = respuestaServidor.length() > 2000 ? respuestaServidor.substring(0, 2000) + "... [truncado]" : respuestaServidor;
                        LoggerCentral.debug("[" + tipoPool + "] << RAW respuesta recibida (len=" + respuestaServidor.length() + "): " + rawForLog);

                        // Truncar respuestas muy largas para evitar imprimir imágenes en base64
                        String respuestaParaLog = truncarRespuesta(respuestaServidor, 500);
                        LoggerCentral.info("[" + tipoPool + "] << Respuesta recibida: " + respuestaParaLog);
                        // Establecer la sesión actual para que los enviadores puedan acceder a ella
                        establecerSesionActual(sesion);
                        try {
                            procesarRespuesta(respuestaServidor);
                        } finally {
                            limpiarSesionActual();
                        }
                    }
                } catch (IOException e) {
                    if (!Thread.currentThread().isInterrupted()) {
                        LoggerCentral.error("La conexión con el servidor se ha perdido en pool " + tipoPool + ": " + e.getMessage(), e);
                    }
                } finally {
                    // liberar la marca de lector en la sesión
                    try {
                        sesion.liberarLector();
                    } catch (Exception ignore) {}
                    // Liberar la sesión de vuelta al pool correspondiente
                    try {
                        if (tipoPool == TipoPool.PEERS) gestorConexion.liberarSesionPeer(sesion);
                        else gestorConexion.liberarSesionCliente(sesion);
                    } catch (Exception ex) {
                        LoggerCentral.error("Error liberando sesión en pool " + tipoPool + ": " + ex.getMessage(), ex);
                    }
                    LoggerCentral.info("El gestor de respuestas ha dejado de escuchar en pool " + tipoPool + ". Volviendo a esperar nuevas sesiones...");
                }
            }
        }, "Hilo-Escucha-Respuestas-" + (tipoPool != null ? tipoPool : TipoPool.CLIENTES));
        hiloEscucha.setDaemon(true);
        hiloEscucha.start();
    }

    /**
     * Inicia un lector dedicado para la sesión proporcionada. Útil para sesiones entrantes
     * aceptadas por el servidor (TransporteServidor). La sesión se liberará al finalizar.
     */
    public void escucharSesionDirecta(DTOSesion sesion, TipoPool tipoPool) {
        Thread hilo = new Thread(() -> {
            if (sesion == null || !sesion.estaActiva()) {
                LoggerCentral.debug("escucharSesionDirecta: sesión nula/inactiva, no se inicia lector directo -> " + sesion);
                return;
            }

            // Intentar asignar lector para evitar duplicar lectores
            if (!sesion.intentarAsignarLector()) {
                LoggerCentral.debug("escucharSesionDirecta: sesión ya tiene lector activo, omitiendo iniciar lector directo -> " + sesion);
                return;
            }

            LoggerCentral.info("GestorRespuesta: iniciando escucha directa en sesión " + sesion + " pool=" + tipoPool);
            try (BufferedReader in = sesion.getIn()) {
                String respuestaServidor;
                while (!Thread.currentThread().isInterrupted() && sesion.estaActiva() && (respuestaServidor = in.readLine()) != null) {
                    String rawForLog = respuestaServidor.length() > 2000 ? respuestaServidor.substring(0, 2000) + "... [truncado]" : respuestaServidor;
                    LoggerCentral.debug("[" + tipoPool + "] << RAW respuesta recibida (len=" + respuestaServidor.length() + "): " + rawForLog);
                    String respuestaParaLog = truncarRespuesta(respuestaServidor, 500);
                    LoggerCentral.info("[" + tipoPool + "] << Respuesta recibida: " + respuestaParaLog);
                    establecerSesionActual(sesion);
                    try {
                        procesarRespuesta(respuestaServidor);
                    } finally {
                        limpiarSesionActual();
                    }
                }
            } catch (IOException e) {
                if (!Thread.currentThread().isInterrupted()) {
                    LoggerCentral.error("La conexión de sesión directa se ha perdido en pool " + tipoPool + ": " + e.getMessage(), e);
                }
            } finally {
                // liberar la marca de lector
                try { sesion.liberarLector(); } catch (Exception ignore) {}
                try {
                    if (tipoPool == TipoPool.PEERS) gestorConexion.liberarSesionPeer(sesion);
                    else gestorConexion.liberarSesionCliente(sesion);
                } catch (Exception ex) {
                    LoggerCentral.error("Error liberando sesión directa en pool " + tipoPool + ": " + ex.getMessage(), ex);
                }
                LoggerCentral.info("GestorRespuesta: lector de sesión directa finalizado para pool " + tipoPool + ".");
            }
        }, "GestorRespuesta-Sesion-" + (sesion.getSocket() != null ? sesion.getSocket().getRemoteSocketAddress() : sesion.hashCode()));
        hilo.setDaemon(true);
        hilo.start();
    }

    // Método simple para truncar respuestas para logging (evita mostrar payloads muy largos)
    private String truncarRespuesta(String respuesta, int maxLength) {
        if (respuesta == null) return null;
        try {
            if (respuesta.length() <= maxLength) return respuesta;
            return respuesta.substring(0, maxLength) + "... [mensaje truncado]";
        } catch (Exception e) {
            return respuesta;
        }
    }

    @Override
    public void detenerEscucha() {
        LoggerCentral.info("Deteniendo gestor de respuestas...");
        if (hiloEscucha != null) {
            try {
                hiloEscucha.interrupt();
                try {
                    hiloEscucha.join(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } catch (Exception e) {
                LoggerCentral.error("Error al detener el hilo de escucha: " + e.getMessage(), e);
            } finally {
                hiloEscucha = null;
            }
        } else {
            LoggerCentral.debug("detenerEscucha: no hay hilo de escucha activo.");
        }
    }

    @Override
    public void registrarManejador(String tipoOperacion, Consumer<DTOResponse> manejador) {
        if (tipoOperacion == null || manejador == null) {
            LoggerCentral.warn("registrarManejador: tipoOperacion o manejador nulo, operación ignorada.");
            return;
        }
        manejadores.put(tipoOperacion, manejador);
        LoggerCentral.debug("registrarManejador: manejador registrado para llave='" + tipoOperacion + "'. Total manejadores=" + manejadores.size());
    }

    @Override
    public void removerManejador(String tipoOperacion) {
        if (tipoOperacion == null) {
            LoggerCentral.warn("removerManejador: tipoOperacion nulo, operación ignorada.");
            return;
        }
        Consumer<DTOResponse> removed = manejadores.remove(tipoOperacion);
        if (removed != null) {
            LoggerCentral.debug("removerManejador: manejador eliminado para llave='" + tipoOperacion + "'. Total manejadores=" + manejadores.size());
        } else {
            LoggerCentral.debug("removerManejador: no existía manejador para llave='" + tipoOperacion + "'.");
        }
    }

    private void procesarRespuesta(String jsonResponse) {
        LoggerCentral.debug("procesarRespuesta: entrada json length=" + (jsonResponse != null ? jsonResponse.length() : 0));
        try {
            DTOResponse response = gson.fromJson(jsonResponse, DTOResponse.class);
            LoggerCentral.debug("procesarRespuesta: parsed DTOResponse=" + (response != null ? response.getAction() + " (action)" : "null"));
            if (response != null && response.getAction() != null) {
                // Intentar extraer requestId desde response.data si existe
                String requestId = null;
                Object dataObj = response.getData();
                if (dataObj != null) {
                    try {
                        Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
                        Map<String, Object> map = gson.fromJson(gson.toJson(dataObj), mapType);
                        LoggerCentral.debug("procesarRespuesta: data convertido a map, keys=" + (map != null ? map.keySet() : "null"));
                        if (map != null && map.containsKey("requestId")) {
                            requestId = String.valueOf(map.get("requestId"));
                            LoggerCentral.debug("procesarRespuesta: requestId extraido=" + requestId);
                        }
                    } catch (Exception ignored) {
                        LoggerCentral.debug("procesarRespuesta: data no es un map, omitiendo extracción de requestId.");
                    }
                }

                // Buscar manejador por clave específica action:requestId (si requestId existe)
                Consumer<DTOResponse> manejador = null;
                if (requestId != null && !requestId.isEmpty()) {
                    String llaveEspecifica = response.getAction() + ":" + requestId;
                    LoggerCentral.debug("procesarRespuesta: buscando manejador específico llave=" + llaveEspecifica);
                    manejador = manejadores.get(llaveEspecifica);
                    if (manejador != null) {
                        try {
                            LoggerCentral.debug("procesarRespuesta: ejecutando manejador específico para " + llaveEspecifica);
                            manejador.accept(response);
                        } catch (Exception e) {
                            LoggerCentral.error("Error ejecutando manejador específico para accion " + llaveEspecifica + ": " + e.getMessage(), e);
                        }
                        return; // ya procesado
                    }
                }

                // Normalizar la acción a minúsculas para comparación case-insensitive
                String actionNormalizada = response.getAction().toLowerCase();

                // Buscar manejador con la acción original o normalizada
                manejador = manejadores.get(response.getAction());
                if (manejador == null) {
                    LoggerCentral.debug("procesarRespuesta: buscando manejador por comparacion case-insensitive para action=" + response.getAction());
                    for (Map.Entry<String, Consumer<DTOResponse>> entry : manejadores.entrySet()) {
                        if (entry.getKey() != null && entry.getKey().toLowerCase().equals(actionNormalizada)) {
                            manejador = entry.getValue();
                            LoggerCentral.debug("procesarRespuesta: encontrado manejador por llave=" + entry.getKey());
                            break;
                        }
                    }
                }

                if (manejador != null) {
                    try {
                        LoggerCentral.debug("procesarRespuesta: ejecutando manejador para accion=" + response.getAction());
                        manejador.accept(response);
                    } catch (Exception e) {
                        LoggerCentral.error("Error ejecutando manejador para accion " + response.getAction() + ": " + e.getMessage(), e);
                    }
                } else {
                    LoggerCentral.warn("No se encontró un manejador para la acción: " + response.getAction() + ". Manejadores registrados=" + manejadores.keySet());
                }
            }
        } catch (JsonSyntaxException e) {
            LoggerCentral.error("Error al parsear la respuesta JSON: " + jsonResponse + " - " + e.getMessage(), e);
        }
    }

}
