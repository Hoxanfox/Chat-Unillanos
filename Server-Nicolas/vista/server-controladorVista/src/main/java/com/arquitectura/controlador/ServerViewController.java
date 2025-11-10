package com.arquitectura.controlador;

import com.arquitectura.DTO.Mensajes.TranscriptionResponseDto;
import com.arquitectura.DTO.canales.ChannelResponseDto;
import com.arquitectura.DTO.usuarios.UserRegistrationRequestDto;
import com.arquitectura.DTO.usuarios.UserResponseDto;
import com.arquitectura.events.ForceDisconnectEvent;
import com.arquitectura.fachada.IChatFachada;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class ServerViewController {

    private final IChatFachada chatFachada;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public ServerViewController(IChatFachada chatFachada, ApplicationEventPublisher eventPublisher) {
        this.chatFachada = chatFachada;
        this.eventPublisher = eventPublisher;
    }

    // metodos registro y mensaje

    public List<UserResponseDto> obtenerUsuariosRegistrados() {
        return chatFachada.usuarios().obtenerTodosLosUsuarios();
    }

//    public void enviarMensajeBroadcast(String contenido) {
//        final int ADMIN_ID = 1; // Asumimos que el admin tiene el userId = 1
//        try {
//            chatFachada.enviarMensajeBroadcast(contenido, ADMIN_ID);
//        } catch (Exception e) {
//            e.printStackTrace(); // En una app real, usaríamos un logger
//        }
//    }

    public void registrarNuevoUsuario(UserRegistrationRequestDto requestDto) throws Exception {
        // La IP para un registro desde el servidor puede ser "localhost"
        chatFachada.usuarios().registrarUsuario(requestDto, "127.0.0.1");
    }
    // MÉTODOS PARA LOS INFORMES

    public Map<ChannelResponseDto, List<UserResponseDto>> obtenerCanalesConMiembros() {
        return chatFachada.canales().obtenerCanalesConMiembros();
    }
    public String getLogContents() {
        try {
            return chatFachada.sistema().getLogContents();
        } catch (IOException e) {
            return "Error al acceder a los logs: " + e.getMessage();
        }
    }
    public List<TranscriptionResponseDto> obtenerTranscripciones() {
        return chatFachada.mensajes().obtenerTranscripciones();
    }

    public void disconnectUser(UUID userId) {
        eventPublisher.publishEvent(new ForceDisconnectEvent(this, userId));
    }
    public List<UserResponseDto> obtenerUsuariosConectados() {
        return chatFachada.usuarios().obtenerUsuariosConectados();
    }

    // MÉTODOS P2P
    
    public Map<String, Object> listarPeersDisponibles() {
        try {
            List<com.arquitectura.DTO.p2p.PeerResponseDto> peers = chatFachada.p2p().listarPeersDisponibles();
            
            java.util.List<java.util.Map<String, Object>> peersData = new java.util.ArrayList<>();
            for (com.arquitectura.DTO.p2p.PeerResponseDto peer : peers) {
                java.util.Map<String, Object> peerMap = new java.util.HashMap<>();
                peerMap.put("peerId", peer.getPeerId().toString());
                peerMap.put("ip", peer.getIp());
                peerMap.put("puerto", peer.getPuerto());
                peerMap.put("conectado", peer.getConectado());
                peersData.add(peerMap);
            }
            
            return java.util.Map.of("peers", peersData);
        } catch (Exception e) {
            e.printStackTrace();
            return java.util.Map.of("peers", java.util.List.of());
        }
    }
    
    public Map<String, Object> obtenerEstadoRed(boolean incluirDetalles) {
        try {
            // Obtener peers
            List<com.arquitectura.DTO.p2p.PeerResponseDto> peers = chatFachada.p2p().listarPeersDisponibles();
            
            int peersOnline = 0;
            int peersOffline = 0;
            java.util.List<java.util.Map<String, Object>> peersData = new java.util.ArrayList<>();
            
            for (com.arquitectura.DTO.p2p.PeerResponseDto peer : peers) {
                if ("ONLINE".equalsIgnoreCase(peer.getConectado())) {
                    peersOnline++;
                } else {
                    peersOffline++;
                }
                
                if (incluirDetalles) {
                    java.util.Map<String, Object> peerMap = new java.util.HashMap<>();
                    peerMap.put("peerId", peer.getPeerId().toString());
                    peerMap.put("ip", peer.getIp());
                    peerMap.put("puerto", peer.getPuerto());
                    peerMap.put("estado", peer.getConectado());
                    peerMap.put("usuariosConectados", 0); // Por defecto
                    peersData.add(peerMap);
                }
            }
            
            java.util.Map<String, Object> topologia = new java.util.HashMap<>();
            topologia.put("totalPeers", peers.size());
            topologia.put("peersOnline", peersOnline);
            topologia.put("peersOffline", peersOffline);
            if (incluirDetalles) {
                topologia.put("peers", peersData);
            }
            
            // Obtener usuarios
            List<UserResponseDto> usuarios = chatFachada.usuarios().obtenerTodosLosUsuarios();
            int usuariosConectados = (int) usuarios.stream()
                .filter(u -> "ONLINE".equalsIgnoreCase(u.getEstado()))
                .count();
            
            java.util.Map<String, Object> usuariosData = new java.util.HashMap<>();
            usuariosData.put("totalUsuarios", usuarios.size());
            usuariosData.put("usuariosConectados", usuariosConectados);
            usuariosData.put("usuariosOffline", usuarios.size() - usuariosConectados);
            
            if (incluirDetalles) {
                // Aquí se podría agregar distribución por peer
                usuariosData.put("distribucion", new java.util.ArrayList<>());
            }
            
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("topologia", topologia);
            result.put("usuarios", usuariosData);
            result.put("fechaConsulta", java.time.LocalDateTime.now().toString());
            
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return java.util.Map.of();
        }
    }
    
    public Map<String, Object> pingPeer(String peerId) {
        try {
            // Aquí implementarías la lógica de ping a un peer específico
            // Por ahora retornamos un resultado simulado
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("peerId", peerId);
            result.put("success", true);
            result.put("latencia", 50); // ms
            result.put("timestamp", java.time.LocalDateTime.now().toString());
            return result;
        } catch (Exception e) {
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("peerId", peerId);
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }
    
    public java.util.List<Map<String, Object>> pingAllPeers() {
        try {
            List<com.arquitectura.DTO.p2p.PeerResponseDto> peers = chatFachada.p2p().listarPeersDisponibles();
            java.util.List<Map<String, Object>> results = new java.util.ArrayList<>();
            
            for (com.arquitectura.DTO.p2p.PeerResponseDto peer : peers) {
                if ("ONLINE".equalsIgnoreCase(peer.getConectado())) {
                    Map<String, Object> pingResult = pingPeer(peer.getPeerId().toString());
                    results.add(pingResult);
                }
            }
            
            return results;
        } catch (Exception e) {
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }
    
    public Map<String, Object> sincronizarUsuarios() {
        try {
            System.out.println("→ [ServerViewController] Iniciando sincronización P2P de usuarios...");
            
            // PASO 1: Obtener usuarios LOCALES
            List<UserResponseDto> usuariosLocales = chatFachada.usuarios().obtenerTodosLosUsuarios();
            
            // Usar un Map para evitar duplicados (por usuarioId)
            Map<String, Map<String, Object>> mapaUsuarios = new java.util.HashMap<>();
            int usuariosConectados = 0;
            
            // Obtener información del peer actual
            UUID peerActualId = chatFachada.p2p().obtenerPeerActualId();
            com.arquitectura.DTO.p2p.PeerResponseDto peerActual = chatFachada.p2p().obtenerPeer(peerActualId);
            
            System.out.println("→ [ServerViewController] Procesando " + usuariosLocales.size() + " usuarios locales");
            
            // Agregar usuarios locales al mapa
            for (UserResponseDto usuario : usuariosLocales) {
                Map<String, Object> usuarioMap = new java.util.HashMap<>();
                usuarioMap.put("usuarioId", usuario.getUserId().toString());
                usuarioMap.put("username", usuario.getUsername());
                
                boolean conectado = "ONLINE".equalsIgnoreCase(usuario.getEstado());
                usuarioMap.put("conectado", conectado);
                
                if (conectado) {
                    usuariosConectados++;
                    usuarioMap.put("peerId", peerActual.getPeerId().toString());
                    usuarioMap.put("peerIp", peerActual.getIp());
                    usuarioMap.put("peerPuerto", peerActual.getPuerto());
                } else {
                    usuarioMap.put("peerId", null);
                    usuarioMap.put("peerIp", null);
                    usuarioMap.put("peerPuerto", null);
                }
                
                mapaUsuarios.put(usuario.getUserId().toString(), usuarioMap);
            }
            
            // PASO 2: Consultar usuarios de OTROS PEERS P2P (todos los disponibles, no solo activos)
            // Usamos listarPeersDisponibles() en lugar de listarPeersActivos() porque las conexiones
            // P2P son efímeras (se abren/cierran por cada petición) usando retransmitirPeticion
            List<com.arquitectura.DTO.p2p.PeerResponseDto> peersDisponibles = chatFachada.p2p().listarPeersDisponibles();

            // Filtrar para obtener solo los peers que NO son el actual
            List<com.arquitectura.DTO.p2p.PeerResponseDto> otrosPeers = peersDisponibles.stream()
                .filter(peer -> !peer.getPeerId().equals(peerActualId))
                .collect(java.util.stream.Collectors.toList());

            System.out.println("→ [ServerViewController] Consultando " + otrosPeers.size() + " peers en la red P2P");

            for (com.arquitectura.DTO.p2p.PeerResponseDto peer : otrosPeers) {
                try {
                    System.out.println("  → Consultando peer P2P: " + peer.getIp() + ":" + peer.getPuerto());
                    
                    // Crear petición para sincronizar usuarios del peer remoto
                    Map<String, Object> requestData = new java.util.HashMap<>();
                    requestData.put("peerId", peerActualId.toString());
                    
                    com.arquitectura.DTO.Comunicacion.DTORequest sincRequest = 
                        new com.arquitectura.DTO.Comunicacion.DTORequest("sincronizarUsuarios", requestData);
                    
                    // Retransmitir petición al peer remoto via P2P
                    // retransmitirPeticion maneja conexiones efímeras (abre, envía, recibe, cierra)
                    com.arquitectura.DTO.Comunicacion.DTOResponse response =
                        chatFachada.p2p().retransmitirPeticion(peer.getPeerId(), sincRequest);
                    
                    if ("success".equals(response.getStatus()) && response.getData() != null) {
                        // Parsear respuesta del peer remoto
                        Map<String, Object> data = (Map<String, Object>) response.getData();
                        List<?> usuariosRemotos = (List<?>) data.get("usuarios");
                        
                        if (usuariosRemotos != null) {
                            System.out.println("  ✓ Recibidos " + usuariosRemotos.size() + " usuarios del peer remoto");
                            
                            for (Object usuarioObj : usuariosRemotos) {
                                if (usuarioObj instanceof Map) {
                                    Map<String, Object> usuarioRemoto = (Map<String, Object>) usuarioObj;
                                    String usuarioId = (String) usuarioRemoto.get("usuarioId");
                                    
                                    // Agregar TODOS los usuarios remotos (no solo los conectados)
                                    // Si el usuario no existe en nuestro mapa, agregarlo
                                    if (!mapaUsuarios.containsKey(usuarioId)) {
                                        mapaUsuarios.put(usuarioId, new java.util.HashMap<>(usuarioRemoto));
                                        
                                        // Incrementar contador solo si está conectado
                                        Boolean conectado = (Boolean) usuarioRemoto.get("conectado");
                                        if (conectado != null && conectado) {
                                            usuariosConectados++;
                                        }
                                    }
                                    // Si ya existe localmente, mantener la info local (prioridad local)
                                }
                            }
                        }
                    } else {
                        System.err.println("  ✗ Peer " + peer.getIp() + " respondió con error: " + response.getMessage());
                    }
                } catch (Exception e) {
                    System.err.println("  ✗ Error al consultar peer " + peer.getIp() + ":" + peer.getPuerto() + ": " + e.getMessage());
                    // Continuar con el siguiente peer
                }
            }
            
            // PASO 3: Convertir el mapa a lista
            List<Map<String, Object>> usuariosData = new java.util.ArrayList<>(mapaUsuarios.values());
            
            // Preparar respuesta
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("usuarios", usuariosData);
            result.put("totalUsuarios", usuariosData.size());
            result.put("usuariosConectados", usuariosConectados);
            result.put("fechaSincronizacion", java.time.LocalDateTime.now().toString());
            
            System.out.println("✓ [ServerViewController] Sincronización P2P completada: " + 
                usuariosData.size() + " usuarios totales (" + usuariosConectados + " conectados) de " + 
                peersDisponibles.size() + " peers");

            // 🔔 PUBLICAR EVENTO para que se notifique a todos los clientes conectados
            System.out.println("🔔 [ServerViewController] Publicando evento de actualización de contactos...");
            try {
                eventPublisher.publishEvent(new com.arquitectura.events.ContactListUpdateEvent(this));
                System.out.println("✓ [ServerViewController] Evento de actualización de contactos publicado");
            } catch (Exception eventEx) {
                System.err.println("⚠ [ServerViewController] Error al publicar evento de actualización: " + eventEx.getMessage());
            }

            return result;
        } catch (Exception e) {
            System.err.println("✗ [ServerViewController] Error al sincronizar usuarios P2P: " + e.getMessage());
            e.printStackTrace();
            return java.util.Map.of("usuarios", java.util.List.of(), "totalUsuarios", 0, "usuariosConectados", 0);
        }
    }

}
