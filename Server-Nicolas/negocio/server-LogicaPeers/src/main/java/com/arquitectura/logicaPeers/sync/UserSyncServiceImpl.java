package com.arquitectura.logicaPeers.sync;

import com.arquitectura.DTO.Comunicacion.DTORequest;
import com.arquitectura.DTO.Comunicacion.DTOResponse;
import com.arquitectura.DTO.p2p.PeerResponseDto;
import com.arquitectura.persistence.repository.PeerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class UserSyncServiceImpl implements UserSyncService {

    private final com.arquitectura.persistence.repository.PeerRepository peerRepository;
    private final com.arquitectura.utils.p2p.PeerConnectionPool peerConnectionPool;
    private final com.arquitectura.utils.network.NetworkUtils networkUtils;
    private final com.arquitectura.logicaPeers.config.P2PConfig p2pConfig;

    @Autowired
    public UserSyncServiceImpl(PeerRepository peerRepository,
                               com.arquitectura.utils.p2p.PeerConnectionPool peerConnectionPool,
                               com.arquitectura.utils.network.NetworkUtils networkUtils,
                               com.arquitectura.logicaPeers.config.P2PConfig p2pConfig) {
        this.peerRepository = peerRepository;
        this.peerConnectionPool = peerConnectionPool;
        this.networkUtils = networkUtils;
        this.p2pConfig = p2pConfig;
    }

    @Override
    public List<java.util.Map<String, Object>> sincronizarUsuariosDeTodosLosPeers() {
        System.out.println("🔄 [UserSyncService] Iniciando sincronización de usuarios de todos los peers...");

        List<java.util.Map<String, Object>> todosLosUsuarios = new ArrayList<>();
        java.util.Set<String> usuariosYaAgregados = new java.util.HashSet<>();

        try {
            // Determinar IP y puerto local para no consultarnos a nosotros mismos
            String ipServidor = null;
            int puertoLocal = -1;
            try {
                ipServidor = networkUtils.getServerIPAddress();
                puertoLocal = p2pConfig.getPuerto();
            } catch (Exception ignored) {
            }

            // Obtener lista de peers activos
            List<PeerResponseDto> peersActivos = new java.util.ArrayList<>();
            List<com.arquitectura.domain.Peer> peers = peerRepository.findByConectado(com.arquitectura.domain.enums.EstadoPeer.ONLINE);
            for (com.arquitectura.domain.Peer p : peers) {
                peersActivos.add(new PeerResponseDto(p.getPeerId(), p.getIp(), p.getPuerto(), p.getConectado().toString(), p.getUltimoLatido(), p.getNombreServidor()));
            }

            // Filtrar el peer local por IP y puerto si están disponibles
            List<PeerResponseDto> peersRemotos = new java.util.ArrayList<>();
            for (PeerResponseDto p : peersActivos) {
                if (ipServidor != null && puertoLocal > 0) {
                    if (ipServidor.equals(p.getIp()) && puertoLocal == p.getPuerto()) {
                        // saltar peer local
                        continue;
                    }
                }
                peersRemotos.add(p);
            }

            System.out.println("→ [UserSyncService] Consultando usuarios de " + peersRemotos.size() + " peers remotos activos");

            // Consultar cada peer
            for (PeerResponseDto peer : peersRemotos) {
                try {
                    System.out.println("  ├─ Consultando peer: " + peer.getNombreServidor() +
                                     " (" + peer.getIp() + ":" + peer.getPuerto() + ")");

                    // Preparar la petición
                    java.util.Map<String, Object> requestData = new java.util.HashMap<>();
                    requestData.put("peerId", null);

                    DTORequest request = new DTORequest("sincronizarUsuarios", requestData);

                    // Usar PeerConnectionPool.enviarPeticion para hacer la petición
                    DTOResponse response = peerConnectionPool.enviarPeticion(
                        peer.getIp(),
                        peer.getPuerto(),
                        request
                    );

                    if (response != null && "success".equals(response.getStatus())) {
                        // Extraer la lista de usuarios de la respuesta
                        Object dataObj = response.getData();

                        if (dataObj instanceof java.util.Map) {
                            @SuppressWarnings("unchecked")
                            java.util.Map<String, Object> dataMap = (java.util.Map<String, Object>) dataObj;

                            if (dataMap.containsKey("usuarios")) {
                                Object usuariosObj = dataMap.get("usuarios");

                                if (usuariosObj instanceof java.util.List) {
                                    @SuppressWarnings("unchecked")
                                    java.util.List<java.util.Map<String, Object>> usuariosPeer =
                                        (java.util.List<java.util.Map<String, Object>>) usuariosObj;

                                    // Agregar usuarios que no estén duplicados
                                    int usuariosAgregados = 0;
                                    for (java.util.Map<String, Object> usuario : usuariosPeer) {
                                        String usuarioId = (String) usuario.get("usuarioId");

                                        // Solo agregar si no está duplicado
                                        if (usuarioId != null && !usuariosYaAgregados.contains(usuarioId)) {
                                            todosLosUsuarios.add(usuario);
                                            usuariosYaAgregados.add(usuarioId);
                                            usuariosAgregados++;
                                        }
                                    }

                                    System.out.println("  └─ ✓ Agregados " + usuariosAgregados + " usuarios del peer " +
                                                     peer.getNombreServidor());
                                }
                            }
                        }
                    } else {
                        System.out.println("  └─ ⚠ Peer respondió con error o sin datos: " +
                                         (response != null ? response.getMessage() : "null"));
                    }

                } catch (Exception e) {
                    System.err.println("  └─ ✗ Error al consultar peer " + peer.getNombreServidor() +
                                     ": " + e.getMessage());
                }
            }

            System.out.println("✓ [UserSyncService] Sincronización completada. Total usuarios de peers remotos: " +
                             todosLosUsuarios.size());

        } catch (Exception e) {
            System.err.println("✗ [UserSyncService] Error al sincronizar usuarios de peers: " + e.getMessage());
            e.printStackTrace();
        }

        return todosLosUsuarios;
    }
}
