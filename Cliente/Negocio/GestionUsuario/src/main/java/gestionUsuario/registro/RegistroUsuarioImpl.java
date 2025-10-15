package gestionUsuario.registro;

import com.google.gson.Gson;
import comunicacion.EnviadorPeticiones;
import comunicacion.GestorRespuesta;
import comunicacion.IEnviadorPeticiones;
import comunicacion.IGestorRespuesta;
import dominio.Usuario;
import dto.comunicacion.DTORequest;
import dto.comunicacion.DTOResponse;
import dto.vistaRegistro.DTORegistro;
import gestionUsuario.especialista.EspecialistaUsuariosImpl;
import gestionUsuario.especialista.IEspecialistaUsuarios;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Implementación del componente de negocio para el registro de usuarios.
 */
public class RegistroUsuarioImpl implements IRegistroUsuario {

    private final IEnviadorPeticiones enviadorPeticiones;
    private final IGestorRespuesta gestorRespuesta;
    private final IEspecialistaUsuarios especialistaUsuarios;
    private final Gson gson;

    public RegistroUsuarioImpl() {
        this.enviadorPeticiones = new EnviadorPeticiones();
        this.gestorRespuesta = GestorRespuesta.getInstancia();
        this.especialistaUsuarios = new EspecialistaUsuariosImpl();
        this.gson = new Gson();
    }

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
                    String photoId = datosServidor.get("photoId");

                    // Convertir Date a LocalDateTime
                    LocalDateTime fechaRegistroLocal = fechaRegistro.toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime();

                    // Crear entidad de dominio Usuario
                    Usuario usuario = new Usuario();
                    usuario.setIdUsuario(userId);
                    usuario.setNombre(dto.getName());
                    usuario.setEmail(dto.getEmail());
                    usuario.setEstado("activo");
                    usuario.setFoto(fotoBytes);
                    usuario.setPhotoIdServidor(photoId);
                    usuario.setIp(dto.getIp());
                    usuario.setFechaRegistro(fechaRegistroLocal);

                    // Guardar usando el especialista
                    especialistaUsuarios.guardarUsuario(usuario);
                    resultadoFuturo.complete(true);

                } catch (Exception e) {
                    System.err.println("❌ [RegistroUsuario]: Error al procesar la respuesta de registro: " + e.getMessage());
                    e.printStackTrace();
                    resultadoFuturo.complete(false);
                }
            } else {
                System.err.println("⚠️ [RegistroUsuario]: Registro no exitoso en el servidor");
                resultadoFuturo.complete(false);
            }
        });

        DTORequest peticion = new DTORequest(ACCION, dto);
        enviadorPeticiones.enviar(peticion);
        return resultadoFuturo;
    }
}
