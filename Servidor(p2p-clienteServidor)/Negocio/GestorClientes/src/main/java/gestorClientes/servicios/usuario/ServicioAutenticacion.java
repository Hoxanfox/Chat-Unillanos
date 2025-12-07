package gestorClientes.servicios.usuario;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import conexion.clientes.interfaces.IGestorConexionesCliente;
import conexion.clientes.interfaces.IRouterMensajesCliente;
import dominio.clienteServidor.Usuario;
import dto.comunicacion.DTOResponse;
import gestorClientes.interfaces.IServicioCliente;
import gestorP2P.servicios.ServicioSincronizacionDatos;
import logger.LoggerCentral;
import observador.IObservador;
import observador.ISujeto;
import repositorio.clienteServidor.UsuarioRepositorio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ServicioAutenticacion implements IServicioCliente, ISujeto {

    private static final String TAG = "AuthService";
    private IGestorConexionesCliente gestor;
    private final UsuarioRepositorio repoUsuario;
    private final Gson gson;

    // Referencia al servicio de sincronización P2P (inyectada externamente)
    private ServicioSincronizacionDatos servicioSync;

    // ✅ NUEVO: Lista de observadores para notificar cambios
    private final List<IObservador> observadores;

    public ServicioAutenticacion() {
        this.repoUsuario = new UsuarioRepositorio();
        this.gson = new Gson();
        this.observadores = new ArrayList<>();
    }

    /**
     * Permite inyectar el servicio de sincronización P2P.
     * Esto permite que los cambios de estado de usuarios se sincronicen automáticamente entre peers.
     */
    public void setServicioSync(ServicioSincronizacionDatos sync) {
        this.servicioSync = sync;
        LoggerCentral.info(TAG, "Servicio de sincronización P2P configurado");
    }

    @Override
    public String getNombre() {
        return "ServicioAutenticacion";
    }

    @Override
    public void inicializar(IGestorConexionesCliente gestor, IRouterMensajesCliente router) {
        this.gestor = gestor;

        // RUTA: authenticateUser (coincide con el cliente)
        router.registrarAccion("authenticateUser", (datos, idSesion) -> {
            try {
                JsonObject creds = datos.getAsJsonObject();

                // Validar campos requeridos
                if (!creds.has("nombreUsuario") || !creds.has("password")) {
                    Map<String, Object> errorData = new HashMap<>();
                    errorData.put("campo", !creds.has("nombreUsuario") ? "nombreUsuario" : "password");
                    errorData.put("motivo", "Campo requerido");
                    return new DTOResponse("authenticateUser", "error", "Datos incompletos", gson.toJsonTree(errorData));
                }

                String email = creds.get("nombreUsuario").getAsString();
                String password = creds.get("password").getAsString();

                // Validar formato básico
                if (email == null || email.trim().isEmpty()) {
                    Map<String, Object> errorData = new HashMap<>();
                    errorData.put("campo", "nombreUsuario");
                    errorData.put("motivo", "El email no puede estar vacío");
                    return new DTOResponse("authenticateUser", "error", "Email inválido", gson.toJsonTree(errorData));
                }

                if (password == null || password.trim().isEmpty()) {
                    Map<String, Object> errorData = new HashMap<>();
                    errorData.put("campo", "password");
                    errorData.put("motivo", "La password no puede estar vacía");
                    return new DTOResponse("authenticateUser", "error", "password inválida", gson.toJsonTree(errorData));
                }

                // Buscar usuario en BD
                Usuario usuario = repoUsuario.buscarPorEmail(email);

                if (usuario == null) {
                    Map<String, Object> errorData = new HashMap<>();
                    errorData.put("campo", "nombreUsuario");
                    errorData.put("motivo", "Usuario no encontrado");
                    LoggerCentral.warn(TAG, "Intento de login con email no registrado: " + email);
                    return new DTOResponse("authenticateUser", "error", "Credenciales incorrectas", gson.toJsonTree(errorData));
                }

                // Verificar contraseña (IMPORTANTE: implementar hash en producción)
                if (!verificarContrasena(password, usuario.getContrasena())) {
                    Map<String, Object> errorData = new HashMap<>();
                    errorData.put("campo", "password");
                    errorData.put("motivo", "password incorrecta");
                    LoggerCentral.warn(TAG, "Intento de login fallido para: " + email);
                    return new DTOResponse("authenticateUser", "error", "Credenciales incorrectas", gson.toJsonTree(errorData));
                }

                // Actualizar estado a ONLINE
                usuario.setEstado(Usuario.Estado.ONLINE);
                boolean estadoActualizado = repoUsuario.actualizarEstado(UUID.fromString(usuario.getId()), Usuario.Estado.ONLINE);

                // Vincular sesión con usuario autenticado
                gestor.registrarUsuarioEnSesion(idSesion, usuario.getId());

                // ✅ ACTIVAR SINCRONIZACIÓN P2P (similar a ServicioArchivos)
                if (estadoActualizado && servicioSync != null) {
                    LoggerCentral.info(TAG, "🔄 Activando sincronización P2P para cambio de estado: " + email + " -> ONLINE");
                    servicioSync.onBaseDeDatosCambio(); // Reconstruir Merkle Tree
                    servicioSync.forzarSincronizacion(); // Sincronizar con peers
                } else if (servicioSync == null) {
                    LoggerCentral.warn(TAG, "⚠️ ServicioSync es NULL, no se puede sincronizar");
                }

                // ✅ NOTIFICAR A OBSERVADORES (Para actualizar UI)
                notificarObservadores("USUARIO_AUTENTICADO", usuario.getId());
                notificarObservadores("USUARIO_ONLINE", usuario.getId());

                // ✅ NUEVO: Enviar señal SIGNAL_UPDATE a TODOS los clientes para que actualicen
                enviarSignalUpdateATodos("USUARIO_ONLINE");

                // Construir respuesta con datos del usuario
                // IMPORTANTE: Usar LinkedHashMap para mantener el orden de los campos
                Map<String, Object> userData = new java.util.LinkedHashMap<>();

                // Asegurar que photoIdServidor siempre esté presente (aunque sea vacío)
                String fotoId = usuario.getFoto() != null ? usuario.getFoto() : "";

                // Orden específico esperado por el cliente
                // El cliente espera "id" no "idUsuario"
                userData.put("id", usuario.getId());
                userData.put("nombre", usuario.getNombre());
                userData.put("email", usuario.getEmail());
                userData.put("photoIdServidor", fotoId);
                userData.put("estado", usuario.getEstado().name());
                userData.put("peerPadre", usuario.getPeerPadre() != null ? usuario.getPeerPadre().toString() : "");

                LoggerCentral.debug(TAG, "Foto del usuario '" + email + "': " + (usuario.getFoto() != null ? usuario.getFoto() : "NULL"));
                LoggerCentral.debug(TAG, "userData completo antes de serializar: " + userData.toString());
                LoggerCentral.info(TAG, "Usuario autenticado exitosamente: " + email + " (ID: " + usuario.getId() + ")");

                // Log del JSON final que se envía
                String jsonFinal = gson.toJson(userData);
                LoggerCentral.info(TAG, "📤 JSON enviado al cliente: " + jsonFinal);

                return new DTOResponse("authenticateUser", "success", "Bienvenido", gson.toJsonTree(userData));

            } catch (Exception e) {
                LoggerCentral.error(TAG, "Error procesando autenticación: " + e.getMessage());
                e.printStackTrace();
                return new DTOResponse("authenticateUser", "error", "Error interno del servidor", null);
            }
        });

        // RUTA: logout
        router.registrarAccion("logout", (datos, idSesion) -> {
            try {
                String idUsuario = gestor.obtenerUsuarioDeSesion(idSesion);
                boolean estadoActualizado = false;
                String emailUsuario = null;

                if (idUsuario != null) {
                    // Actualizar estado a OFFLINE
                    Usuario usuario = repoUsuario.buscarPorId(UUID.fromString(idUsuario));
                    if (usuario != null) {
                        estadoActualizado = repoUsuario.actualizarEstado(UUID.fromString(usuario.getId()), Usuario.Estado.OFFLINE);
                        emailUsuario = usuario.getEmail();
                        LoggerCentral.info(TAG, "Usuario desconectado: " + emailUsuario);
                    }

                    // Desvincular sesión
                    gestor.desregistrarUsuarioEnSesion(idSesion);

                    // ✅ ACTIVAR SINCRONIZACIÓN P2P (similar a ServicioArchivos)
                    if (estadoActualizado && servicioSync != null) {
                        LoggerCentral.info(TAG, "🔄 Activando sincronización P2P para cambio de estado: " + emailUsuario + " -> OFFLINE");
                        servicioSync.onBaseDeDatosCambio(); // Reconstruir Merkle Tree
                        servicioSync.forzarSincronizacion(); // Sincronizar con peers
                    }

                    // ✅ NOTIFICAR A OBSERVADORES (Para actualizar UI)
                    notificarObservadores("USUARIO_DESCONECTADO", idUsuario);
                    notificarObservadores("USUARIO_OFFLINE", idUsuario);

                    // ✅ NUEVO: Enviar señal SIGNAL_UPDATE a TODOS los clientes
                    enviarSignalUpdateATodos("USUARIO_OFFLINE");
                }

                return new DTOResponse("logout", "success", "Sesión cerrada", null);
            } catch (Exception e) {
                LoggerCentral.error(TAG, "Error en logout: " + e.getMessage());
                return new DTOResponse("logout", "error", "Error cerrando sesión", null);
            }
        });

        // ✅ NUEVA RUTA: logoutUser (El cliente usa esta acción)
        router.registrarAccion("logoutUser", (datos, idSesion) -> {
            try {
                JsonObject payload = datos.getAsJsonObject();

                if (!payload.has("userId")) {
                    return new DTOResponse("logoutUser", "error", "userId requerido", null);
                }

                String userId = payload.get("userId").getAsString();
                boolean estadoActualizado = false;
                String emailUsuario = null;

                // Actualizar estado a OFFLINE
                Usuario usuario = repoUsuario.buscarPorId(UUID.fromString(userId));
                if (usuario != null) {
                    estadoActualizado = repoUsuario.actualizarEstado(UUID.fromString(usuario.getId()), Usuario.Estado.OFFLINE);
                    emailUsuario = usuario.getEmail();
                    LoggerCentral.info(TAG, "✅ Usuario cerró sesión: " + emailUsuario + " (ID: " + userId + ")");

                    // Desvincular sesión
                    gestor.desregistrarUsuarioEnSesion(idSesion);

                    // ✅ ACTIVAR SINCRONIZACIÓN P2P
                    if (estadoActualizado && servicioSync != null) {
                        LoggerCentral.info(TAG, "🔄 Activando sincronización P2P para cambio de estado: " + emailUsuario + " -> OFFLINE");
                        servicioSync.onBaseDeDatosCambio(); // Reconstruir Merkle Tree
                        servicioSync.forzarSincronizacion(); // Sincronizar con peers
                    } else if (servicioSync == null) {
                        LoggerCentral.warn(TAG, "⚠️ ServicioSync es NULL, no se puede sincronizar");
                    }

                    // ✅ NOTIFICAR A OBSERVADORES (Para actualizar UI)
                    notificarObservadores("USUARIO_DESCONECTADO", userId);
                    notificarObservadores("USUARIO_OFFLINE", userId);

                    // ✅ NUEVO: Enviar señal SIGNAL_UPDATE a TODOS los clientes
                    enviarSignalUpdateATodos("USUARIO_OFFLINE");

                    return new DTOResponse("logoutUser", "success", "Sesión cerrada exitosamente", null);
                } else {
                    LoggerCentral.warn(TAG, "⚠️ Usuario no encontrado para logout: " + userId);
                    return new DTOResponse("logoutUser", "error", "Usuario no encontrado", null);
                }
            } catch (Exception e) {
                LoggerCentral.error(TAG, "Error en logoutUser: " + e.getMessage());
                e.printStackTrace();
                return new DTOResponse("logoutUser", "error", "Error cerrando sesión", null);
            }
        });

        LoggerCentral.info(TAG, "Servicio de autenticación inicializado con rutas: authenticateUser, logout, logoutUser");
    }

    /**
     * Verifica si la contraseña proporcionada coincide con la almacenada.
     * NOTA: En producción, implementar con BCrypt o similar para hash seguro.
     */
    private boolean verificarContrasena(String passwordPlano, String passwordAlmacenado) {
        // TODO: Implementar hash con BCrypt en producción
        // Por ahora, comparación directa (solo para desarrollo)
        return passwordPlano.equals(passwordAlmacenado);
    }

    @Override
    public void iniciar() {
        LoggerCentral.info(TAG, "Servicio de autenticación iniciado");
    }

    @Override
    public void detener() {
        LoggerCentral.info(TAG, "Servicio de autenticación detenido");
    }

    @Override
    public void registrarObservador(IObservador observador) {
        if (!observadores.contains(observador)) {
            observadores.add(observador);
            LoggerCentral.debug(TAG, "Observador registrado: " + observador.getClass().getSimpleName());
        }
    }

    @Override
    public void removerObservador(IObservador observador) {
        observadores.remove(observador);
        LoggerCentral.debug(TAG, "Observador eliminado: " + observador.getClass().getSimpleName());
    }

    @Override
    public void notificarObservadores(String tipo, Object datos) {
        LoggerCentral.debug(TAG, "Notificando a " + observadores.size() + " observadores: " + tipo);
        for (IObservador obs : observadores) {
            obs.actualizar(tipo, datos);
        }
    }

    /**
     * ✅ NUEVO: Envía señal SIGNAL_UPDATE a todos los clientes conectados
     * Esta señal indica a los clientes que deben actualizar todos sus componentes:
     * - Lista de contactos
     * - Lista de canales
     * - Mensajes privados
     * - Mensajes de canales
     *
     * @param resource Recurso que cambió (ej: "USUARIO_ONLINE", "USUARIO_OFFLINE", "NUEVO_MENSAJE")
     */
    private void enviarSignalUpdateATodos(String resource) {
        try {
            LoggerCentral.info(TAG, "📡 Enviando SIGNAL_UPDATE a todos los clientes - Resource: " + resource);

            // Construir mensaje de señal
            Map<String, Object> signalData = new HashMap<>();
            signalData.put("type", "SIGNAL_UPDATE");
            signalData.put("resource", resource);

            String mensajeJson = gson.toJson(signalData);

            // Enviar a todos los clientes conectados usando broadcast
            gestor.broadcast(mensajeJson);

            LoggerCentral.info(TAG, "✅ SIGNAL_UPDATE enviada a todos los clientes conectados");

        } catch (Exception e) {
            LoggerCentral.error(TAG, "❌ Error enviando SIGNAL_UPDATE: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
