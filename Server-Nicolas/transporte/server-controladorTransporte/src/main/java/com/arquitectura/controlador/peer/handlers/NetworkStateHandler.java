package com.arquitectura.controlador.peer.handlers;

import com.arquitectura.DTO.Comunicacion.DTORequest;
import com.arquitectura.DTO.p2p.PeerResponseDto;
import com.arquitectura.controlador.IClientHandler;
import com.arquitectura.fachada.IChatFachada;
import com.google.gson.Gson;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Handler para estado de red y sincronización de canales
 */
@Component
public class NetworkStateHandler {

    private final IChatFachada chatFachada;
    private final Gson gson;
    private final PeerResponseHelper responseHelper;

    public NetworkStateHandler(IChatFachada chatFachada, Gson gson, PeerResponseHelper responseHelper) {
        this.chatFachada = chatFachada;
        this.gson = gson;
        this.responseHelper = responseHelper;
    }

    public void handleObtenerEstadoRed(DTORequest request, IClientHandler handler) {
        System.out.println("→ [NetworkStateHandler] Procesando obtenerEstadoRed");

        try {
            UUID peerActualId = chatFachada.p2p().obtenerPeerActualId();
            PeerResponseDto peerActual = chatFachada.p2p().obtenerPeer(peerActualId);
            List<PeerResponseDto> todosLosPeers = chatFachada.p2p().listarPeersDisponibles();
            List<com.arquitectura.DTO.usuarios.UserResponseDto> usuariosLocales =
                chatFachada.usuarios().obtenerTodosLosUsuarios();

            Map<String, Object> responseData = buildNetworkState(peerActual, todosLosPeers, usuariosLocales);

            System.out.println("✓ [NetworkStateHandler] Estado de red obtenido: " +
                todosLosPeers.size() + " peers, " + usuariosLocales.size() + " usuarios locales");

            responseHelper.sendSuccess(handler, "obtenerEstadoRed", "Estado de la red obtenido exitosamente", responseData);

        } catch (Exception e) {
            System.err.println("✗ [NetworkStateHandler] Error: " + e.getMessage());
            responseHelper.sendError(handler, "obtenerEstadoRed", "Error al obtener el estado de la red", null);
        }
    }

    public void handleSincronizarCanales(DTORequest request, IClientHandler handler) {
        System.out.println("→ [NetworkStateHandler] Procesando sincronizarCanales");

        try {
            List<com.arquitectura.DTO.canales.ChannelResponseDto> canalesLocales =
                chatFachada.canales().obtenerTodosLosCanales();

            List<Map<String, Object>> canalesData = convertChannelsToMapList(canalesLocales);

            String fechaSincronizacion = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("canales", canalesData);
            responseData.put("totalCanales", canalesData.size());
            responseData.put("fechaSincronizacion", fechaSincronizacion);

            System.out.println("✓ [NetworkStateHandler] Canales sincronizados: " + canalesData.size() + " canales");

            responseHelper.sendSuccess(handler, "sincronizarCanales", "Canales sincronizados exitosamente", responseData);

        } catch (Exception e) {
            System.err.println("✗ [NetworkStateHandler] Error: " + e.getMessage());
            responseHelper.sendError(handler, "sincronizarCanales", "Error al sincronizar canales: " + e.getMessage(), null);
        }
    }

    private Map<String, Object> buildNetworkState(PeerResponseDto peerActual,
                                                   List<PeerResponseDto> todosLosPeers,
                                                   List<com.arquitectura.DTO.usuarios.UserResponseDto> usuariosLocales) {
        Map<String, Object> responseData = new HashMap<>();

        // Info peer actual
        Map<String, Object> peerActualInfo = new HashMap<>();
        peerActualInfo.put("peerId", peerActual.getPeerId().toString());
        peerActualInfo.put("ip", peerActual.getIp());
        peerActualInfo.put("puerto", peerActual.getPuerto());
        peerActualInfo.put("conectado", peerActual.getConectado());
        responseData.put("peerActual", peerActualInfo);

        // Lista peers
        List<Map<String, Object>> peersData = new ArrayList<>();
        int peersActivos = 0;
        for (PeerResponseDto peer : todosLosPeers) {
            Map<String, Object> peerMap = new HashMap<>();
            peerMap.put("peerId", peer.getPeerId().toString());
            peerMap.put("ip", peer.getIp());
            peerMap.put("puerto", peer.getPuerto());
            peerMap.put("conectado", peer.getConectado());

            if ("ACTIVO".equals(peer.getConectado()) || "ONLINE".equals(peer.getConectado())) {
                peersActivos++;
            }

            peersData.add(peerMap);
        }
        responseData.put("peers", peersData);
        responseData.put("totalPeers", todosLosPeers.size());
        responseData.put("peersActivos", peersActivos);

        // Usuarios
        int usuariosConectados = 0;
        for (com.arquitectura.DTO.usuarios.UserResponseDto usuario : usuariosLocales) {
            if ("ONLINE".equalsIgnoreCase(usuario.getEstado())) {
                usuariosConectados++;
            }
        }
        responseData.put("usuariosLocales", usuariosLocales.size());
        responseData.put("usuariosConectados", usuariosConectados);

        // Timestamp
        responseData.put("timestamp", System.currentTimeMillis());
        responseData.put("fecha", java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        return responseData;
    }

    private List<Map<String, Object>> convertChannelsToMapList(
            List<com.arquitectura.DTO.canales.ChannelResponseDto> canales) {
        List<Map<String, Object>> canalesData = new ArrayList<>();

        for (com.arquitectura.DTO.canales.ChannelResponseDto canal : canales) {
            Map<String, Object> canalMap = new HashMap<>();
            canalMap.put("channelId", canal.getChannelId().toString());
            canalMap.put("nombreCanal", canal.getChannelName());
            canalMap.put("tipoCanal", canal.getChannelType());
            canalMap.put("creadorId", canal.getOwner() != null ? canal.getOwner().getUserId().toString() : null);
            canalMap.put("creadorUsername", canal.getOwner() != null ? canal.getOwner().getUsername() : null);

            canalesData.add(canalMap);
        }

        return canalesData;
    }
}
