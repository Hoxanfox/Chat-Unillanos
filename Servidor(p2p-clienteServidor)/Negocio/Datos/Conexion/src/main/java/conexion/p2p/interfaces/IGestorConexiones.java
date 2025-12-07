package conexion.p2p.interfaces;

import dto.p2p.DTOPeerDetails;
import java.util.List;

public interface IGestorConexiones {
    void iniciarServidor(int puertoEscucha);
    void conectarAPeer(String host, int puerto);
    void enviarMensaje(DTOPeerDetails peer, String mensaje);
    void broadcast(String mensaje);
    void desconectar(DTOPeerDetails peer);

    /**
     * Actualiza el puerto lógico (servidor) de un peer ya conectado.
     * Útil cuando un peer se conecta desde un puerto efímero pero queremos
     * guardar su puerto de escucha real para futuras conexiones.
     */
    void actualizarPuertoServidor(String connectionId, int puertoReal);


    List<DTOPeerDetails> obtenerDetallesPeers();
    void apagar();
}