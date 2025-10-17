package gestionUsuario.autenticacion;

import com.google.gson.Gson;
import comunicacion.EnviadorPeticiones;
import comunicacion.GestorRespuesta;
import comunicacion.IEnviadorPeticiones;
import comunicacion.IGestorRespuesta;
import dominio.Usuario;
import dto.comunicacion.DTORequest;
import dto.comunicacion.DTOResponse;
import dto.vistaLogin.DTOAutenticacion;
import gestionUsuario.especialista.EspecialistaUsuariosImpl;
import gestionUsuario.especialista.IEspecialistaUsuarios;
import gestionUsuario.sesion.GestorSesionUsuario;
import observador.IObservador;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.text.SimpleDateFormat;

/**
 * Implementación del componente de autenticación con patrón Observador.
 * Notifica a la UI sobre eventos de autenticación y guarda el usuario en BD local.
 */
public class AutenticarUsuario implements IAutenticarUsuario {

    private final IEnviadorPeticiones enviadorPeticiones;
    private final IGestorRespuesta gestorRespuesta;
    private final IEspecialistaUsuarios especialistaUsuarios;
    private final Gson gson;

    // Patrón Observador
    private final List<IObservador> observadores;

    public AutenticarUsuario() {
        this.enviadorPeticiones = new EnviadorPeticiones();
        this.gestorRespuesta = GestorRespuesta.getInstancia();
        this.especialistaUsuarios = new EspecialistaUsuariosImpl();
        this.gson = new Gson();
        this.observadores = new ArrayList<>();
        System.out.println("✅ [AutenticarUsuario]: Inicializado con Observador");
    }

    // Implementación del patrón Observador
    @Override
    public void registrarObservador(IObservador observador) {
        if (!observadores.contains(observador)) {
            observadores.add(observador);
            System.out.println("[AutenticarUsuario] Observador registrado: " + observador.getClass().getSimpleName());
        }
    }

    @Override
    public void removerObservador(IObservador observador) {
        observadores.remove(observador);
        System.out.println("[AutenticarUsuario] Observador removido: " + observador.getClass().getSimpleName());
    }

    @Override
    public void notificarObservadores(String tipoDeDato, Object datos) {
        System.out.println("[AutenticarUsuario] Notificando a " + observadores.size() + " observadores - Tipo: " + tipoDeDato);
        for (IObservador observador : observadores) {
            observador.actualizar(tipoDeDato, datos);
        }
    }

    @Override
    public CompletableFuture<Boolean> autenticar(DTOAutenticacion dto) {
        CompletableFuture<Boolean> resultadoFuturo = new CompletableFuture<>();
        final String ACCION_ENVIADA = "authenticateUser";
        final String ACCION_RESPUESTA = "login"; // El servidor responde con esta acción

        // Notificar inicio de autenticación
        notificarObservadores("AUTENTICACION_INICIADA", dto.getEmailUsuario());

        // Manejador común para procesar la respuesta
        java.util.function.Consumer<DTOResponse> procesarRespuesta = (DTOResponse respuesta) -> {
            if (respuesta.fueExitoso()) {
                try {
                    // Extraer datos del servidor
                    Map<String, Object> datosUsuario = gson.fromJson(gson.toJson(respuesta.getData()), Map.class);

                    UUID userId = UUID.fromString((String) datosUsuario.get("id"));
                    String nombre = (String) datosUsuario.get("nombre");
                    String email = (String) datosUsuario.get("email");
                    String photoId = (String) datosUsuario.get("photoId");
                    String estado = (String) datosUsuario.get("estado");
                    String fechaRegistroStr = (String) datosUsuario.get("fechaRegistro");

                    if (userId == null || userId.toString().isEmpty()) {
                        throw new Exception("La respuesta del servidor no contenía un 'id' válido.");
                    }

                    // Verificar estado del usuario
                    if ("baneado".equals(estado)) {
                        String razon = datosUsuario.containsKey("razon") ? (String) datosUsuario.get("razon") : "Usuario baneado";
                        notificarObservadores("USUARIO_BANEADO", razon);
                        resultadoFuturo.complete(false);
                        return;
                    }

                    // Convertir fecha de registro
                    LocalDateTime fechaRegistro = null;
                    if (fechaRegistroStr != null) {
                        Date fecha = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(fechaRegistroStr);
                        fechaRegistro = fecha.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                    }

                    // Crear o actualizar usuario en BD local
                    Usuario usuario;
                    Usuario usuarioExistente = especialistaUsuarios.obtenerUsuarioPorId(userId);

                    if (usuarioExistente != null) {
                        // Usuario ya existe, actualizar
                        usuario = usuarioExistente;
                        usuario.setNombre(nombre);
                        usuario.setEmail(email);
                        usuario.setEstado(estado);
                        usuario.setPhotoIdServidor(photoId);
                        especialistaUsuarios.actualizarUsuario(usuario);
                        System.out.println("✅ [AutenticarUsuario]: Usuario actualizado en BD local");
                    } else {
                        // Usuario no existe, crear nuevo
                        usuario = new Usuario();
                        usuario.setIdUsuario(userId);
                        usuario.setNombre(nombre);
                        usuario.setEmail(email);
                        usuario.setEstado(estado);
                        usuario.setPhotoIdServidor(photoId);
                        usuario.setFechaRegistro(fechaRegistro);
                        especialistaUsuarios.guardarUsuario(usuario);
                        System.out.println("✅ [AutenticarUsuario]: Usuario guardado en BD local");
                    }

                    // Guardar en sesión global
                    GestorSesionUsuario.getInstancia().setUserId(userId.toString());
                    GestorSesionUsuario.getInstancia().setUsuarioLogueado(usuario);

                    System.out.println("✅ [AutenticarUsuario]: Sesión iniciada para: " + nombre);

                    // Notificar éxito
                    notificarObservadores("AUTENTICACION_EXITOSA", usuario);
                    notificarObservadores("USUARIO_LOGUEADO", usuario);

                    resultadoFuturo.complete(true);

                } catch (Exception e) {
                    System.err.println("❌ [AutenticarUsuario]: Error al procesar respuesta: " + e.getMessage());
                    e.printStackTrace();
                    notificarObservadores("AUTENTICACION_ERROR", "Error al procesar datos del usuario");
                    resultadoFuturo.complete(false);
                }
            } else {
                String mensajeError = respuesta.getMessage() != null ? respuesta.getMessage() : "Credenciales incorrectas";
                System.err.println("⚠️ [AutenticarUsuario]: " + mensajeError);
                notificarObservadores("AUTENTICACION_ERROR", mensajeError);
                resultadoFuturo.complete(false);
            }
        };

        // Registrar AMBOS manejadores (por si acaso cambia el servidor)
        gestorRespuesta.registrarManejador(ACCION_ENVIADA, procesarRespuesta);
        gestorRespuesta.registrarManejador(ACCION_RESPUESTA, procesarRespuesta);

        DTORequest peticion = new DTORequest(ACCION_ENVIADA, dto);
        enviadorPeticiones.enviar(peticion);
        return resultadoFuturo;
    }
}
