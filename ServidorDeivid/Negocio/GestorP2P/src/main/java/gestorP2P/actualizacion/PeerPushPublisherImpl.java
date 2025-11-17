package gestorP2P.actualizacion;

import com.google.gson.Gson;
import comunicacion.peticionesPull.AccionesComunicacion;
import comunicacion.EnviadorPeticiones;
import conexion.TipoPool;
import dto.comunicacion.DTORequest;
import dto.p2p.DTOPeer;
import dto.p2p.DTOPeerListResponse;
import dto.p2p.DTOPeerPush;
import dominio.p2p.Peer;
import observador.IObservador;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación que publica actualizaciones/push a todos los peers.
 * Se registra como observador del PeerRegistrar para reenviar eventos a la red.
 */
public class PeerPushPublisherImpl implements IPushPublisher, IObservador {

    private final EnviadorPeticiones enviador;
    private final Gson gson = new Gson();

    public PeerPushPublisherImpl() {
        this.enviador = new EnviadorPeticiones();
    }

    @Override
    public void publicarNuevoPeer(Peer peer) {
        if (peer == null) return;
        DTOPeerPush push = new DTOPeerPush();
        try {
            if (peer.getId() != null) push.setId(peer.getId().toString());
            push.setIp(peer.getIp());
            push.setPort(0); // puerto desconocido aquí; el receptor puede usar puerto por defecto de config
            push.setSocketInfo(null);
            push.setTimestamp(Instant.now().toString());
        } catch (Exception ignored) {}
        DTORequest request = new DTORequest(AccionesComunicacion.PEER_PUSH, push);
        enviador.enviar(request, TipoPool.PEERS);
    }

    @Override
    public void publicarActualizacion(List<Peer> peers) {
        DTOPeerListResponse lista = new DTOPeerListResponse();
        List<DTOPeer> dtos = new ArrayList<>();
        if (peers != null) {
            for (Peer p : peers) {
                try {
                    DTOPeer dto = new DTOPeer();
                    if (p.getId() != null) dto.setId(p.getId().toString());
                    dto.setIp(p.getIp());
                    dto.setSocketInfo(null);
                    dto.setEstado(p.getEstado() != null ? p.getEstado().name() : "OFFLINE");
                    dto.setFechaCreacion(p.getFechaCreacion() != null ? p.getFechaCreacion().toString() : null);
                    dtos.add(dto);
                } catch (Exception ignored) { }
            }
        }
        lista.setPeers(dtos);
        lista.setCount(dtos.size());
        DTORequest request = new DTORequest(AccionesComunicacion.PEER_UPDATE, lista);
        enviador.enviar(request, TipoPool.PEERS);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        try {
            if ("PEER_REGISTRADO".equals(tipoDeDato) && datos instanceof Peer) {
                publicarNuevoPeer((Peer) datos);
            } else if ("PEER_LISTA_REGISTRADA".equals(tipoDeDato)) {
                if (datos instanceof List) {
                    publicarActualizacion((List<Peer>) datos);
                }
            }
        } catch (Exception ignored) {
            // No interrumpir flujo por fallo en push
        }
    }
}
