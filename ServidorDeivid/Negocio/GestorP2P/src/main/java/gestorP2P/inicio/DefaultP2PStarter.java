package gestorP2P.inicio;

import gestorP2P.IGestorP2P;
import gestorP2P.config.FileConfigReader;
import gestorP2P.config.IConfigReader;
import gestorP2P.registroP2P.IPeerRegistrar;
import repositorio.p2p.PeerRepositorio;

import dominio.p2p.Peer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.UUID;

/**
 * Implementación por defecto del starter P2P.
 */
public class DefaultP2PStarter implements IStarterP2P {

    private final IGestorP2P gestorP2P;
    private final IPeerRegistrar peerRegistrar;
    private final PeerRepositorio peerRepo;
    private final IConfigReader config;
    private final ExecutorService exec = Executors.newCachedThreadPool();

    public DefaultP2PStarter(IGestorP2P gestorP2P, IPeerRegistrar peerRegistrar) {
        this(gestorP2P, peerRegistrar, new PeerRepositorio(), new FileConfigReader());
    }

    public DefaultP2PStarter(IGestorP2P gestorP2P, IPeerRegistrar peerRegistrar, PeerRepositorio peerRepo, IConfigReader config) {
        this.gestorP2P = gestorP2P;
        this.peerRegistrar = peerRegistrar;
        this.peerRepo = peerRepo;
        this.config = config;
    }

    @Override
    public CompletableFuture<Void> iniciar() {
        return CompletableFuture.supplyAsync(() -> {
            // 1) Leer configuración de host/puerto local
            String localHost = config.getString("peer.host", "localhost");
            int localPort = config.getInt("peer.puerto", 9000);
            String localSocketInfo = localHost + ":" + localPort;

            // 2) Verificar si existe peer local en BD por socketInfo
            try {
                Peer existente = peerRepo.obtenerPorSocketInfo(localSocketInfo);
                if (existente != null) {
                    // Tenemos un peer local registrado: intentar conectar a peers en BD
                    List<String> sockets = peerRepo.listarSocketInfos();
                    List<CompletableFuture<?>> futures = new ArrayList<>();
                    for (String s : sockets) {
                        if (s == null || s.isEmpty()) continue;
                        // evitar intentar conectarnos a nosotros mismos
                        if (s.equals(localSocketInfo)) continue;
                        String[] parts = s.split(":" );
                        if (parts.length < 2) continue;
                        final String ip = parts[0];
                        final int port;
                        try { port = Integer.parseInt(parts[1]); } catch (Exception e) { continue; }
                        // ejecutar unirse asíncrono
                        CompletableFuture<?> f = gestorP2P.unirseRed(ip, port)
                                .exceptionally(ex -> null);
                        futures.add(f);
                    }
                    // esperar a que todas las intentos terminen (no bloquear indefinidamente)
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                    return null;
                }

                // 3) No existe peer local: intentar bootstrap desde config
                String bootstrapHost = config.getString("peer.inicial.host").orElse(null);
                int bootstrapPort = config.getInt("peer.inicial.puerto", -1);
                if (bootstrapHost != null && !bootstrapHost.isEmpty() && bootstrapPort > 0) {
                    try {
                        UUID id = gestorP2P.unirseRed(bootstrapHost, bootstrapPort).get();
                        if (id != null) {
                            // solicitar lista para sincronizar
                            gestorP2P.solicitarListaPeers(bootstrapHost, bootstrapPort).exceptionally(ex -> null).get();
                            return null;
                        }
                    } catch (Exception ignored) {
                        // si falla bootstrap, proceder a crear genesis
                    }
                }

                // 4) No hay bootstrap configurado o no fue posible unirse => crear peer genesis local
                UUID newId = UUID.randomUUID();
                Peer genesis = new Peer(newId, localHost, null, Peer.Estado.ONLINE, Instant.now());
                peerRegistrar.registrarPeer(genesis, localSocketInfo);
                return null;

            } catch (Exception e) {
                // No propagar excepción; loggear y continuar
                System.err.println("[DefaultP2PStarter] Error iniciando P2P: " + e.getMessage());
                return null;
            }
        }, exec);
    }
}

