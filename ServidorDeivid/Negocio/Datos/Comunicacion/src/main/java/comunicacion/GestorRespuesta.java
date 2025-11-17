package comunicacion;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import conexion.GestorConexion;
import conexion.TipoPool;
import dto.comunicacion.DTOResponse;
import dto.gestionConexion.conexion.DTOSesion;
import logger.LoggerCentral;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

/**
 * Implementación del componente que escucha y gestiona las respuestas del servidor.
 * AHORA implementado como un Singleton.
 */
public class GestorRespuesta implements IGestorRespuesta {

    private static GestorRespuesta instancia; // Instancia única del Singleton
    private final GestorConexion gestorConexion;
    private final Map<String, Consumer<DTOResponse>> manejadores;
    private final Gson gson;
    private Thread hiloEscucha;

    // El constructor ahora es privado.
    private GestorRespuesta() {
        this.gestorConexion = GestorConexion.getInstancia();
        this.manejadores = new ConcurrentHashMap<>();
        this.gson = new Gson();
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
        iniciarEscucha(TipoPool.CLIENTES);
    }

    /**
     * Inicia la escucha de respuestas usando el pool indicado (CLIENTES o PEERS).
     */
    public void iniciarEscucha(TipoPool tipoPool) {
        if (hiloEscucha != null && hiloEscucha.isAlive()) {
            LoggerCentral.info("El gestor de respuestas ya está escuchando.");
            return;
        }

        hiloEscucha = new Thread(() -> {
            // Obtener una sesión del pool seleccionado
            DTOSesion sesion = null;
            if (tipoPool == TipoPool.PEERS) {
                sesion = gestorConexion.obtenerSesionPeer(5000);
            } else {
                sesion = gestorConexion.obtenerSesionCliente(5000);
            }

            if (sesion == null || !sesion.estaActiva()) {
                LoggerCentral.error("No se puede iniciar la escucha en pool " + tipoPool + ": no hay sesión activa disponible.");
                return;
            }

            try (BufferedReader in = sesion.getIn()) {
                LoggerCentral.info("Gestor de respuestas iniciado en pool " + tipoPool + ". Esperando mensajes...");
                String respuestaServidor;
                while (!Thread.currentThread().isInterrupted() && (respuestaServidor = in.readLine()) != null) {
                    // Truncar respuestas muy largas para evitar imprimir imágenes en base64
                    String respuestaParaLog = truncarRespuesta(respuestaServidor, 500);
                    LoggerCentral.info("[" + tipoPool + "] << Respuesta recibida: " + respuestaParaLog);
                    procesarRespuesta(respuestaServidor);
                }
            } catch (IOException e) {
                if (!Thread.currentThread().isInterrupted()) {
                    LoggerCentral.error("La conexión con el servidor se ha perdido en pool " + tipoPool + ": " + e.getMessage(), e);
                }
            } finally {
                // Liberar la sesión de vuelta al pool correspondiente
                try {
                    if (tipoPool == TipoPool.PEERS) gestorConexion.liberarSesionPeer(sesion);
                    else gestorConexion.liberarSesionCliente(sesion);
                } catch (Exception ex) {
                    LoggerCentral.error("Error liberando sesión en pool " + tipoPool + ": " + ex.getMessage(), ex);
                }
                LoggerCentral.info("El gestor de respuestas ha dejado de escuchar en pool " + tipoPool + ".");
            }
        }, "Hilo-Escucha-Respuestas-" + (tipoPool != null ? tipoPool : TipoPool.CLIENTES));
        hiloEscucha.setDaemon(true);
        hiloEscucha.start();
    }

    private void procesarRespuesta(String jsonResponse) {
        try {
            DTOResponse response = gson.fromJson(jsonResponse, DTOResponse.class);
            if (response != null && response.getAction() != null) {
                // Intentar extraer requestId desde response.data si existe
                String requestId = null;
                Object dataObj = response.getData();
                if (dataObj != null) {
                    try {
                        Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
                        Map<String, Object> map = gson.fromJson(gson.toJson(dataObj), mapType);
                        if (map != null && map.containsKey("requestId")) {
                            requestId = String.valueOf(map.get("requestId"));
                        }
                    } catch (Exception ignored) {
                        // no hacer nada si no es un map
                    }
                }

                // Buscar manejador por clave específica action:requestId (si requestId existe)
                Consumer<DTOResponse> manejador = null;
                if (requestId != null && !requestId.isEmpty()) {
                    String llaveEspecifica = response.getAction() + ":" + requestId;
                    manejador = manejadores.get(llaveEspecifica);
                    if (manejador != null) {
                        try {
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
                    // Intentar buscar con todas las claves normalizadas
                    for (Map.Entry<String, Consumer<DTOResponse>> entry : manejadores.entrySet()) {
                        if (entry.getKey().toLowerCase().equals(actionNormalizada)) {
                            manejador = entry.getValue();
                            break;
                        }
                    }
                }

                if (manejador != null) {
                    try {
                        manejador.accept(response);
                    } catch (Exception e) {
                        LoggerCentral.error("Error ejecutando manejador para accion " + response.getAction() + ": " + e.getMessage(), e);
                    }
                } else {
                    LoggerCentral.warn("No se encontró un manejador para la acción: " + response.getAction());
                }
            }
        } catch (JsonSyntaxException e) {
            LoggerCentral.error("Error al parsear la respuesta JSON: " + jsonResponse + " - " + e.getMessage(), e);
        }
    }

    @Override
    public void detenerEscucha() {
        if (hiloEscucha != null && hiloEscucha.isAlive()) {
            hiloEscucha.interrupt();
            LoggerCentral.info("Solicitud de detención del gestor de respuestas enviada.");
        }
    }

    @Override
    public void registrarManejador(String tipoOperacion, Consumer<DTOResponse> manejador) {
        manejadores.put(tipoOperacion, manejador);
        LoggerCentral.debug("Manejador registrado para operación: " + tipoOperacion);
    }

    @Override
    public void removerManejador(String tipoOperacion) {
        if (tipoOperacion == null) return;
        manejadores.remove(tipoOperacion);
        LoggerCentral.debug("Manejador removido para operación: " + tipoOperacion);
    }

    /**
     * Trunca la respuesta para evitar imprimir mensajes demasiado largos que contengan imágenes en base64.
     * También elimina el campo imagenBase64 si está presente.
     *
     * @param respuesta La respuesta completa del servidor.
     * @param maxLength La longitud máxima del mensaje truncado.
     * @return El mensaje limpio y truncado si es necesario.
     */
    private String truncarRespuesta(String respuesta, int maxLength) {
        // Si la respuesta contiene imagenBase64, eliminarlo del log
        if (respuesta.contains("\"imagenBase64\":")) {
            try {
                // Usar regex para eliminar el campo imagenBase64 y su valor
                String respuestaLimpia = respuesta.replaceAll(
                        "\"imagenBase64\":\\s*\"[^\"]*\"\\s*,?",
                        "\"imagenBase64\":\"[IMAGEN_OMITIDA]\","
                );

                // Si después de limpiar aún es muy largo, truncar
                if (respuestaLimpia.length() > maxLength) {
                    return respuestaLimpia.substring(0, maxLength) + "... [mensaje truncado]";
                }
                return respuestaLimpia;
            } catch (Exception e) {
                // Si falla el regex, solo truncar
                if (respuesta.length() > maxLength) {
                    return respuesta.substring(0, maxLength) + "... [mensaje truncado]";
                }
                return respuesta;
            }
        }

        // Si no contiene imagenBase64 pero es muy largo, truncar normalmente
        if (respuesta.length() > maxLength) {
            return respuesta.substring(0, maxLength) + "... [mensaje truncado]";
        }

        return respuesta;
    }
}
