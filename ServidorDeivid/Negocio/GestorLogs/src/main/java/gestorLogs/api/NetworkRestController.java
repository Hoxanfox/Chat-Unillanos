package gestorLogs.api;

import dto.p2p.DTOPeerDetails;
import logger.LoggerCentral;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Controlador REST para exponer la topología de la red.
 * El API Gateway consumirá este endpoint para descubrir a otros peers.
 */
@RestController
@RequestMapping("/api/network")
@CrossOrigin(origins = "*") // Permite que el Gateway (Docker) llame sin problemas
public class NetworkRestController {

    private static final String TAG = "NetworkRestController";
    
    // Una función simple que devuelve una lista. No necesitamos importar FachadaP2P aquí.
    private final Supplier<List<DTOPeerDetails>> proveedorPeers;

    // Inyectamos el proveedor en el constructor
    public NetworkRestController(Supplier<List<DTOPeerDetails>> proveedorPeers) {
        this.proveedorPeers = proveedorPeers;
        LoggerCentral.info(TAG, "NetworkRestController inicializado");
    }

    /**
     * GET /api/network/peers
     * Devuelve la lista de peers conocidos por este nodo (obtenidos de la Fachada P2P)
     */
    @GetMapping("/peers")
    public ResponseEntity<List<DTOPeerDetails>> obtenerPeers() {
        try {
            // Ejecutamos la función proveedora para obtener los datos frescos
            List<DTOPeerDetails> peers = proveedorPeers != null ? proveedorPeers.get() : new ArrayList<>();
            
            LoggerCentral.debug(TAG, "GET /api/network/peers - Retornando " + peers.size() + " peers");
            return ResponseEntity.ok(peers);
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error obteniendo peers: " + e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }
}