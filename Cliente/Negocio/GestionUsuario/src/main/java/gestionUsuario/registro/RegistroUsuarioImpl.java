package gestionUsuario.registro;

import com.google.gson.Gson;
import comunicacion.EnviadorPeticiones;
import comunicacion.GestorRespuesta;
import comunicacion.IEnviadorPeticiones;
import comunicacion.IGestorRespuesta;
import dto.comunicacion.DTORequest;
import dto.comunicacion.DTOResponse;
import dto.repositorio.DTOUsuarioRepositorio;
import dto.vistaRegistro.DTORegistro;
// CORRECCIÓN: Se utiliza el paquete correcto para la capa de persistencia.
import repositorio.usuario.IRepositorioUsuario;
import repositorio.usuario.RepositorioUsuarioImpl;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Implementación del componente de negocio para el registro de usuarios.
 */
public class RegistroUsuarioImpl implements IRegistroUsuario {

    private final IEnviadorPeticiones enviadorPeticiones = new EnviadorPeticiones();
    private final IGestorRespuesta gestorRespuesta = new GestorRespuesta();
    private final IRepositorioUsuario repositorioUsuario = new RepositorioUsuarioImpl();
    private final Gson gson = new Gson();

    @Override
    public CompletableFuture<Boolean> registrar(DTORegistro dto, byte[] fotoBytes) {
        CompletableFuture<Boolean> resultadoFuturo = new CompletableFuture<>();
        final String ACCION = "registerUser";

        gestorRespuesta.registrarManejador(ACCION, (DTOResponse respuesta) -> {
            if (respuesta.fueExitoso()) {
                try {
                    Map<String, String> datosServidor = gson.fromJson(gson.toJson(respuesta.getData()), Map.class);
                    UUID userId = UUID.fromString(datosServidor.get("userId"));
                    String fechaRegistroStr = datosServidor.get("fechaRegistro");
                    Date fechaRegistro = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(fechaRegistroStr);
                    String photoId = datosServidor.get("photoId"); // Se asume que el servidor también devuelve el photoId

                    DTOUsuarioRepositorio datosParaGuardar = new DTOUsuarioRepositorio(
                            userId,
                            dto.getName(),
                            dto.getEmail(),
                            dto.getPassword(),
                            fotoBytes,
                            photoId, // Se pasa el photoId al DTO del repositorio
                            dto.getIp(),
                            fechaRegistro
                    );

                    repositorioUsuario.guardarUsuario(datosParaGuardar);
                    resultadoFuturo.complete(true);

                } catch (Exception e) {
                    System.err.println("Error al procesar la respuesta de registro: " + e.getMessage());
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

