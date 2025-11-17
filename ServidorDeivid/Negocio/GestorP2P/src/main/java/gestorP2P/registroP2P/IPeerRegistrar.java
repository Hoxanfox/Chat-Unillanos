package gestorP2P.registroP2P;

import dominio.p2p.Peer;
import dto.p2p.DTOPeer;
import observador.ISujeto;

import java.util.List;

/**
 * Interfaz que separa la responsabilidad de registrar/persistir peers.
 * Sigue SOLID: single responsibility para el registro/persistencia de peers.
 */
public interface IPeerRegistrar extends ISujeto {
    boolean registrarPeer(Peer peer, String socketInfo);
    Peer mapearDesdeDTO(DTOPeer dto);
    boolean registrarDesdeDTO(DTOPeer dto);
    List<Peer> registrarListaDesdeDTO(List<DTOPeer> dtos);
}
