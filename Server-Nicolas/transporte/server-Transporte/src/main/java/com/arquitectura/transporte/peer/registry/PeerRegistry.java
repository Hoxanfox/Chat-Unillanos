package com.arquitectura.transporte.peer.registry;

import com.arquitectura.DTO.Comunicacion.DTORequest;
import com.arquitectura.DTO.Comunicacion.DTOResponse;
import com.arquitectura.DTO.p2p.PeerResponseDto;
import com.arquitectura.logicaPeers.IPeerService;
import com.arquitectura.transporte.peer.PeerConnectionManager;
import com.arquitectura.utils.p2p.PeerClient;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Responsable de las operaciones relacionadas con la tabla de peers y bootstrap.
 */
public class PeerRegistry {
    private static final Logger log = LoggerFactory.getLogger(PeerRegistry.class);

    private final IPeerService peerService;
    private final PeerConnectionManager manager;
    private final Gson gson;
    private final String bootstrapNodes;
    private final int peerPort;

    // Cliente ligero para contactar bootstrap peers cuando sea necesario
    private final PeerClient peerClient;

    public PeerRegistry(IPeerService peerService, PeerConnectionManager manager, Gson gson, String bootstrapNodes, int peerPort) {
        this.peerService = peerService;
        this.manager = manager;
        this.gson = gson;
        this.bootstrapNodes = bootstrapNodes;
        this.peerPort = peerPort;
        this.peerClient = new PeerClient(gson);
    }

    /**
     * Inicializa/obtiene el ID del peer local.
     * - Primero intenta pedir a los bootstrap nodes que nos asignen/retornen un peerId via la acción 'registrarPeer'.
     * - Si eso falla o no hay bootstrap configurado, obtiene o crea el peer local en la BD.
     */
    public UUID initializeLocalPeerId() {
        String localIp = null;
        try {
            localIp = manager.getNetworkUtils().getServerIPAddress();
        } catch (Exception e) {
            log.warn("Error obteniendo IP local desde NetworkUtils: {}", e.getMessage());
        }

        if (localIp == null || localIp.isEmpty() || "127.0.0.1".equals(localIp)) {
            log.warn("No se pudo obtener IP local válida, usando 'localhost'");
            localIp = "localhost";
        }

        log.info("Inicializando peer local con IP: {} y puerto P2P: {}", localIp, peerPort);

        // 1) Intentar obtener peerId desde bootstrap nodes si hay configuración
        if (bootstrapNodes != null && !bootstrapNodes.trim().isEmpty()) {
            try {
                UUID fromBootstrap = tryRequestPeerIdFromBootstrap(localIp, peerPort);
                if (fromBootstrap != null) {
                    log.info("Se obtuvo peerId desde bootstrap: {}", fromBootstrap);
                    return fromBootstrap;
                } else {
                    log.debug("Ningún bootstrap devolvió peerId válido");
                }
            } catch (Exception e) {
                log.warn("Error al solicitar peerId a bootstrap nodes: {}", e.getMessage());
            }
        } else {
            log.debug("No hay bootstrap nodes configurados (peer.bootstrap.nodes vacío)");
        }

        // 2) Si no se obtuvo peerId desde bootstrap, crear/obtener el peer local en la BD
        PeerResponseDto localPeer = peerService.obtenerOCrearPeerLocal(localIp, peerPort);
        log.info("Peer local obtenido/creado en BD con ID: {}", localPeer.getPeerId());
        return localPeer.getPeerId();
    }

    /**
     * Intenta comunicarse con cada bootstrap configurado para pedir que nos asignen/retornen un peerId.
     * Devuelve el primer UUID válido que obtenga, o null si ninguno responde con un peerId válido.
     */
    private UUID tryRequestPeerIdFromBootstrap(String localIp, int localPort) {
        if (bootstrapNodes == null || bootstrapNodes.trim().isEmpty()) return null;

        String[] nodes = bootstrapNodes.split(",");
        for (String node : nodes) {
            node = node.trim();
            if (node.isEmpty()) continue;

            String[] parts = node.split(":");
            if (parts.length != 2) {
                log.warn("Formato inválido de bootstrap node: '{}' (se esperaba ip:puerto)", node);
                continue;
            }

            String ip = parts[0].trim();
            int port;
            try {
                port = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException nfe) {
                log.warn("Puerto inválido en bootstrap node '{}'", node);
                continue;
            }

            try {
                log.info("Contactando bootstrap {}:{} para solicitar peerId...", ip, port);

                JsonObject payload = new JsonObject();
                payload.addProperty("ip", localIp);
                payload.addProperty("puerto", localPort);

                DTORequest req = new DTORequest("registrarPeer", gson.fromJson(gson.toJson(payload), Object.class));

                DTOResponse resp = peerClient.enviarPeticion(ip, port, req, 5000);
                if (resp == null) {
                    log.debug("Respuesta nula desde bootstrap {}:{}", ip, port);
                    continue;
                }

                if (!"success".equalsIgnoreCase(resp.getStatus())) {
                    log.debug("Bootstrap {}:{} respondió con estado no exitoso: {}", ip, port, resp.getStatus());
                    continue;
                }

                Object data = resp.getData();
                if (data == null) {
                    log.debug("Bootstrap {}:{} no devolvió data en la respuesta", ip, port);
                    continue;
                }

                JsonObject json = gson.toJsonTree(data).getAsJsonObject();
                if (json.has("peerSolicitante")) {
                    JsonObject ps = json.getAsJsonObject("peerSolicitante");
                    if (ps.has("peerId") && !ps.get("peerId").isJsonNull()) {
                        String peerIdStr = ps.get("peerId").getAsString();
                        try {
                            UUID assigned = UUID.fromString(peerIdStr);

                            // Procesar lista de peers que el bootstrap nos devolvió (si la incluye)
                            try {
                                // El bootstrap puede devolver la lista bajo diferentes claves.
                                // Aceptamos: peersDisponibles (sólo online), peers (toda la BD del bootstrap), peersEnBD
                                var arrPeers = (json.has("peersDisponibles") ? json.getAsJsonArray("peersDisponibles") :
                                              (json.has("peers") ? json.getAsJsonArray("peers") :
                                              (json.has("peersEnBD") ? json.getAsJsonArray("peersEnBD") : null)));
                                if (arrPeers != null) {
                                    for (var elemPeer : arrPeers) {
                                        try {
                                            var objPeer = elemPeer.getAsJsonObject();
                                            String pIp = objPeer.has("ip") && !objPeer.get("ip").isJsonNull() ? objPeer.get("ip").getAsString() : null;
                                            int pPuerto = objPeer.has("puerto") && !objPeer.get("puerto").isJsonNull() ? objPeer.get("puerto").getAsInt() : -1;

                                            if (pIp == null || pIp.trim().isEmpty() || pPuerto <= 0 || pPuerto > 65535) {
                                                log.debug("Peer del bootstrap inválido (ip/puerto): {}/{}", pIp, pPuerto);
                                                continue;
                                            }

                                            try {
                                                Optional<PeerResponseDto> existingPeerBoot = peerService.buscarPeerPorIpYPuerto(pIp, pPuerto);
                                                if (existingPeerBoot.isEmpty()) {
                                                    // Registrar peer aunque esté offline; esto puebla la tabla local con la información del bootstrap
                                                    peerService.agregarPeer(pIp, pPuerto);
                                                    log.info("Peer proveniente del bootstrap registrado en BD local: {}:{}", pIp, pPuerto);
                                                } else {
                                                    log.debug("Peer del bootstrap ya existe en BD local: {}:{} (id={})", pIp, pPuerto, existingPeerBoot.get().getPeerId());
                                                }
                                            } catch (Exception epp) {
                                                log.warn("Error registrando peer del bootstrap {}:{} -> {}", pIp, pPuerto, epp.getMessage());
                                            }

                                        } catch (Exception inner) {
                                            log.debug("Elemento peers devuelto por bootstrap inválido: {}", inner.getMessage());
                                        }
                                    }
                                } else {
                                    log.debug("El bootstrap no devolvió una lista de peers en 'peersDisponibles' ni en claves alternativas");
                                }
                             }
                             catch (Exception exPeers) {
                                log.debug("Error procesando peersDisponibles devueltos por bootstrap: {}", exPeers.getMessage());
                            }

                            // Asegurar que la BD conoce este peer (en caso de que bootstrap no lo haya persistido localmente)
                            try {
                                Optional<PeerResponseDto> exist = peerService.buscarPeerPorIpYPuerto(localIp, localPort);
                                if (exist.isEmpty()) {
                                    peerService.agregarPeer(localIp, localPort);
                                    log.info("Se creó registro local en BD para {}:{} tras recibir peerId desde bootstrap", localIp, localPort);
                                }
                            } catch (Exception ex) {
                                log.debug("No se pudo verificar/crear registro local en BD: {}", ex.getMessage());
                            }

                            return assigned;
                        } catch (Exception iae) {
                            log.warn("peerId retornado por bootstrap no es un UUID válido: {}", peerIdStr);
                        }
                    }
                }

                log.debug("Bootstrap {}:{} respondió pero no incluyó peerSolicitante.peerId", ip, port);

            } catch (Exception e) {
                log.warn("Error comunicándose con bootstrap {}:{} -> {}", ip, port, e.getMessage());
            }
        }

        return null;
    }

    public void initializePeersOnStartup() {
        try {
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("INICIALIZANDO PEERS EN STARTUP");
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            List<PeerResponseDto> peers = peerService.listarPeersDisponibles();
            log.info("Peers encontrados en BD: {}", peers == null ? 0 : peers.size());

            if (peers != null) {
                for (PeerResponseDto peer : peers) {
                    log.info("  - Peer: {} ({}:{}) - Estado: {}",
                            peer.getPeerId(), peer.getIp(), peer.getPuerto(), peer.getConectado());
                }
            }

            if (peers == null || peers.isEmpty() || peers.size() == 1) {
                log.warn("⚠️ La BD contiene 0 o solo 1 peer (probablemente solo el local)");
                log.info("Intentando poblar tabla de peers desde bootstrap...");

                connectToBootstrapPeers();

                log.info("Esperando 2 segundos para que peers bootstrap se registren en BD...");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("Espera interrumpida durante inicialización de peers");
                }

                peers = peerService.listarPeersDisponibles();
                log.info("Peers en BD tras bootstrap: {}", peers == null ? 0 : peers.size());
            } else {
                log.info("✓ Se encontraron {} peers en BD", peers.size());
            }

            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("CONECTANDO A PEERS CONOCIDOS");
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            manager.connectToAllKnownPeers();

            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("INICIALIZACIÓN DE PEERS COMPLETADA");
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        } catch (Exception e) {
            log.error("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.error("ERROR INICIALIZANDO PEERS EN STARTUP: {}", e.getMessage(), e);
            log.error("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        }
    }

    public void connectToBootstrapPeers() {
        if (bootstrapNodes == null || bootstrapNodes.trim().isEmpty()) {
            log.warn("No bootstrap peers configurados");
            return;
        }

        String[] nodes = bootstrapNodes.split(",");
        int bootstrapAttempts = 0;

        log.info("Encontrados {} bootstrap peers configurados", nodes.length);

        for (String node : nodes) {
            node = node.trim();
            if (node.isEmpty()) continue;

            String[] parts = node.split(":");
            if (parts.length != 2) {
                log.warn("Formato inválido de bootstrap peer: '{}' (esperado ip:puerto)", node);
                continue;
            }

            try {
                String ip = parts[0].trim();
                int port = Integer.parseInt(parts[1].trim());

                Optional<PeerResponseDto> existingPeer = peerService.buscarPeerPorIpYPuerto(ip, port);

                UUID peerId;
                if (existingPeer.isPresent()) {
                    peerId = existingPeer.get().getPeerId();
                    log.info("Bootstrap peer {}:{} ya existe en BD con ID {}", ip, port, peerId);
                } else {
                    PeerResponseDto saved = peerService.agregarPeer(ip, port);
                    peerId = saved.getPeerId();
                    log.info("Nuevo bootstrap peer {}:{} registrado con ID {}", ip, port, peerId);
                }

                manager.connectToPeer(peerId, ip, port);
                bootstrapAttempts++;

            } catch (NumberFormatException e) {
                log.error("Puerto inválido en bootstrap peer: '{}'", node);
            } catch (Exception e) {
                log.error("Error procesando bootstrap peer '{}': {}", node, e.getMessage());
            }
        }

        if (bootstrapAttempts > 0) {
            log.info("✓ Intentando conectar a {} bootstrap peers", bootstrapAttempts);
        } else {
            log.warn("⚠️ No se pudo procesar ningún bootstrap peer");
        }
    }

    public void registerDiscoveredPeers(Object payload) {
        if (payload == null) {
            log.debug("registerDiscoveredPeers: payload nulo, nada que registrar");
            return;
        }

        try {
            try {
                log.debug("registerDiscoveredPeers() - payload raw: {}", gson.toJson(payload));
            } catch (Exception jse) {
                log.debug("registerDiscoveredPeers() - payload (toString): {}", payload.toString());
            }

            var json = gson.toJsonTree(payload).getAsJsonObject();
            if (!json.has("peersDisponibles")) {
                log.debug("registerDiscoveredPeers: no contiene 'peersDisponibles'");
                // Pero puede contener 'peerSolicitante' cuando fue la respuesta al registrar nuestro peer
                if (json.has("peerSolicitante")) {
                    var ps = json.getAsJsonObject("peerSolicitante");
                    if (ps.has("peerId") && !ps.get("peerId").isJsonNull()) {
                        try {
                            UUID assigned = UUID.fromString(ps.get("peerId").getAsString());
                            // Si el manager aún no tiene localPeerId, intentamos guardarlo allí
                            if (manager.getLocalPeerId() == null) {
                                try {
                                    var existing = peerService.buscarPeerPorId(assigned);
                                    if (existing.isEmpty()) {
                                        peerService.agregarPeer(manager.getNetworkUtils().getServerIPAddress(), peerPort);
                                    }
                                } catch (Exception ex) {
                                    log.debug("No se pudo asegurar existencia del peer asignado en BD: {}", ex.getMessage());
                                }
                                log.info("Se detectó peerSolicitante con peerId asignado por bootstrap: {}", assigned);
                            }
                        } catch (Exception e) {
                            log.debug("peerSolicitante.peerId inválido en payload de descubrimiento: {}", e.getMessage());
                        }
                    }
                }
                return;
            }

            var arr = json.getAsJsonArray("peersDisponibles");
            int added = 0;
            int updated = 0;

            log.info("Procesando {} peers descubiertos...", arr.size());

            for (var elem : arr) {
                var obj = elem.getAsJsonObject();
                log.debug("registerDiscoveredPeers() - peer element: {}", obj.toString());

                String ip = obj.has("ip") && !obj.get("ip").isJsonNull() ? obj.get("ip").getAsString() : null;
                int puerto = obj.has("puerto") && !obj.get("puerto").isJsonNull() ? obj.get("puerto").getAsInt() : -1;

                if (ip == null || ip.trim().isEmpty() || puerto <= 0 || puerto > 65535) {
                    log.debug("Peer descubierto inválido (ip/puerto): {}/{}", ip, puerto);
                    continue;
                }

                try {
                    var existing = peerService.buscarPeerPorIpYPuerto(ip, puerto);
                    if (existing.isPresent()) {
                        log.debug("Peer descubierto ya existe en BD: {}:{} (id={})", ip, puerto, existing.get().getPeerId());
                        updated++;
                    } else {
                        PeerResponseDto addedPeer = peerService.agregarPeer(ip, puerto);
                        added++;
                        log.info("✓ Nuevo peer registrado en BD: {}:{} (id={})", ip, puerto, addedPeer.getPeerId());
                    }
                } catch (Exception e) {
                    log.warn("Error registrando peer descubierto {}:{} -> {}", ip, puerto, e.getMessage());
                }
            }

            if (added > 0 || updated > 0) {
                log.info("Descubrimiento completado: {} nuevos, {} existentes", added, updated);

                if (added > 0) {
                    log.info("Esperando 1 segundo antes de conectar a {} peers nuevos...", added);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    manager.connectToAllKnownPeers();
                }
            } else {
                log.debug("No se registraron peers nuevos desde descubrimiento");
            }

        } catch (Exception e) {
            log.warn("Error procesando payload de descubrimiento: {}", e.getMessage(), e);
        }
    }
}
