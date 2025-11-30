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
import gestionUsuario.autenticacion.mapper.RespuestaUsuarioMapper;
import gestionUsuario.autenticacion.mapper.EstadoServidorMapper;
import observador.IObservador;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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
    @SuppressWarnings("unused")
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
                    // Usar mapper para convertir la sección data a Map y construir Usuario
                    Map<String, Object> datosUsuario = RespuestaUsuarioMapper.toMap(dataObj, gson);

                    if (datosUsuario == null || datosUsuario.isEmpty()) {
                        throw new Exception("La respuesta no contiene 'data' con información del usuario.");
                    }

                    Usuario usuarioNuevo = RespuestaUsuarioMapper.buildUsuarioFromMap(datosUsuario);
                    if (usuarioNuevo == null) {
                        throw new Exception("No se pudo construir el usuario a partir de la respuesta del servidor.");
                    }

                    // Crear o actualizar usuario en BD local
                    Usuario usuario;
                    Usuario usuarioExistente = especialistaUsuarios.obtenerUsuarioPorId(usuarioNuevo.getIdUsuario());

                    if (usuarioExistente != null) {
                        usuario = usuarioExistente;
                        if (usuarioNuevo.getNombre() != null) usuario.setNombre(usuarioNuevo.getNombre());
                        if (usuarioNuevo.getEmail() != null) usuario.setEmail(usuarioNuevo.getEmail());
                        usuario.setEstado(EstadoServidorMapper.mapearEstadoServidor(null)); // mantener activo por defecto
                        if (usuarioNuevo.getPhotoIdServidor() != null) usuario.setPhotoIdServidor(usuarioNuevo.getPhotoIdServidor());
                        especialistaUsuarios.actualizarUsuario(usuario);
                        System.out.println("✅ [AutenticarUsuario]: Usuario actualizado en BD local");
                    } else {
                        usuario = usuarioNuevo;
                        especialistaUsuarios.guardarUsuario(usuario);
                        System.out.println("✅ [AutenticarUsuario]: Usuario guardado en BD local");
                    }

                    // Guardar en sesión global
                    GestorSesionUsuario.getInstancia().setUserId(usuario.getIdUsuario().toString());
                    GestorSesionUsuario.getInstancia().setUsuarioLogueado(usuario);

                    System.out.println("✅ [AutenticarUsuario]: Sesión iniciada para: " + usuario.getNombre());

                    // Notificar éxito de autenticación
                    // La descarga de foto se manejará en la capa de Fachada/Servicio
                    notificarObservadores("AUTENTICACION_EXITOSA", usuario);
                    notificarObservadores("USUARIO_LOGUEADO", usuario);

                    resultadoFuturo.complete(true);
                } else {
                    // Manejar error según especificación
                    String mensajeError = mensaje != null ? mensaje : "Credenciales incorrectas";

                    // Verificar si hay detalles adicionales en data (campo/motivo)
                    if (dataObj != null && !"null".equalsIgnoreCase(String.valueOf(dataObj))) {
                        Map<String, Object> datosError = RespuestaUsuarioMapper.toMap(dataObj, gson);
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
                // Imprimir stack trace de forma controlada (evita printStackTrace directo)
                for (StackTraceElement ste : e.getStackTrace()) {
                    System.err.println("    at " + ste.toString());
                }
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
}
