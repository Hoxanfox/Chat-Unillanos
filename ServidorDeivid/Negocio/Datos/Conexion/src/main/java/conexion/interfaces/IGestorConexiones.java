package conexion.interfaces;

import dto.p2p.DTOPeerDetails;
import java.util.List;

public interface IGestorConexiones {
    void iniciarServidor(int puertoEscucha);
    void conectarAPeer(String host, int puerto);
    void enviarMensaje(DTOPeerDetails peer, String mensaje);
    void broadcast(String mensaje);
    void desconectar(DTOPeerDetails peer);
    List<DTOPeerDetails> obtenerDetallesPeers();
    void apagar();
}