package comunicacion;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import conexion.GestorConexion;
import dto.comunicacion.DTOResponse;
import dto.gestionConexion.conexion.DTOSesion;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

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

    // ... El resto de los métodos permanecen igual, pero ahora se accede a ellos
    // a través de GestorRespuesta.getInstancia().metodo()
    @Override
    public void iniciarEscucha() {
        if (hiloEscucha != null && hiloEscucha.isAlive()) {
            System.out.println("El gestor de respuestas ya está escuchando.");
            return;
        }

        hiloEscucha = new Thread(() -> {
            DTOSesion sesion = gestorConexion.getSesion();
            if (sesion == null || !sesion.estaActiva()) {
                System.err.println("No se puede iniciar la escucha, no hay sesión activa.");
                return;
            }

            try (BufferedReader in = sesion.getIn()) {
                System.out.println("Gestor de respuestas iniciado. Esperando mensajes...");
                String respuestaServidor;
                while (!Thread.currentThread().isInterrupted() && (respuestaServidor = in.readLine()) != null) {
                    // Truncar respuestas muy largas para evitar imprimir imágenes en base64
                    String respuestaParaLog = truncarRespuesta(respuestaServidor, 500);
                    System.out.println("<< Respuesta recibida: " + respuestaParaLog);
                    procesarRespuesta(respuestaServidor);
                }
            } catch (IOException e) {
                if (!Thread.currentThread().isInterrupted()) {
                    System.err.println("La conexión con el servidor se ha perdido.");
                }
            } finally {
                System.out.println("El gestor de respuestas ha dejado de escuchar.");
            }
        });
        hiloEscucha.start();
    }

    private void procesarRespuesta(String jsonResponse) {
        try {
            DTOResponse response = gson.fromJson(jsonResponse, DTOResponse.class);
            if (response != null) {
                // Obtener el identificador (puede ser action o type)
                String identificador = response.getIdentificador();

                if (identificador != null) {
                    // Normalizar el identificador a minúsculas para comparación case-insensitive
                    String identificadorNormalizado = identificador.toLowerCase();

                    // Buscar manejador con el identificador original o normalizado
                    Consumer<DTOResponse> manejador = manejadores.get(identificador);
                    if (manejador == null) {
                        // Intentar buscar con todas las claves normalizadas
                        for (Map.Entry<String, Consumer<DTOResponse>> entry : manejadores.entrySet()) {
                            if (entry.getKey().toLowerCase().equals(identificadorNormalizado)) {
                                manejador = entry.getValue();
                                break;
                            }
                        }
                    }

                    if (manejador != null) {
                        System.out.println("✅ Ejecutando manejador para: " + identificador);
                        manejador.accept(response);
                    } else {
                        System.out.println("⚠️ No se encontró un manejador para: " + identificador);
                    }
                } else {
                    System.out.println("⚠️ Respuesta sin identificador (action/type): " + jsonResponse);
                }
            }
        } catch (JsonSyntaxException e) {
            System.err.println("❌ Error al parsear la respuesta JSON: " + jsonResponse);
            e.printStackTrace();
        }
    }

    @Override
    public void detenerEscucha() {
        if (hiloEscucha != null && hiloEscucha.isAlive()) {
            hiloEscucha.interrupt();
        }
    }

    @Override
    public void registrarManejador(String tipoOperacion, Consumer<DTOResponse> manejador) {
        manejadores.put(tipoOperacion, manejador);
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
