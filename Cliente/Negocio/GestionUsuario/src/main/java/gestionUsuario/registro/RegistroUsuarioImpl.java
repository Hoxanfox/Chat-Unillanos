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
import observador.IObservador;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Implementación del componente de negocio para el registro de usuarios.
 * Notifica a la UI sobre eventos de registro mediante el patrón Observador.
 */
public class RegistroUsuarioImpl implements IRegistroUsuario {

    private final IEnviadorPeticiones enviadorPeticiones;
    private final IGestorRespuesta gestorRespuesta;
    private final IEspecialistaUsuarios especialistaUsuarios;
    private final Gson gson;

    // Patrón Observador
    private final List<IObservador> observadores;

    public RegistroUsuarioImpl() {
        this.enviadorPeticiones = new EnviadorPeticiones();
        this.gestorRespuesta = GestorRespuesta.getInstancia();
        this.especialistaUsuarios = new EspecialistaUsuariosImpl();
        this.gson = new Gson();
        this.observadores = new ArrayList<>();
        System.out.println("✅ [RegistroUsuario]: Inicializado con Observador");
    }

    // Implementación del patrón Observador
    @Override
    public void registrarObservador(IObservador observador) {
        if (!observadores.contains(observador)) {
            observadores.add(observador);
            System.out.println("[RegistroUsuario] Observador registrado: " + observador.getClass().getSimpleName());
        }
    }

    @Override
    public void removerObservador(IObservador observador) {
        observadores.remove(observador);
        System.out.println("[RegistroUsuario] Observador removido: " + observador.getClass().getSimpleName());
    }

    @Override
    public void notificarObservadores(String tipoDeDato, Object datos) {
        System.out.println("[RegistroUsuario] Notificando a " + observadores.size() + " observadores - Tipo: " + tipoDeDato);
        for (IObservador observador : observadores) {
            observador.actualizar(tipoDeDato, datos);
        }
    }

    @Override
    public CompletableFuture<Boolean> registrar(DTORegistro dto, byte[] fotoBytes) {
        CompletableFuture<Boolean> resultadoFuturo = new CompletableFuture<>();
        final String ACCION_ENVIADA = "registerUser";
        final String ACCION_RESPUESTA = "register"; // Por si el servidor responde con otra acción

        // Notificar inicio de registro
        notificarObservadores("REGISTRO_INICIADO", dto);

        // Manejador común para procesar la respuesta
        java.util.function.Consumer<DTOResponse> procesarRespuesta = (DTOResponse respuesta) -> {
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

                    System.out.println("✅ [RegistroUsuario]: Usuario guardado en BD local - ID: " + userId);

                    // Notificar éxito con el usuario completo
                    notificarObservadores("REGISTRO_EXITOSO", usuario);

                    resultadoFuturo.complete(true);

                } catch (Exception e) {
                    System.err.println("❌ [RegistroUsuario]: Error al procesar respuesta: " + e.getMessage());
                    e.printStackTrace();
                    notificarObservadores("REGISTRO_ERROR", "Error al guardar usuario en BD local");
                    resultadoFuturo.complete(false);
                }
            } else {
                String mensajeError = respuesta.getMessage() != null ? respuesta.getMessage() : "Error en el registro";
                System.err.println("⚠️ [RegistroUsuario]: " + mensajeError);
                notificarObservadores("REGISTRO_ERROR", mensajeError);
                resultadoFuturo.complete(false);
            }
        };

        // Registrar AMBOS manejadores (por si el servidor cambia la acción de respuesta)
        gestorRespuesta.registrarManejador(ACCION_ENVIADA, procesarRespuesta);
        gestorRespuesta.registrarManejador(ACCION_RESPUESTA, procesarRespuesta);

        DTORequest peticion = new DTORequest(ACCION_ENVIADA, dto);
        enviadorPeticiones.enviar(peticion);
        return resultadoFuturo;
    }
}
