package gestionUsuario.autenticacion;

import com.google.gson.Gson;
import comunicacion.EnviadorPeticiones;
import comunicacion.GestorRespuesta;
import comunicacion.IEnviadorPeticiones;
import comunicacion.IGestorRespuesta;
import dto.comunicacion.DTORequest;
import dto.comunicacion.DTOResponse;
import dto.vistaLogin.DTOAutenticacion;
import gestionUsuario.sesion.GestorSesionUsuario; // Se importa el nuevo gestor

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * AHORA guarda los datos del usuario en el GestorSesionUsuario tras una autenticación exitosa.
 */
public class AutenticarUsuario implements IAutenticarUsuario {

    private final IEnviadorPeticiones enviadorPeticiones;
    private final IGestorRespuesta gestorRespuesta;
    private final Gson gson;

    public AutenticarUsuario() {
        this.enviadorPeticiones = new EnviadorPeticiones();
        this.gestorRespuesta = GestorRespuesta.getInstancia();
        this.gson = new Gson();
    }

    @Override
    public CompletableFuture<Boolean> autenticar(DTOAutenticacion dto) {
        CompletableFuture<Boolean> resultadoFuturo = new CompletableFuture<>();
        final String ACCION = "authenticateUser";

        gestorRespuesta.registrarManejador(ACCION, (DTOResponse respuesta) -> {
            if (respuesta.fueExitoso()) {
                try {
                    // 1. Extraer el userId de la respuesta del servidor.
                    Map<String, String> datosUsuario = gson.fromJson(gson.toJson(respuesta.getData()), Map.class);
                    String userId = datosUsuario.get("userId");

                    if (userId == null || userId.isEmpty()) {
                        throw new Exception("La respuesta del servidor no contenía un 'userId'.");
                    }

                    // 2. Guardar el ID del usuario en el gestor de sesión global.
                    GestorSesionUsuario.getInstancia().setUserId(userId);
                    System.out.println("✅ [AutenticarUsuario]: Sesión iniciada para el usuario con ID: " + userId);

                    resultadoFuturo.complete(true);
                } catch (Exception e) {
                    System.err.println("❌ [AutenticarUsuario]: Error al procesar la respuesta de autenticación: " + e.getMessage());
                    resultadoFuturo.complete(false);
                }
            } else {
                resultadoFuturo.complete(false);
            }
        });

        DTORequest peticion = new DTORequest(ACCION, dto);
        enviadorPeticiones.enviar(peticion);
        return resultadoFuturo;
    }
}

