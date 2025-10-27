package com.arquitectura.transporte;

import com.arquitectura.DTO.Mensajes.MessageResponseDto;
import com.arquitectura.controlador.IClientHandler;
import com.arquitectura.controlador.RequestDispatcher;
import com.arquitectura.events.*;
import com.arquitectura.utils.file.IFileStorageService;
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@PropertySource("file:./config/server.properties")
@Component
public class ServerListener {

    private static final Logger log = LoggerFactory.getLogger(ServerListener.class);

    @Value("${server.port}")
    private int port;
    @Value("${server.max.connections}")
    private int maxConnectedUsers;

    private final Gson gson;
    private ExecutorService clientPool;

    private final RequestDispatcher requestDispatcher;
    private final IFileStorageService fileStorageService;

    // El mapa de sesiones activas vuelve a ser responsabilidad de esta clase.
    private final Map<Integer, List<IClientHandler>> activeClientsById = Collections.synchronizedMap(new HashMap<>());

    @Autowired
    public ServerListener(Gson gson, RequestDispatcher requestDispatcher, IFileStorageService fileStorageService) {
        this.gson = gson;
        this.requestDispatcher = requestDispatcher;
        this.fileStorageService = fileStorageService;
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
                        out.println("ERROR;SERVER_FULL;El servidor ha alcanzado su capacidad máxima.");
                    }
                    clientSocket.close();
                    continue;
                }

                log.info("Nuevo cliente conectado: {}", clientSocket.getInetAddress().getHostAddress());
                // --- ¡LÍNEA CORREGIDA! ---
                // Pasamos la instancia completa de 'this' (el ServerListener)
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
        activeClientsById.values().forEach(handlerList -> {
            handlerList.forEach(handler -> handler.sendMessage(event.getFormattedMessage()));
        });
    }

    @EventListener
    public void handleNewMessageEvent(NewMessageEvent event) {
        MessageResponseDto originalDto = event.getMessageDto();
        log.info("Nuevo mensaje en canal {}. Propagando a los miembros conectados.", originalDto.getChannelId());
        List<Integer> memberIds = event.getRecipientUserIds();
        MessageResponseDto dtoParaPropagar = originalDto;
        if ("AUDIO".equals(originalDto.getMessageType())) {
            try {
                String base64Content = fileStorageService.readFileAsBase64(originalDto.getContent());
                dtoParaPropagar = new MessageResponseDto(
                        originalDto.getMessageId(),
                        originalDto.getChannelId(),
                        originalDto.getAuthor(),
                        originalDto.getTimestamp(),
                        originalDto.getMessageType(),
                        base64Content // <-- ¡Aquí va el contenido Base64!
                );
                log.info("Mensaje de audio ID {} codificado a Base64 para su propagación.", originalDto.getMessageId());

            } catch (Exception e) {
                log.error("Error al leer y codificar el archivo de audio para propagación: {}", e.getMessage());
                return;
            }
        }

        String notification = "EVENTO;NUEVO_MENSAJE;" + gson.toJson(dtoParaPropagar);

        memberIds.forEach(memberId -> {
            List<IClientHandler> userSessions = activeClientsById.get(memberId);
            if (userSessions != null) {
                userSessions.forEach(handler -> handler.sendMessage(notification));
            }
        });
    }

    @EventListener
    public void handleUserInvitedEvent(UserInvitedEvent event) {
        int invitedUserId = event.getInvitedUserId();
        log.info("Usuario {} invitado al canal '{}'. Notificando si está en línea.", invitedUserId, event.getChannelDto().getChannelName());
        List<IClientHandler> userSessions = activeClientsById.get(invitedUserId);
        if (userSessions != null && !userSessions.isEmpty()) {
            String notificationJson = gson.toJson(event.getChannelDto());
            String notification = "EVENTO;NUEVA_INVITACION;" + notificationJson;
            userSessions.forEach(handler -> handler.sendMessage(notification));
            log.info("Notificación de invitación enviada al usuario {}.", invitedUserId);
        }
    }
    @EventListener
    public void handleConnectedUsersRequest(ConnectedUsersRequestEvent event) {
        // Obtenemos la "canasta" vacía del evento y la llenamos con nuestros datos.
        event.getResponseContainer().addAll(this.activeClientsById.keySet());
    }

    // --- MÉTODOS PÚBLICOS PARA GESTIÓN DE SESIONES ---
    public void registerAuthenticatedClient(IClientHandler handler) {
        if (handler.getAuthenticatedUser() != null) {
            int userId = handler.getAuthenticatedUser().getUserId();
            activeClientsById.computeIfAbsent(userId, k -> new ArrayList<>()).add(handler);
            log.info("Cliente autenticado y registrado: UserID={}, IP={}", userId, handler.getClientIpAddress());
        }
    }


    public Set<Integer> getConnectedUserIds() {
        return new HashSet<>(activeClientsById.keySet());
    }

    public void forceDisconnectUser(int userId) {
        List<IClientHandler> userSessions = activeClientsById.get(userId);
        if (userSessions != null && !userSessions.isEmpty()) {
            log.info("Administrador forzando desconexión para el usuario ID: {}", userId);
            List<IClientHandler> sessionsToClose = new ArrayList<>(userSessions);
            sessionsToClose.forEach(IClientHandler::forceDisconnect); // Llama al método final
        } else {
            log.warn("Se intentó desconectar al usuario ID: {}, pero no se encontraron sesiones activas.", userId);
        }
    }

    // --- MÉTODO PRIVADO DE LIMPIEZA ---

    private void removeClient(IClientHandler clientHandler) {
        if (clientHandler.isAuthenticated()) {
            int userId = clientHandler.getAuthenticatedUser().getUserId();
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