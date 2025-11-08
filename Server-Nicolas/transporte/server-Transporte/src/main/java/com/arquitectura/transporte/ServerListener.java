package com.arquitectura.transporte;

import com.arquitectura.DTO.Comunicacion.DTOResponse;
import com.arquitectura.DTO.Mensajes.MessageResponseDto;
import com.arquitectura.controlador.IClientHandler;
import com.arquitectura.controlador.IContactListBroadcaster;
import com.arquitectura.controlador.RequestDispatcher;
import com.arquitectura.events.*;
import com.google.gson.Gson;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@PropertySource(value = "file:./config/server.properties", ignoreResourceNotFound = true)
@Component
public class ServerListener implements IContactListBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(ServerListener.class);

    @Value("${server.port:22100}")
    private int port;
    @Value("${server.max.connections:100}")
    private int maxConnectedUsers;

    private final Gson gson;
    private ExecutorService clientPool;

    private final RequestDispatcher requestDispatcher;


    // El mapa de sesiones activas responsabilidad
    private final Map<UUID, List<IClientHandler>> activeClientsById = Collections.synchronizedMap(new HashMap<>());

    @Autowired
    public ServerListener(Gson gson, RequestDispatcher requestDispatcher) {
        this.gson = gson;
        this.requestDispatcher = requestDispatcher;
    }

    @PostConstruct
    public void init() {
        this.clientPool = Executors.newFixedThreadPool(maxConnectedUsers);
    }

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            log.info("Servidor de Chat iniciado en el puerto {} con un límite de {} conexiones.", port, maxConnectedUsers);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                int totalConnections = activeClientsById.values().stream().mapToInt(List::size).sum();

                if (totalConnections >= maxConnectedUsers) {
                    log.warn("Conexión rechazada de {}. Límite de {} conexiones alcanzado.", clientSocket.getInetAddress().getHostAddress(), maxConnectedUsers);
                    try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
                        DTOResponse errResponse = new DTOResponse("connect", "error", "El servidor ha alcanzado su capacidad máxima.", null);
                        out.println(gson.toJson(errResponse));
                    }
                    clientSocket.close();
                    continue;
                }

                log.info("Nuevo cliente conectado: {}", clientSocket.getInetAddress().getHostAddress());
                ClientHandler clientHandler = new ClientHandler(clientSocket, requestDispatcher, this, this::removeClient);
                clientPool.submit(clientHandler);
            }
        } catch (IOException e) {
            log.error("Error fatal al iniciar el servidor: {}", e.getMessage(), e);
        }
    }

    // --- MÉTODOS OBSERVADORES (EVENT LISTENERS) ---

    @EventListener
    public void handleBroadcastEvent(BroadcastMessageEvent event) {
        log.info("Evento de Broadcast recibido. Enviando a todas las sesiones activas.");
        DTOResponse response = new DTOResponse("broadcast", "success", "Mensaje global", event.getFormattedMessage());
        String notification = gson.toJson(response);
        activeClientsById.values().forEach(handlerList -> {
            handlerList.forEach(handler -> handler.sendMessage(notification));
        });
    }

    @EventListener
    public void handleNewMessageEvent(NewMessageEvent event) {
        MessageResponseDto originalDto = event.getMessageDto();
        log.info("Nuevo mensaje en canal {}. Propagando a los miembros conectados.", originalDto.getChannelId());
        List<UUID> memberIds = event.getRecipientUserIds();
        
        // Determinar el tipo de acción según el tipo de mensaje
        String action = "TEXT".equals(originalDto.getMessageType()) ? "nuevoMensajeDirecto" : "nuevoMensajeDirectoAudio";
        String message = "TEXT".equals(originalDto.getMessageType()) ? "Nuevo mensaje recibido" : "Nuevo mensaje de audio recibido";
        
        // Enriquecer el mensaje (convertir audio a base64 si es necesario)
        MessageResponseDto dtoParaPropagar = requestDispatcher.enrichOutgoingMessage(originalDto);
        if (dtoParaPropagar != originalDto) {
            log.info("Mensaje de audio ID {} codificado a Base64 para su propagación.", originalDto.getMessageId());
        }

        // Construir el objeto de datos de la notificación en el formato solicitado
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("mensajeId", dtoParaPropagar.getMessageId().toString());
        notificationData.put("remitenteId", dtoParaPropagar.getAuthor().getUserId().toString());
        notificationData.put("remitenteNombre", dtoParaPropagar.getAuthor().getUsername());
        
        // Peer IDs (si están disponibles)
        if (dtoParaPropagar.getAuthor().getPeerId() != null) {
            notificationData.put("peerRemitenteId", dtoParaPropagar.getAuthor().getPeerId().toString());
        } else {
            notificationData.put("peerRemitenteId", null);
        }
        notificationData.put("peerDestinoId", null); // TODO: Obtener del destinatario si es necesario
        
        // Tipo y contenido
        String tipo = "TEXT".equals(dtoParaPropagar.getMessageType()) ? "texto" : "audio";
        notificationData.put("tipo", tipo);
        notificationData.put("contenido", dtoParaPropagar.getContent());
        notificationData.put("fechaEnvio", dtoParaPropagar.getTimestamp().toString());

        DTOResponse response = new DTOResponse(action, "success", message, notificationData);
        String notification = gson.toJson(response);

        // Enviar a todos los destinatarios EXCEPTO al remitente
        UUID remitenteId = dtoParaPropagar.getAuthor().getUserId();
        memberIds.forEach(memberId -> {
            // Solo enviar si NO es el remitente
            if (!memberId.equals(remitenteId)) {
                notificationData.put("destinatarioId", memberId.toString());
                // Reconstruir el response con el destinatarioId actualizado
                DTOResponse recipientResponse = new DTOResponse(action, "success", message, notificationData);
                String recipientNotification = gson.toJson(recipientResponse);

                List<IClientHandler> userSessions = activeClientsById.get(memberId);
                if (userSessions != null) {
                    userSessions.forEach(handler -> handler.sendMessage(recipientNotification));
                }
            }
        });
    }

    @EventListener
    public void handleUserInvitedEvent(UserInvitedEvent event) {
        UUID invitedUserId = event.getInvitedUserId();
        log.info("Usuario {} invitado al canal '{}'. Notificando si está en línea.", invitedUserId, event.getChannelDto().getChannelName());
        List<IClientHandler> userSessions = activeClientsById.get(invitedUserId);
        if (userSessions != null && !userSessions.isEmpty()) {
            DTOResponse response = new DTOResponse("notificacionInvitacionCanal", "success", "Has sido invitado a un canal", event.getChannelDto());
            String notification = gson.toJson(response);
            userSessions.forEach(handler -> handler.sendMessage(notification));
            log.info("Notificación de invitación enviada al usuario {}.", invitedUserId);
        }
    }
    @EventListener
    public void handleConnectedUsersRequest(ConnectedUsersRequestEvent event) {
        // Obtenemos la "canasta" vacía del evento y la llenamos con nuestros datos.
        event.getResponseContainer().addAll(this.activeClientsById.keySet());
    }

    @EventListener
    public void handleContactListUpdate(ContactListUpdateEvent event) {
        log.info("Evento de actualización de lista de contactos recibido. Enviando a todos los clientes conectados.");
        // Este evento será manejado por el RequestDispatcher para construir y enviar la lista
    }

    @EventListener
    public void handleForceLogoutEvent(ForceLogoutEvent event) {
        UUID userId = event.getUserId();
        log.info("Evento de logout forzado recibido para usuario {}. Motivo: {}", userId, event.getMotivo());

        // Construir la notificación en el formato solicitado
        Map<String, Object> logoutData = new HashMap<>();
        logoutData.put("estado", "OFFLINE");
        logoutData.put("id", userId.toString());
        logoutData.put("peerId", event.getPeerId() != null ? event.getPeerId().toString() : null);

        DTOResponse response = new DTOResponse("pedirLogout", "success", "Cerrar sesión!", logoutData);
        String notification = gson.toJson(response);

        // Enviar a todas las sesiones del usuario
        List<IClientHandler> userSessions = activeClientsById.get(userId);
        if (userSessions != null && !userSessions.isEmpty()) {
            log.info("Enviando pedido de logout a {} sesiones del usuario {}", userSessions.size(), userId);
            List<IClientHandler> sessionsToNotify = new ArrayList<>(userSessions);
            sessionsToNotify.forEach(handler -> {
                handler.sendMessage(notification);
                log.info("Notificación de logout enviada a sesión de usuario {} desde IP {}", userId, handler.getClientIpAddress());
            });
        } else {
            log.warn("No se encontraron sesiones activas para el usuario {} al intentar enviar pedido de logout", userId);
        }
    }

    // --- MÉTODOS PÚBLICOS PARA GESTIÓN DE SESIONES ---
    public void registerAuthenticatedClient(IClientHandler handler) {
        if (handler.getAuthenticatedUser() != null) {
            UUID userId = handler.getAuthenticatedUser().getUserId();
            activeClientsById.computeIfAbsent(userId, k -> new ArrayList<>()).add(handler);
            log.info("Cliente autenticado y registrado: UserID={}, IP={}", userId, handler.getClientIpAddress());
        }
    }


    public Set<UUID> getConnectedUserIds() {
        return new HashSet<>(activeClientsById.keySet());
    }

    public void forceDisconnectUser(UUID userId) {
        List<IClientHandler> userSessions = activeClientsById.get(userId);
        if (userSessions != null && !userSessions.isEmpty()) {
            log.info("Administrador forzando desconexión para el usuario ID: {}", userId);
            List<IClientHandler> sessionsToClose = new ArrayList<>(userSessions);
            sessionsToClose.forEach(IClientHandler::forceDisconnect);
        } else {
            log.warn("Se intentó desconectar al usuario ID: {}, pero no se encontraron sesiones activas.", userId);
        }
    }

    /**
     * Envía la lista de contactos actualizada a TODOS los clientes conectados
     */
    public void broadcastContactListUpdate(Object contactListData) {
        log.info("📢 Broadcasting actualización de lista de contactos a {} usuarios conectados", activeClientsById.size());
        DTOResponse response = new DTOResponse("solicitarListaContactos", "success", "Lista de contactos obtenida exitosamente", contactListData);
        String notification = gson.toJson(response);

        int totalNotifications = 0;
        for (List<IClientHandler> handlerList : activeClientsById.values()) {
            for (IClientHandler handler : handlerList) {
                handler.sendMessage(notification);
                totalNotifications++;
            }
        }
        log.info("✅ Lista de contactos enviada a {} sesiones activas", totalNotifications);
    }

    // --- MÉTODO PRIVADO DE LIMPIEZA ---

    private void removeClient(IClientHandler clientHandler) {
        if (clientHandler.isAuthenticated()) {
            UUID userId = clientHandler.getAuthenticatedUser().getUserId();
            List<IClientHandler> userHandlers = activeClientsById.get(userId);
            if (userHandlers != null) {
                userHandlers.remove(clientHandler);
                log.info("Sesión eliminada para usuario {}. Sesiones restantes para este usuario: {}.", userId, userHandlers.size());
                if (userHandlers.isEmpty()) {
                    activeClientsById.remove(userId);
                    log.info("Usuario {} se ha desconectado por completo.", userId);
                }
            }
        }
        log.info("Cliente [{}] desconectado.", clientHandler.getClientIpAddress());
    }
}

