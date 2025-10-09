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
 */
public class GestorRespuesta implements IGestorRespuesta {

    private final GestorConexion gestorConexion;
    private final Map<String, Consumer<DTOResponse>> manejadores;
    private final Gson gson;
    private Thread hiloEscucha;

    public GestorRespuesta() {
        this.gestorConexion = GestorConexion.getInstancia();
        this.manejadores = new ConcurrentHashMap<>();
        this.gson = new Gson();
    }

    @Override
    public void iniciarEscucha() {
        if (hiloEscucha != null && hiloEscucha.isAlive()) {
            System.out.println("El gestor de respuestas ya está escuchando.");
            return;
        }

        hiloEscucha = new Thread(() -> {
            // CORRECCIÓN: Se utiliza el nombre de método correcto 'getSesion()'.
            DTOSesion sesion = gestorConexion.getSesion();
            if (sesion == null || !sesion.estaActiva()) {
                System.err.println("No se puede iniciar la escucha, no hay sesión activa.");
                return;
            }

            try (BufferedReader in = sesion.getIn()) {
                System.out.println("Gestor de respuestas iniciado. Esperando mensajes...");
                String respuestaServidor;
                while (!Thread.currentThread().isInterrupted() && (respuestaServidor = in.readLine()) != null) {
                    System.out.println("<< Respuesta recibida: " + respuestaServidor);
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
            if (response != null && response.getAction() != null) {
                Consumer<DTOResponse> manejador = manejadores.get(response.getAction());
                if (manejador != null) {
                    manejador.accept(response);
                } else {
                    System.out.println("No se encontró un manejador para la acción: " + response.getAction());
                }
            }
        } catch (JsonSyntaxException e) {
            System.err.println("Error al parsear la respuesta JSON: " + jsonResponse);
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
}

