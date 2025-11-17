package gestorP2P.actualizacion;

import dominio.p2p.Peer;
import java.util.List;

/**
 * Responsabilidad única: publicar notificaciones push de peers/actualizaciones.
 */
public interface IPushPublisher {
    void publicarNuevoPeer(Peer peer);
    void publicarActualizacion(List<Peer> peers);
}

