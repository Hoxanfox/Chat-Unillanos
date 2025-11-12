package com.arquitectura.controlador.servidor;

import com.arquitectura.DTO.Comunicacion.DTORequest;
import com.arquitectura.DTO.usuarios.LoginRequestDto;
import com.arquitectura.DTO.usuarios.UserRegistrationRequestDto;
import com.arquitectura.DTO.usuarios.UserResponseDto;
import com.arquitectura.controlador.IClientHandler;
import com.arquitectura.controlador.peer.IContactListBroadcaster;
import com.arquitectura.events.ContactListUpdateEvent;
import com.arquitectura.fachada.IChatFachada;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Controlador para operaciones relacionadas con usuarios:
 * - Autenticación
 * - Registro
 * - Logout
 * - Listar contactos
 */
@Component
public class UserController extends BaseController {
    
    private static final Set<String> SUPPORTED_ACTIONS = Set.of(
        "authenticateuser",
        "registeruser",
        "logoutuser",
        "listarcontactos"
    );
    
    private final IContactListBroadcaster contactListBroadcaster;

    @Autowired
    public UserController(IChatFachada chatFachada, Gson gson, @Lazy IContactListBroadcaster contactListBroadcaster) {
        super(chatFachada, gson);
        this.contactListBroadcaster = contactListBroadcaster;
    }
    
    @Override
    public boolean handleAction(String action, DTORequest request, IClientHandler handler) {
        if (!SUPPORTED_ACTIONS.contains(action.toLowerCase())) {
            return false;
        }
        
        switch (action.toLowerCase()) {
            case "authenticateuser":
                handleAuthenticate(request, handler);
                break;
            case "registeruser":
                handleRegister(request, handler);
                break;
            case "logoutuser":
                handleLogout(request, handler);
                break;
            case "listarcontactos":
                handleListContacts(request, handler);
                break;
            default:
                return false;
        }
        
        return true;
    }
    
    @Override
    public Set<String> getSupportedActions() {
        return SUPPORTED_ACTIONS;
    }
    
    private void handleAuthenticate(DTORequest request, IClientHandler handler) {
        if (!validatePayload(request.getPayload(), handler, "authenticateUser")) {
            return;
        }

        try {
            JsonObject dataJson = gson.toJsonTree(request.getPayload()).getAsJsonObject();
            String nombreUsuario = dataJson.has("nombreUsuario") ? dataJson.get("nombreUsuario").getAsString() : null;
            String password = dataJson.has("password") ? dataJson.get("password").getAsString() : null;

            if (nombreUsuario == null || password == null) {
                sendJsonResponse(handler, "authenticateUser", false, "usuario o contraseña inválidos",
                    createErrorData("nombreUsuario", "El campo nombreUsuario es requerido"));
                return;
            }

            LoginRequestDto serverLoginDto = new LoginRequestDto(nombreUsuario, password);
            UserResponseDto userDto = chatFachada.usuarios().autenticarUsuario(serverLoginDto, handler.getClientIpAddress());

            handler.setAuthenticatedUser(userDto);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("userId", userDto.getUserId().toString());
            responseData.put("nombre", userDto.getUsername());
            responseData.put("email", userDto.getEmail());
            responseData.put("fileId", userDto.getPhotoAddress());

            sendJsonResponse(handler, "authenticateUser", true, "Autenticación exitosa", responseData);

            // 🔔 NOTIFICAR A TODOS LOS CLIENTES que deben actualizar su lista de contactos
            System.out.println("🔔 [UserController] Usuario autenticado: " + userDto.getUsername() + ". Enviando notificación push a todos los clientes...");
            broadcastContactListToAllClients();
            try {
                // Obtener información del peer actual
                UUID peerActualId = chatFachada.p2p().obtenerPeerActualId();
                com.arquitectura.DTO.p2p.PeerResponseDto peerActual = chatFachada.p2p().obtenerPeer(peerActualId);
                
                // Enviar notificación PUSH a todos los peers
                chatFachada.p2p().notificarCambioUsuarioATodosLosPeers(
                    userDto.getUserId(),
                    userDto.getUsername(),
                    "ONLINE",
                    peerActualId,
                    peerActual.getIp(),
                    peerActual.getPuerto()
                );
                System.out.println("✓ [UserController] Notificación PUSH enviada a todos los peers");
            } catch (Exception ex) {
                System.err.println("⚠ [UserController] Error al enviar notificación PUSH: " + ex.getMessage());
                // No fallar la autenticación por error en notificación
            }

        } catch (Exception e) {
            System.err.println("Error en autenticación: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(handler, "authenticateUser", false, "Error interno del servidor", null);
        }
    }
    
    private void handleRegister(DTORequest request, IClientHandler handler) {
        if (!validatePayload(request.getPayload(), handler, "registerUser")) {
            return;
        }

        try {
            JsonObject registerJson = gson.toJsonTree(request.getPayload()).getAsJsonObject();
            String regUsername = registerJson.has("username") ? registerJson.get("username").getAsString() : null;
            String regEmail = registerJson.has("email") ? registerJson.get("email").getAsString() : null;
            String regPassword = registerJson.has("password") ? registerJson.get("password").getAsString() : null;
            String regPhotoFileId = registerJson.has("photoFileId") ? registerJson.get("photoFileId").getAsString() : null;

            if (regUsername == null || regUsername.trim().isEmpty()) {
                sendJsonResponse(handler, "registerUser", false, "El nombre de usuario es requerido",
                    createErrorData("username", "Campo requerido"));
                return;
            }

            if (regEmail == null || regEmail.trim().isEmpty()) {
                sendJsonResponse(handler, "registerUser", false, "El email es requerido",
                    createErrorData("email", "Campo requerido"));
                return;
            }

            if (regPassword == null || regPassword.trim().isEmpty()) {
                sendJsonResponse(handler, "registerUser", false, "La contraseña es requerida",
                    createErrorData("password", "Campo requerido"));
                return;
            }

            if (!regEmail.contains("@") || !regEmail.contains(".")) {
                sendJsonResponse(handler, "registerUser", false, "Formato de email inválido",
                    createErrorData("email", "Formato inválido"));
                return;
            }

            if (regPassword.length() < 6) {
                sendJsonResponse(handler, "registerUser", false, "La contraseña debe tener al menos 6 caracteres",
                    createErrorData("password", "Mínimo 6 caracteres"));
                return;
            }

            UserRegistrationRequestDto registrationDto = new UserRegistrationRequestDto(
                regUsername,
                regEmail,
                regPassword,
                regPhotoFileId
            );

            chatFachada.usuarios().registrarUsuario(registrationDto, handler.getClientIpAddress());

            Map<String, Object> registerResponseData = new HashMap<>();
            registerResponseData.put("username", regUsername);
            registerResponseData.put("email", regEmail);
            registerResponseData.put("message", "Usuario registrado exitosamente. Ahora puedes iniciar sesión.");

            sendJsonResponse(handler, "registerUser", true, "Registro exitoso", registerResponseData);

        } catch (IllegalArgumentException e) {
            String errorMessage = e.getMessage();
            String campo = "general";
            
            if (errorMessage.contains("email")) {
                campo = "email";
            } else if (errorMessage.contains("username") || errorMessage.contains("usuario")) {
                campo = "username";
            }
            
            sendJsonResponse(handler, "registerUser", false, errorMessage,
                createErrorData(campo, errorMessage));
                
        } catch (Exception e) {
            System.err.println("Error al registrar usuario: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(handler, "registerUser", false, "Error interno del servidor al registrar usuario", null);
        }
    }
    
    private void handleLogout(DTORequest request, IClientHandler handler) {
        if (!validatePayload(request.getPayload(), handler, "logoutUser")) {
            return;
        }

        try {
            JsonObject logoutDataJson = gson.toJsonTree(request.getPayload()).getAsJsonObject();
            String userIdStr = logoutDataJson.has("userId") ? logoutDataJson.get("userId").getAsString() : null;

            if (userIdStr == null || userIdStr.isEmpty()) {
                sendJsonResponse(handler, "logoutUser", false, "Error al cerrar sesión: userId requerido", null);
                return;
            }

            UUID userId = UUID.fromString(userIdStr);

            if (!handler.getAuthenticatedUser().getUserId().equals(userId)) {
                sendJsonResponse(handler, "logoutUser", false, "Usuario no autenticado o token inválido", null);
                return;
            }

            chatFachada.usuarios().cambiarEstadoUsuario(userId, false);
            
            // Obtener información del usuario antes de cerrar sesión
            String username = handler.getAuthenticatedUser().getUsername();
            
            handler.clearAuthenticatedUser();

            sendJsonResponse(handler, "logoutUser", true, "Sesión cerrada exitosamente", null);

            // 🔔 NOTIFICAR A TODOS LOS PEERS (PUSH) sobre el cambio de estado del usuario
            System.out.println("🔔 [UserController] Usuario desconectado: " + username + ". Enviando notificación PUSH a todos los peers...");
            broadcastContactListToAllClients();
            try {
                // Enviar notificación PUSH a todos los peers (estado OFFLINE)
                chatFachada.p2p().notificarCambioUsuarioATodosLosPeers(
                    userId,
                    username,
                    "OFFLINE",
                    null,  // Sin peer al desconectarse
                    null,  // Sin IP
                    null   // Sin puerto
                );
                System.out.println("✓ [UserController] Notificación PUSH de logout enviada a todos los peers");
            } catch (Exception ex) {
                System.err.println("⚠ [UserController] Error al enviar notificación PUSH: " + ex.getMessage());
                // No fallar el logout por error en notificación
            }

        } catch (IllegalArgumentException e) {
            sendJsonResponse(handler, "logoutUser", false, "Error al cerrar sesión: userId inválido", null);
        } catch (Exception e) {
            System.err.println("Error en logout: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(handler, "logoutUser", false, "Error interno del servidor", null);
        }
    }
    
    private void handleListContacts(DTORequest request, IClientHandler handler) {
        if (!validatePayload(request.getPayload(), handler, "listarContactos")) {
            return;
        }

        try {
            JsonObject listarContactosJson = gson.toJsonTree(request.getPayload()).getAsJsonObject();
            String usuarioIdStr = listarContactosJson.has("usuarioId") ? listarContactosJson.get("usuarioId").getAsString() : null;

            if (usuarioIdStr == null || usuarioIdStr.isEmpty()) {
                sendJsonResponse(handler, "listarContactos", false, "Error al obtener contactos: usuarioId requerido", null);
                return;
            }

            UUID usuarioId = UUID.fromString(usuarioIdStr);

            if (!handler.getAuthenticatedUser().getUserId().equals(usuarioId)) {
                sendJsonResponse(handler, "listarContactos", false, "Usuario no autenticado o token inválido", null);
                return;
            }

            List<UserResponseDto> contactos = chatFachada.usuarios().listarContactos(usuarioId);

            // Construir lista plana con los campos esperados por el cliente
            List<Map<String, Object>> contactosData = new ArrayList<>();
            for (UserResponseDto contacto : contactos) {
                Map<String, Object> contactoMap = new HashMap<>();
                contactoMap.put("id", contacto.getUserId() != null ? contacto.getUserId().toString() : null);

                // peer id (si está disponible)
                contactoMap.put("peerid", contacto.getPeerId() != null ? contacto.getPeerId().toString() : null);

                contactoMap.put("nombre", contacto.getUsername());
                contactoMap.put("email", contacto.getEmail());

                // Proveer tanto imagenBase64 (si el DTO ya la trae) como imagenId (ruta/identificador en servidor)
                if (contacto.getImagenBase64() != null && !contacto.getImagenBase64().isEmpty()) {
                    contactoMap.put("imagenBase64", contacto.getImagenBase64());
                } else {
                    contactoMap.put("imagenBase64", null);
                }
                contactoMap.put("imagenId", contacto.getPhotoAddress());

                // Normalizar estado a "ONLINE" / "OFFLINE"
                String estadoRaw = contacto.getEstado();
                String conectado;
                if (estadoRaw == null) {
                    conectado = "OFFLINE";
                } else {
                    String lower = estadoRaw.trim().toLowerCase();
                    if (lower.equals("true") || lower.equals("online") || lower.equals("activo") || lower.equals("1")) {
                        conectado = "ONLINE";
                    } else {
                        conectado = "OFFLINE";
                    }
                }
                contactoMap.put("conectado", conectado);

                contactosData.add(contactoMap);
            }

            // Enviar la lista directamente en el campo data (no anidada)
            sendJsonResponse(handler, "listarContactos", true, "Contactos obtenidos exitosamente", contactosData);

        } catch (IllegalArgumentException e) {
            sendJsonResponse(handler, "listarContactos", false, "Error al obtener contactos: usuarioId inválido", null);
        } catch (Exception e) {
            System.err.println("Error al listar contactos: " + e.getMessage());
            e.printStackTrace();
            sendJsonResponse(handler, "listarContactos", false, "Error interno del servidor", null);
        }
    }

    /**
     * Obtiene la lista completa de contactos y la envía como notificación push a todos los clientes conectados
     */
    private void broadcastContactListToAllClients() {
        System.out.println("━━━━━━━━━━━ BROADCAST DE LISTA DE CONTACTOS ━━━━━━━━━━━");
        try {
            // 🔄 PASO 1: Sincronizar usuarios de peers remotos
            System.out.println("🔄 [UserController] Sincronizando usuarios de peers remotos...");
            List<Map<String, Object>> usuariosDePeers = chatFachada.p2p().sincronizarUsuariosDeTodosLosPeers();
            System.out.println("✓ [UserController] Obtenidos " + usuariosDePeers.size() + " usuarios de peers remotos");

            // 📋 PASO 2: Obtener usuarios locales
            System.out.println("📋 [UserController] Obteniendo usuarios locales...");
            List<UserResponseDto> contactosLocales = chatFachada.usuarios().obtenerTodosLosUsuarios();
            System.out.println("✓ [UserController] Obtenidos " + contactosLocales.size() + " usuarios locales de BD");

            // 🔗 PASO 3: Combinar usuarios locales con usuarios de peers (evitando duplicados)
            List<Map<String, Object>> contactosData = new ArrayList<>();
            Set<String> usuariosYaAgregados = new HashSet<>();

            // Primero agregar usuarios locales
            for (UserResponseDto contacto : contactosLocales) {
                String usuarioId = contacto.getUserId().toString();

                if (!usuariosYaAgregados.contains(usuarioId)) {
                    Map<String, Object> contactoMap = new HashMap<>();
                    contactoMap.put("id", usuarioId);
                    contactoMap.put("peerid", contacto.getPeerId() != null ? contacto.getPeerId().toString() : null);
                    contactoMap.put("nombre", contacto.getUsername());
                    contactoMap.put("email", contacto.getEmail());

                    // Determinar estado online/offline
                    String estadoRaw = contacto.getEstado();
                    String estado;
                    if (estadoRaw == null) {
                        estado = "offline";
                    } else {
                        String lower = estadoRaw.trim().toLowerCase();
                        if (lower.equals("true") || lower.equals("online") || lower.equals("activo") || lower.equals("1")) {
                            estado = "online";
                        } else {
                            estado = "offline";
                        }
                    }
                    contactoMap.put("estado", estado);
                    contactoMap.put("photoFileId", contacto.getPhotoAddress());

                    contactosData.add(contactoMap);
                    usuariosYaAgregados.add(usuarioId);
                }
            }

            System.out.println("✓ [UserController] Procesados " + contactosData.size() + " usuarios locales");

            // Luego agregar usuarios de peers remotos (solo si no están duplicados)
            int usuariosDePeresAgregados = 0;
            for (Map<String, Object> usuarioPeer : usuariosDePeers) {
                String usuarioId = (String) usuarioPeer.get("usuarioId");

                if (usuarioId != null && !usuariosYaAgregados.contains(usuarioId)) {
                    // Transformar al formato esperado por el cliente
                    Map<String, Object> contactoMap = new HashMap<>();
                    contactoMap.put("id", usuarioId);
                    contactoMap.put("peerid", usuarioPeer.get("peerId"));
                    contactoMap.put("nombre", usuarioPeer.get("username"));
                    contactoMap.put("email", null); // Los peers remotos no devuelven email por seguridad

                    // Determinar estado basado en "conectado"
                    Boolean conectado = (Boolean) usuarioPeer.get("conectado");
                    String estado = (conectado != null && conectado) ? "online" : "offline";
                    contactoMap.put("estado", estado);
                    contactoMap.put("photoFileId", null); // Los archivos se descargan bajo demanda

                    contactosData.add(contactoMap);
                    usuariosYaAgregados.add(usuarioId);
                    usuariosDePeresAgregados++;
                }
            }

            System.out.println("✓ [UserController] Agregados " + usuariosDePeresAgregados + " usuarios de peers remotos");
            System.out.println("📊 [UserController] Total usuarios combinados: " + contactosData.size());

            // 📤 PASO 4: Preparar y enviar el broadcast
            Map<String, Object> data = new HashMap<>();
            data.put("contacts", contactosData);
            data.put("total", contactosData.size());

            System.out.println("📡 [UserController] Enviando broadcast a todos los clientes conectados...");
            contactListBroadcaster.broadcastContactListUpdate(data);

            System.out.println("✅ [UserController] Notificación enviada. Total contactos: " + contactosData.size() +
                             " (Locales: " + (contactosData.size() - usuariosDePeresAgregados) +
                             ", Peers remotos: " + usuariosDePeresAgregados + ")");
            System.out.println("━━━━━━━━━━━ BROADCAST COMPLETADO ━━━━━━━━━━━");

        } catch (Exception e) {
            System.err.println("━━━━━━━━━━━ ERROR EN BROADCAST ━━━━━━━━━━━");
            System.err.println("❌ [UserController] Error al enviar broadcast de lista de contactos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Listener de eventos: Se activa cuando ocurre una actualización en la lista de contactos
     * (por ejemplo, después de una sincronización P2P)
     */
    @EventListener
    public void handleContactListUpdateEvent(ContactListUpdateEvent event) {
        System.out.println("🔔 [UserController] Evento ContactListUpdateEvent recibido desde: " + event.getSource().getClass().getSimpleName());
        System.out.println("→ [UserController] Activando broadcast automático de lista de contactos...");

        // Llamar al método que hace el broadcast
        broadcastContactListToAllClients();
    }
}
