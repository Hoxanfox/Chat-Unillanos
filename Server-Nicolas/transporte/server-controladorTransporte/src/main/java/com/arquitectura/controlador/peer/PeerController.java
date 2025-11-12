package com.arquitectura.controlador.peer;

import com.arquitectura.DTO.Comunicacion.DTORequest;
import com.arquitectura.controlador.IClientHandler;
import com.arquitectura.controlador.peer.handlers.*;
import com.arquitectura.controlador.servidor.BaseController;
import com.arquitectura.fachada.IChatFachada;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Controlador refactorizado para operaciones P2P (Peer-to-Peer).
 *
 * Este controlador actúa como punto de entrada para todas las operaciones P2P
 * y delega las responsabilidades específicas a handlers especializados:
 *
 * - PeerDiscoveryHandler: Descubrimiento y listado de peers
 * - PeerHeartbeatHandler: Gestión de heartbeats y verificación de conexión
 * - PeerRoutingHandler: Retransmisión de peticiones entre peers
 * - UserLocationHandler: Búsqueda de usuarios y enrutamiento de mensajes
 * - UserSyncHandler: Sincronización de usuarios y cambios de estado
 * - NetworkStateHandler: Estado de la red y sincronización de canales
 */
@Component
public class PeerController extends BaseController {
    
    private static final Set<String> SUPPORTED_ACTIONS = Set.of(
        "listarpeersdisponibles",
        "reportarlatido",
        "retransmitirpeticion",
        "buscarusuario",
        "enrutarmensaje",
        "descubrirpeers",
        "sincronizarusuarios",
        "notificarcambioestado",
        "notificacioncambiousuario",
        "verificarconexion",
        "ping",
        "obtenerestadored",
        "sincronizarcanales"
    );
    
    // Handlers especializados
    private final PeerDiscoveryHandler discoveryHandler;
    private final PeerHeartbeatHandler heartbeatHandler;
    private final PeerRoutingHandler routingHandler;
    private final UserLocationHandler locationHandler;
    private final UserSyncHandler syncHandler;
    private final NetworkStateHandler networkStateHandler;

    @Autowired
    public PeerController(
            IChatFachada chatFachada,
            Gson gson,
            PeerDiscoveryHandler discoveryHandler,
            PeerHeartbeatHandler heartbeatHandler,
            PeerRoutingHandler routingHandler,
            UserLocationHandler locationHandler,
            UserSyncHandler syncHandler,
            NetworkStateHandler networkStateHandler) {
        super(chatFachada, gson);
        this.discoveryHandler = discoveryHandler;
        this.heartbeatHandler = heartbeatHandler;
        this.routingHandler = routingHandler;
        this.locationHandler = locationHandler;
        this.syncHandler = syncHandler;
        this.networkStateHandler = networkStateHandler;
    }
    
    @Override
    public boolean handleAction(String action, DTORequest request, IClientHandler handler) {
        String actionLower = action.toLowerCase();
        
        if (!SUPPORTED_ACTIONS.contains(actionLower)) {
            return false;
        }
        
        System.out.println("→ [PeerController] Manejando acción P2P: " + action);

        // CRÍTICO: Reportar heartbeat del peer que envía la petición
        reportarHeartbeatDePeticion(request, handler);

        // Delegar a los handlers especializados
        switch (actionLower) {
            case "listarpeersdisponibles":
                discoveryHandler.handleListarPeersDisponibles(request, handler);
                break;

            case "descubrirpeers":
                discoveryHandler.handleDescubrirPeers(request, handler);
                break;

            case "reportarlatido":
                heartbeatHandler.handleReportarLatido(request, handler);
                break;

            case "verificarconexion":
            case "ping":
                heartbeatHandler.handleVerificarConexion(request, handler);
                break;

            case "retransmitirpeticion":
                routingHandler.handleRetransmitirPeticion(request, handler);
                break;

            case "buscarusuario":
                locationHandler.handleBuscarUsuario(request, handler);
                break;

            case "enrutarmensaje":
                locationHandler.handleEnrutarMensaje(request, handler);
                break;

            case "sincronizarusuarios":
                syncHandler.handleSincronizarUsuarios(request, handler);
                break;

            case "notificarcambioestado":
                syncHandler.handleNotificarCambioEstado(request, handler);
                break;

            case "notificacioncambiousuario":
                syncHandler.handleNotificacionCambioUsuario(request, handler);
                break;

            case "obtenerestadored":
                networkStateHandler.handleObtenerEstadoRed(request, handler);
                break;

            case "sincronizarcanales":
                networkStateHandler.handleSincronizarCanales(request, handler);
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

    /**
     * Reporta automáticamente el heartbeat del peer que envía cualquier petición P2P.
     * Esto asegura que las conexiones efímeras también actualicen el estado del peer.
     */
    private void reportarHeartbeatDePeticion(DTORequest request, IClientHandler handler) {
        try {
            if (request.getPayload() != null) {
                JsonObject payload = gson.toJsonTree(request.getPayload()).getAsJsonObject();

                // Si la petición incluye peerId, reportar heartbeat automáticamente
                if (payload.has("peerId") && !payload.get("peerId").isJsonNull()) {
                    String peerIdStr = payload.get("peerId").getAsString();
                    UUID peerId = UUID.fromString(peerIdStr);

                    // Si también incluye IP y puerto, usarlos para actualizar
                    if (payload.has("ip") && payload.has("puerto")) {
                        String ip = payload.get("ip").getAsString();
                        int puerto = payload.get("puerto").getAsInt();
                        chatFachada.p2p().reportarLatido(peerId, ip, puerto);
                        System.out.println("  ↳ [PeerController] Heartbeat automático reportado: " + peerId);
                    } else {
                        chatFachada.p2p().reportarLatido(peerId);
                        System.out.println("  ↳ [PeerController] Heartbeat automático reportado: " + peerId);
                    }
                }
            }
        } catch (Exception e) {
            // Si hay error reportando heartbeat automático, continuar silenciosamente
            System.out.println("  ⚠ [PeerController] No se pudo reportar heartbeat automático: " + e.getMessage());
        }
    }
}
