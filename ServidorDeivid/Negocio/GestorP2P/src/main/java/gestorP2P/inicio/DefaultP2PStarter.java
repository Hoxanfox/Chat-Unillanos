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
import transporte.FabricaTransporte;
import transporte.TransporteServidor;
import dto.gestionConexion.transporte.DTOConexion;
import conexion.GestorConexion;
import dto.gestionConexion.conexion.DTOSesion;

/**
 * Implementación por defecto del starter P2P.
 */
public class DefaultP2PStarter implements IStarterP2P {

    private final IGestorP2P gestorP2P;
    private final IPeerRegistrar peerRegistrar;
    private final PeerRepositorio peerRepo;
    private final IConfigReader config;
    private final ExecutorService exec = Executors.newCachedThreadPool();

    // Transporte servidor local para aceptar conexiones entrantes (peers)
    private final TransporteServidor transporteServidor = new TransporteServidor();

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

                    // Iniciar servidor de escucha local (solo si existe peer local en BD)
                    try {
                        transporteServidor.iniciar(localHost, localPort, true, sesion -> {
                            try {
                                GestorConexion.getInstancia().agregarSesionPeer(sesion);
                                // Evitar iniciar un lector directo aquí: el gestor de respuestas ya
                                // tiene un hilo que consume sesiones desde el pool PEERS.
                                // Se eliminó la llamada a GestorRespuesta.getInstancia().escucharSesionDirecta(sesion, TipoPool.PEERS);
                            } catch (Exception e) {
                                LoggerCentral.warn("DefaultP2PStarter: fallo al añadir sesión entrante al poolPeers -> " + e.getMessage());
                                try { if (sesion != null && sesion.getSocket() != null) sesion.getSocket().close(); } catch (Exception ignored) {}
                            }
                        });
                        // Arrancar la escucha del gestor de respuestas en pool PEERS ahora que el servidor local está iniciado
                        try { if (gestorP2P != null) gestorP2P.iniciarEscuchaPoolPeers(); } catch (Exception e) { LoggerCentral.warn("DefaultP2PStarter: no se pudo iniciar escucha pool PEERS tras iniciar transporte servidor -> " + e.getMessage()); }
                    } catch (Exception e) {
                        LoggerCentral.warn("DefaultP2PStarter.iniciar: no se pudo iniciar transporte servidor en " + localSocketInfo + " -> " + e.getMessage());
                    }

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

                        // Intento proactivo: crear conexión TCP directa al bootstrap y añadir al poolPeers para que el envío use la sesión
                        try {
                            LoggerCentral.debug("DefaultP2PStarter: intentando conectar proactivamente al bootstrap " + bootstrapHost + ":" + bootstrapPort);
                            DTOConexion datos = new DTOConexion(bootstrapHost, bootstrapPort);
                            DTOSesion s = FabricaTransporte.crearTransporte("TCP").conectar(datos);
                            if (s != null && s.estaActiva()) {
                                LoggerCentral.debug("DefaultP2PStarter: conexión proactiva al bootstrap establecida -> " + s + ". Añadiendo al poolPeers");
                                GestorConexion.getInstancia().agregarSesionPeer(s);
                            } else {
                                LoggerCentral.debug("DefaultP2PStarter: no se pudo establecer conexión proactiva al bootstrap");
                            }
                        } catch (Exception e) {
                            LoggerCentral.debug("DefaultP2PStarter: error al intentar conexión proactiva al bootstrap -> " + e.getMessage());
                        }

                        UUID id = gestorP2P.unirseRed(bootstrapHost, bootstrapPort).get();
                        if (id != null) {
                            LoggerCentral.info("DefaultP2PStarter.iniciar: bootstrap exitoso, id asignado=" + id);

                            // Iniciar servidor de escucha local tras unirse con éxito (ahora el peer local debería estar registrado)
                            try {
                                transporteServidor.iniciar(localHost, localPort, true, sesion -> {
                                    try {
                                        GestorConexion.getInstancia().agregarSesionPeer(sesion);
                                        // Evitar iniciar un lector directo aquí: el gestor de respuestas ya
                                        // consume sesiones desde pool PEERS y crear otro lector provocaría
                                        // que ambos hilos accedan/cierren el mismo BufferedReader.
                                    } catch (Exception e) {
                                        LoggerCentral.warn("DefaultP2PStarter: fallo al añadir sesión entrante al poolPeers -> " + e.getMessage());
                                        try { if (sesion != null && sesion.getSocket() != null) sesion.getSocket().close(); } catch (Exception ignored) {}
                                    }
                                });
                                try { if (gestorP2P != null) gestorP2P.iniciarEscuchaPoolPeers(); } catch (Exception e) { LoggerCentral.warn("DefaultP2PStarter: no se pudo iniciar escucha pool PEERS tras iniciar transporte servidor (bootstrap) -> " + e.getMessage()); }
                            } catch (Exception e) {
                                LoggerCentral.warn("DefaultP2PStarter.iniciar: no se pudo iniciar transporte servidor tras bootstrap en " + localSocketInfo + " -> " + e.getMessage());
                            }

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

                // Iniciar servidor de escucha local tras crear/registrar peer genesis
                try {
                    transporteServidor.iniciar(localHost, localPort, true, sesion -> {
                        try {
                            GestorConexion.getInstancia().agregarSesionPeer(sesion);
                            // Evitar iniciar un lector directo aquí: el gestor de respuestas escucha
                            // en el pool PEERS.
                        } catch (Exception e) {
                            LoggerCentral.warn("DefaultP2PStarter: fallo al añadir sesión entrante al poolPeers -> " + e.getMessage());
                            try { if (sesion != null && sesion.getSocket() != null) sesion.getSocket().close(); } catch (Exception ignored) {}
                        }
                    });
                    try { if (gestorP2P != null) gestorP2P.iniciarEscuchaPoolPeers(); } catch (Exception e) { LoggerCentral.warn("DefaultP2PStarter: no se pudo iniciar escucha pool PEERS tras iniciar transporte servidor (genesis) -> " + e.getMessage()); }
                } catch (Exception e) {
                    LoggerCentral.warn("DefaultP2PStarter.iniciar: no se pudo iniciar transporte servidor tras crear peer genesis en " + localSocketInfo + " -> " + e.getMessage());
                }

                return null;

            } catch (Exception e) {
                // No propagar excepción; loggear y continuar
                LoggerCentral.error("DefaultP2PStarter.iniciar: Error iniciando P2P: " + e.getMessage(), e);
                return null;
            }
        }, exec);
    }
}
