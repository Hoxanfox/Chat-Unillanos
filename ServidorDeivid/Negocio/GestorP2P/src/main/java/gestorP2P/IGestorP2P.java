package gestorP2P;

import observador.ISujeto;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import dominio.p2p.Peer;

public interface IGestorP2P extends ISujeto {
    /**
     * Solicita unirse a la red P2P pasando la IP y puerto del peer objetivo.
     * Devuelve un CompletableFuture con el UUID asignado por la red o una excepcion en caso de fallo.
     */
    CompletableFuture<UUID> unirseRed(String ip, int puerto);

    /**
     * Solicita al peer remoto la lista de peers conocidos en la red P2P.
     * Devuelve un CompletableFuture con la lista de objetos Peer (persistidos localmente) o una excepcion en caso de fallo.
     */
    CompletableFuture<List<Peer>> solicitarListaPeers(String ip, int puerto);

    /**
     * Inicia el comportamiento de bootstrap/inicio de la red P2P según la configuración y la BD local.
     * - Si existe un bootstrap en config intentará unirse y sincronizar.
     * - Si no hay bootstrap se crea el peer "genesis" en la BD para aceptar conexiones.
     */
    CompletableFuture<Void> iniciarRed();
}
