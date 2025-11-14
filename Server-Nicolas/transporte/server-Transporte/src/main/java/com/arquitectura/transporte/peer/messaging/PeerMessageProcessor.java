package com.arquitectura.transporte.peer.messaging;

import com.arquitectura.controlador.IClientHandler;
import com.arquitectura.controlador.RequestDispatcher;
import com.arquitectura.controlador.peer.IPeerHandler;
import com.arquitectura.DTO.Comunicacion.DTORequest;
import com.arquitectura.DTO.Comunicacion.DTOResponse;
import com.arquitectura.transporte.peer.comm.PeerCommunicator;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Procesa mensajes P2P delegando a RequestDispatcher y al communicator para envíos.
 */
public class PeerMessageProcessor {
    private static final Logger log = LoggerFactory.getLogger(PeerMessageProcessor.class);

    private final RequestDispatcher requestDispatcher;
    private final Gson gson;
    private final PeerCommunicator communicator;

    public PeerMessageProcessor(RequestDispatcher requestDispatcher, Gson gson, PeerCommunicator communicator) {
        this.requestDispatcher = requestDispatcher;
        this.gson = gson;
        this.communicator = communicator;
    }

    public void processPeerRequest(IPeerHandler handler, DTORequest request) {
        log.info("Procesando request P2P '{}' de peer {}", request.getAction(), handler.getPeerId());
        try {
            String requestJson = gson.toJson(request);
            log.debug("processPeerRequest() - request JSON to dispatch: {}", requestJson);
            requestDispatcher.dispatch(requestJson, (IClientHandler) handler);
            log.debug("Request P2P '{}' procesado exitosamente", request.getAction());
        } catch (Exception e) {
            log.error("Error procesando request P2P '{}': {}", request.getAction(), e.getMessage(), e);
            DTOResponse response = new DTOResponse(
                    request.getAction(),
                    "error",
                    "Error procesando request: " + e.getMessage(),
                    null
            );
            try {
                handler.sendMessage(gson.toJson(response));
            } catch (Exception sendEx) {
                log.warn("No se pudo enviar respuesta de error al peer {}: {}", handler.getPeerId(), sendEx.getMessage());
            }
        }
    }

    public void handleRetransmitRequest(IPeerHandler handler, DTORequest request) {
        log.info("Manejando retransmisión de peer {}", handler.getPeerId());
        // Ejemplo: delegar a evento o reenvío según payload
        try {
            if (request.getPayload() instanceof java.util.Map) {
                var map = (java.util.Map<?, ?>) request.getPayload();
                Object origin = map.get("originPeerId");
                if (origin != null) {
                    java.util.UUID originId = java.util.UUID.fromString(origin.toString());
                    // Construir DTOResponse y usar communicator
                    DTOResponse resp = new DTOResponse(request.getAction(), "success", "Retransmitido", request.getPayload());
                    communicator.sendToPeer(originId, gson.toJson(resp));
                }
            }
        } catch (Exception e) {
            log.warn("Error manejando retransmit request de {}: {}", handler.getPeerId(), e.getMessage());
        }
    }

    public void handleSyncRequest(IPeerHandler handler, DTORequest request) {
        log.info("Manejando sincronización de peer {}", handler.getPeerId());
        // Implementar lógicas de sincronización puntuales aquí
    }
}
