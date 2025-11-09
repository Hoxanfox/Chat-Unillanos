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
            List<UserResponseDto> usuarios = chatFachada.usuarios().obtenerTodosLosUsuarios();
            
            java.util.List<java.util.Map<String, Object>> usuariosData = new java.util.ArrayList<>();
            int usuariosConectados = 0;
            
            for (UserResponseDto usuario : usuarios) {
                java.util.Map<String, Object> usuarioMap = new java.util.HashMap<>();
                usuarioMap.put("usuarioId", usuario.getUserId().toString());
                usuarioMap.put("username", usuario.getUsername());
                
                boolean conectado = "ONLINE".equalsIgnoreCase(usuario.getEstado());
                usuarioMap.put("conectado", conectado);
                
                if (conectado) {
                    usuariosConectados++;
                    try {
                        com.arquitectura.DTO.p2p.UserLocationResponseDto ubicacion = 
                            chatFachada.p2p().buscarUsuario(usuario.getUserId());
                        
                        if (ubicacion.getPeerId() != null) {
                            usuarioMap.put("peerId", ubicacion.getPeerId().toString());
                            usuarioMap.put("peerIp", ubicacion.getPeerIp());
                            usuarioMap.put("peerPuerto", ubicacion.getPeerPuerto());
                        } else {
                            usuarioMap.put("peerId", null);
                            usuarioMap.put("peerIp", null);
                            usuarioMap.put("peerPuerto", null);
                        }
                    } catch (Exception e) {
                        usuarioMap.put("peerId", null);
                        usuarioMap.put("peerIp", null);
                        usuarioMap.put("peerPuerto", null);
                    }
                } else {
                    usuarioMap.put("peerId", null);
                    usuarioMap.put("peerIp", null);
                    usuarioMap.put("peerPuerto", null);
                }
                
                usuariosData.add(usuarioMap);
            }
            
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("usuarios", usuariosData);
            result.put("totalUsuarios", usuarios.size());
            result.put("usuariosConectados", usuariosConectados);
            result.put("fechaSincronizacion", java.time.LocalDateTime.now().toString());
            
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return java.util.Map.of("usuarios", java.util.List.of());
        }
    }

}