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
import logger.LoggerCentral;

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
        LoggerCentral.debug("DefaultP2PStarter: instanciado con config provider=" + config.getClass().getSimpleName());
    }

    @Override
    public CompletableFuture<Void> iniciar() {
        return CompletableFuture.supplyAsync(() -> {
            LoggerCentral.info("DefaultP2PStarter.iniciar: inicio del proceso de arranque P2P");
            // 1) Leer configuración de host/puerto local
            String localHost = config.getString("peer.host", "localhost");
            int localPort = config.getInt("peer.puerto", 9000);
            String localSocketInfo = localHost + ":" + localPort;
            LoggerCentral.debug("DefaultP2PStarter.iniciar: localHost=" + localHost + " localPort=" + localPort + " socket=" + localSocketInfo);

            // 2) Verificar si existe peer local en BD por socketInfo
            try {
                Peer existente = peerRepo.obtenerPorSocketInfo(localSocketInfo);
                if (existente != null) {
                    LoggerCentral.info("DefaultP2PStarter.iniciar: peer local ya existe en repo id=" + (existente.getId()!=null?existente.getId():"<null>") + " socket=" + localSocketInfo);
                    // Re-notificar el peer local (incluir socketInfo) para que Observadores reciban información local
                    try {
                        peerRegistrar.registrarPeer(existente, localSocketInfo);
                    } catch (Exception e) {
                        LoggerCentral.warn("DefaultP2PStarter.iniciar: no se pudo re-notificar peer local: " + e.getMessage());
                    }
                    // Tenemos un peer local registrado: intentar conectar a peers en BD
                    List<String> sockets = peerRepo.listarSocketInfos();
                    LoggerCentral.debug("DefaultP2PStarter.iniciar: sockets en repo=" + (sockets!=null?sockets.size():0));
                    List<CompletableFuture<?>> futures = new ArrayList<>();
                    if (sockets != null) {
                        for (String s : sockets) {
                            if (s == null || s.isEmpty()) continue;
                            // evitar intentar conectarnos a nosotros mismos
                            if (s.equals(localSocketInfo)) continue;
                            String[] parts = s.split(":" );
                            if (parts.length < 2) continue;
                            final String ip = parts[0];
                            final int port;
                            try { port = Integer.parseInt(parts[1]); } catch (Exception e) { LoggerCentral.warn("DefaultP2PStarter.iniciar: puerto inválido en socket '" + s + "' -> " + e.getMessage()); continue; }
                            LoggerCentral.debug("DefaultP2PStarter.iniciar: intentando unirse a peer listado " + ip + ":" + port);
                            // ejecutar unirse asíncrono
                            CompletableFuture<?> f = gestorP2P.unirseRed(ip, port)
                                    .exceptionally(ex -> { LoggerCentral.warn("DefaultP2PStarter.iniciar: fallo al unirse a " + ip + ":" + port + " -> " + (ex!=null?ex.getMessage():"<null>")); return null; });
                            futures.add(f);
                        }
                    } else {
                        LoggerCentral.debug("DefaultP2PStarter.iniciar: lista de sockets en repo es null, omitiendo reconexiones");
                    }
                    // esperar a que todas las intentos terminen (no bloquear indefinidamente)
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                    LoggerCentral.info("DefaultP2PStarter.iniciar: intentos de reconexión a peers listados finalizados");
                    return null;
                }

                LoggerCentral.debug("DefaultP2PStarter.iniciar: no existe peer local en BD para socket=" + localSocketInfo);

                // 3) No existe peer local: intentar bootstrap desde config
                String bootstrapHost = config.getString("peer.inicial.host").orElse(null);
                int bootstrapPort = config.getInt("peer.inicial.puerto", -1);
                LoggerCentral.debug("DefaultP2PStarter.iniciar: bootstrapHost=" + bootstrapHost + " bootstrapPort=" + bootstrapPort);
                if (bootstrapHost != null && !bootstrapHost.isEmpty() && bootstrapPort > 0) {
                    try {
                        LoggerCentral.info("DefaultP2PStarter.iniciar: intentando bootstrap contra " + bootstrapHost + ":" + bootstrapPort);
                        UUID id = gestorP2P.unirseRed(bootstrapHost, bootstrapPort).get();
                        if (id != null) {
                            LoggerCentral.info("DefaultP2PStarter.iniciar: bootstrap exitoso, id asignado=" + id);
                            // solicitar lista para sincronizar
                            gestorP2P.solicitarListaPeers(bootstrapHost, bootstrapPort).exceptionally(ex -> { LoggerCentral.warn("DefaultP2PStarter.iniciar: fallo solicitando lista tras bootstrap -> " + (ex!=null?ex.getMessage():"<null>")); return null; }).get();
                            return null;
                        } else {
                            LoggerCentral.warn("DefaultP2PStarter.iniciar: bootstrap devolvió id nulo");
                        }
                    } catch (Exception e) {
                        LoggerCentral.warn("DefaultP2PStarter.iniciar: fallo en bootstrap contra " + bootstrapHost + ":" + bootstrapPort + " -> " + e.getMessage());
                        // si falla bootstrap, proceder a crear genesis
                    }
                } else {
                    LoggerCentral.debug("DefaultP2PStarter.iniciar: no hay bootstrap configurado o puerto inválido");
                }

                // 4) No hay bootstrap configurado o no fue posible unirse => crear peer genesis local
                UUID newId = UUID.randomUUID();
                Peer genesis = new Peer(newId, localHost, null, Peer.Estado.ONLINE, Instant.now());
                LoggerCentral.info("DefaultP2PStarter.iniciar: creando peer genesis local id=" + newId + " socket=" + localSocketInfo);
                peerRegistrar.registrarPeer(genesis, localSocketInfo);
                return null;

            } catch (Exception e) {
                // No propagar excepción; loggear y continuar
                LoggerCentral.error("DefaultP2PStarter.iniciar: Error iniciando P2P: " + e.getMessage(), e);
                return null;
            }
        }, exec);
    }
}
