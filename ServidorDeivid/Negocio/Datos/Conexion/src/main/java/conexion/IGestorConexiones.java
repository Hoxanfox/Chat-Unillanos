package conexion;

import dto.p2p.DTOPeerDetails;
import java.util.List;

/**
 * Contrato para gestionar el pool de conexiones.
 * AHORA: Todos los métodos aceptan o devuelven DTOPeerDetails.
 * La capa superior no maneja IDs sueltos ni entidades de dominio.
 */
public interface IGestorConexiones {

    void iniciarServidor(int puertoEscucha);

    // Se mantiene host/port porque al conectar por primera vez no tenemos un DTO aún
    void conectarAPeer(String host, int puerto);

    // CORRECCIÓN: Recibimos el DTO completo para enviar el mensaje
    void enviarMensaje(DTOPeerDetails peer, String mensaje);

    void broadcast(String mensaje);

    // CORRECCIÓN: Recibimos el DTO para desconectar
    void desconectar(DTOPeerDetails peer);

    List<DTOPeerDetails> obtenerDetallesPeers();

    void apagar();
}