package gestionUsuario.autenticacion;

import com.google.gson.Gson;
import comunicacion.EnviadorPeticiones;
import comunicacion.GestorRespuesta;
import comunicacion.IEnviadorPeticiones;
import comunicacion.IGestorRespuesta;
import dto.comunicacion.DTORequest;
import dto.comunicacion.DTOResponse;
import dto.vistaLogin.DTOAutenticacion;

import java.util.concurrent.CompletableFuture;

/**
 * Implementación del componente de negocio para la autenticación de usuarios.
 * Esta clase se comunica con la capa de persistencia (comunicación) y
 * maneja la respuesta del servidor de forma asíncrona.
 */
public class AutenticarUsuario implements IAutenticarUsuario {

    // Dependencias con la capa de comunicación. En una app real, serían inyectadas.
    private final IEnviadorPeticiones enviadorPeticiones = new EnviadorPeticiones();
    private final IGestorRespuesta gestorRespuesta = new GestorRespuesta();
    private final Gson gson = new Gson(); // Para manejar el campo 'data' si es necesario.

    @Override
    public CompletableFuture<Boolean> autenticar(DTOAutenticacion dto) {
        // 1. Crear la "promesa". Este objeto será devuelto inmediatamente,
        // y lo "completaremos" cuando llegue la respuesta del servidor.
        CompletableFuture<Boolean> resultadoFuturo = new CompletableFuture<>();

        final String ACCION = "authenticateUser";

        // 2. Construir y registrar el MANEJADOR para esta petición específica.
        // Le decimos al GestorRespuesta: "Cuando llegue una respuesta para la acción 'authenticateUser',
        // ejecuta este código".
        gestorRespuesta.registrarManejador(ACCION, (DTOResponse respuesta) -> {

            // Este código se ejecutará en el hilo del GestorRespuesta cuando llegue la respuesta.
            if (respuesta.fueExitoso()) {
                System.out.println("GestionUsuario (Manejador): La autenticación fue exitosa.");
                // Cumplimos la promesa con un valor de 'true'.
                resultadoFuturo.complete(true);
            } else {
                System.out.println("GestionUsuario (Manejador): " + respuesta.getMessage());
                // Cumplimos la promesa con un valor de 'false'.
                resultadoFuturo.complete(false);
            }
        });

        // 3. Crear el DTO de la petición con la acción y los datos (payload).
        DTORequest peticion = new DTORequest(ACCION, dto);

        // 4. Enviar la petición. Esto no bloquea la aplicación.
        System.out.println("GestionUsuario: Enviando petición de autenticación...");
        enviadorPeticiones.enviar(peticion);

        // 5. Devolver la promesa inmediatamente. La vista se quedará "esperando"
        // a que el manejador la complete en el futuro.
        return resultadoFuturo;
    }
}

