package controlador.p2p;

import observador.IObservador;
import servicio.p2p.IP2PService;
import servicio.p2p.P2PServiceImpl;
import dto.p2p.DTOPeer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Controlador que expone operaciones para la capa de presentación relacionadas con P2P.
 * Se registra como observador para recibir notificaciones del servicio P2P y notifica a
 * listeners de alto nivel (P2PListener) para evitar que la UI dependa de la infraestructura o dominio.
 */
public class P2PController implements IObservador, IP2PController {

    private final IP2PService service;
    private final List<P2PListener> listeners = Collections.synchronizedList(new ArrayList<>());

    public P2PController() {
        this.service = new P2PServiceImpl();
        // Suscribirse a eventos del servicio
        this.service.registrarObservador(this);
    }

    // Constructor para inyección/testing
    public P2PController(IP2PService service) {
        this.service = service;
        this.service.registrarObservador(this);
    }

    @Override
    public CompletableFuture<Void> iniciarRed() {
        return service.iniciarRed();
    }

    @Override
    public void registrarObservador(IObservador observador) {
        this.service.registrarObservador(observador);
    }

    @Override
    public void removerObservador(IObservador observador) {
        this.service.removerObservador(observador);
    }

    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        try {
            switch (tipoDeDato) {
                case "P2P_PEER_LIST_RECIBIDA":
                case "P2P_ACTUALIZACION":
                    notifyPeerList(datos);
                    break;
                case "P2P_JOIN_EXITOSA":
                    notifyJoinSuccess(datos);
                    break;
                case "P2P_JOIN_ERROR":
                case "P2P_PEER_LIST_ERROR":
                    notifyError(tipoDeDato, datos);
                    break;
                default:
                    // forward raw events
                    notifyRaw(tipoDeDato, datos);
            }
        } catch (Exception ignored) {
            // proteger contra fallos en el mapeo
            notifyRaw("P2P_CONTROLLER_ERROR", ignored);
        }
    }

    private void notifyPeerList(Object datos) {
        List<PeerDTO> dtoList = new ArrayList<>();
        if (datos instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) datos;
            Object peersObj = map.get("peers");
            if (peersObj instanceof List) {
                List<?> list = (List<?>) peersObj;
                for (Object o : list) {
                    if (o instanceof DTOPeer) dtoList.add(toDTO((DTOPeer) o));
                }
            }
        } else if (datos instanceof List) {
            List<?> list = (List<?>) datos;
            for (Object o : list) if (o instanceof DTOPeer) dtoList.add(toDTO((DTOPeer) o));
        }
        notifyRaw("P2P_PEER_LIST_RECIBIDA", dtoList);
    }

    private void notifyJoinSuccess(Object datos) {
        if (datos instanceof DTOPeer) {
            PeerDTO dto = toDTO((DTOPeer) datos);
            notifyRaw("P2P_JOIN_EXITOSA", dto);
        }
    }

    private void notifyError(String tipo, Object datos) {
        notifyRaw(tipo, datos);
    }

    private PeerDTO toDTO(DTOPeer p) {
        if (p == null) return null;
        String uuid = p.getId() != null ? p.getId() : "-";
        String ip = p.getIp() != null ? p.getIp() : "-";
        int port = -1;
        boolean online = "ONLINE".equalsIgnoreCase(p.getEstado());
        String label = uuid.length() > 6 ? uuid.substring(0,6) : uuid;
        return new PeerDTO(uuid, label, ip, port, online);
    }

    private void notifyRaw(String tipo, Object datos) {
        // copia para iteración segura
        List<P2PListener> copy;
        synchronized (listeners) {
            copy = new ArrayList<>(listeners);
        }
        for (P2PListener l : copy) {
            try { l.onEvent(tipo, datos); } catch (Exception ignored) {}
        }
    }

    @Override
    public void cerrar() {
        try { this.service.removerObservador(this); } catch (Exception ignored) {}
    }

    @Override
    public void addListener(P2PListener listener) {
        if (listener == null) return;
        listeners.add(listener);
    }

    @Override
    public void removeListener(P2PListener listener) {
        if (listener == null) return;
        listeners.remove(listener);
    }
}
