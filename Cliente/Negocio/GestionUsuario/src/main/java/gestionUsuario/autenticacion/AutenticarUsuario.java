// language: java
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

    // Nuevo constructor para inyección de dependencias en pruebas
    public AutenticarUsuario(IEnviadorPeticiones enviadorPeticiones,
                             IGestorRespuesta gestorRespuesta,
                             IEspecialistaUsuarios especialistaUsuarios) {
        this.enviadorPeticiones = enviadorPeticiones;
        this.gestorRespuesta = gestorRespuesta;
        this.especialistaUsuarios = especialistaUsuarios;
        this.gson = new Gson();
        this.observadores = new ArrayList<>();
        System.out.println("✅ [AutenticarUsuario]: Inicializado (testing constructor)");
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
        final String ACCION_RESPUESTA = "authenticateUser";

        // Notificar inicio de autenticación
        notificarObservadores("AUTENTICACION_INICIADA", dto.getNombreUsuario());

        // Manejador común para procesar la respuesta
        java.util.function.Consumer<DTOResponse> procesarRespuesta = (DTOResponse respuesta) -> {
            try {
                // Detectar éxito: puede venir como "success": true o "status": "success"
                boolean exito = respuesta.fueExitoso();
                String mensaje = respuesta.getMessage();
                Object dataObj = respuesta.getData();

                // Verificar si hay error por status: "error"
                if (respuesta.getAction() != null && respuesta.getAction().equals(ACCION_RESPUESTA)) {
                    // Verificar campo status alternativo si existe
                    String status = respuesta.getStatus();
                    if ("error".equalsIgnoreCase(status)) {
                        exito = false;
                    } else if ("success".equalsIgnoreCase(status)) {
                        exito = true;
                    }
                }

                if (exito && dataObj != null && !"null".equalsIgnoreCase(String.valueOf(dataObj))) {
                    // Extraer datos del servidor según especificación
                    Map<String, Object> datosUsuario = gson.fromJson(gson.toJson(dataObj), Map.class);

                    if (datosUsuario == null || datosUsuario.isEmpty()) {
                        throw new Exception("La respuesta no contiene 'data' con información del usuario.");
                    }

                    String userIdStr = firstString(datosUsuario, "userId", "id");
                    String nombre = firstString(datosUsuario, "nombre");
                    String email = firstString(datosUsuario, "email");
                    String imagenBase64 = firstString(datosUsuario, "imagenBase64");

                    if (userIdStr == null || userIdStr.isEmpty()) {
                        throw new Exception("La respuesta del servidor no contenía un 'userId' válido.");
                    }

                    UUID userId = UUID.fromString(userIdStr);

                    // Crear o actualizar usuario en BD local
                    Usuario usuario;
                    Usuario usuarioExistente = especialistaUsuarios.obtenerUsuarioPorId(userId);

                    if (usuarioExistente != null) {
                        usuario = usuarioExistente;
                        if (nombre != null) usuario.setNombre(nombre);
                        if (email != null) usuario.setEmail(email);
                        usuario.setEstado("activo"); // Estado por defecto ya que el servidor no lo envía
                        if (imagenBase64 != null) usuario.setPhotoIdServidor(imagenBase64);
                        especialistaUsuarios.actualizarUsuario(usuario);
                        System.out.println("✅ [AutenticarUsuario]: Usuario actualizado en BD local");
                    } else {
                        usuario = new Usuario();
                        usuario.setIdUsuario(userId);
                        usuario.setNombre(nombre);
                        usuario.setEmail(email);
                        usuario.setEstado("activo");
                        usuario.setPhotoIdServidor(imagenBase64);
                        usuario.setFechaRegistro(LocalDateTime.now()); // Usar fecha actual ya que el servidor no la envía
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
                } else {
                    // Manejar error según especificación
                    String mensajeError = mensaje != null ? mensaje : "Credenciales incorrectas";

                    // Verificar si hay detalles adicionales en data (campo/motivo)
                    if (dataObj != null && !"null".equalsIgnoreCase(String.valueOf(dataObj))) {
                        Map<String, Object> datosError = gson.fromJson(gson.toJson(dataObj), Map.class);
                        if (datosError != null && datosError.containsKey("campo")) {
                            String campo = String.valueOf(datosError.get("campo"));
                            String motivo = String.valueOf(datosError.getOrDefault("motivo", ""));
                            mensajeError += " - Campo: " + campo + ", Motivo: " + motivo;
                        }
                    }

                    System.err.println("⚠️ [AutenticarUsuario]: " + mensajeError);
                    notificarObservadores("AUTENTICACION_ERROR", mensajeError);
                    resultadoFuturo.complete(false);
                }
            } catch (Exception e) {
                System.err.println("❌ [AutenticarUsuario]: Error al procesar respuesta: " + e.getMessage());
                e.printStackTrace();
                notificarObservadores("AUTENTICACION_ERROR", "Error al procesar datos del usuario");
                resultadoFuturo.complete(false);
            }
        };

        // Registrar manejadores
        gestorRespuesta.registrarManejador(ACCION_ENVIADA, procesarRespuesta);
        gestorRespuesta.registrarManejador(ACCION_RESPUESTA, procesarRespuesta);

        DTORequest peticion = new DTORequest(ACCION_ENVIADA, dto);
        enviadorPeticiones.enviar(peticion);
        return resultadoFuturo;
    }

    /**
     * Mapea el estado del servidor al formato de la BD local.
     * Servidor: ONLINE, OFFLINE, BANNED
     * BD Local: activo, inactivo, baneado
     */
    private String mapearEstadoServidor(String estadoServidor) {
        if (estadoServidor == null) {
            return "activo"; // Default
        }

        switch (estadoServidor.toUpperCase()) {
            case "ONLINE":
                return "activo";
            case "OFFLINE":
                return "inactivo";
            case "BANNED":
            case "BANEADO":
                return "baneado";
            case "ACTIVE":
            case "ACTIVO":
                return "activo";
            case "INACTIVE":
            case "INACTIVO":
                return "inactivo";
            default:
                System.out.println("⚠️ [AutenticarUsuario]: Estado desconocido del servidor: " + estadoServidor + ", usando 'activo' por defecto");
                return "activo";
        }
    }

    // Helper: returns the first non-null, non-empty string value from the map for given keys
    private String firstString(Map<String, Object> map, String... keys) {
        if (map == null) return null;
        for (String k : keys) {
            if (map.containsKey(k)) {
                Object v = map.get(k);
                if (v != null) {
                    String s = String.valueOf(v);
                    if (!s.isEmpty() && !"null".equalsIgnoreCase(s)) {
                        return s;
                    }
                }
            }
        }
        return null;
    }
}
