package com.arquitectura.utils.p2p;

import com.arquitectura.DTO.Comunicacion.DTORequest;
import com.arquitectura.DTO.Comunicacion.DTOResponse;
import com.google.gson.Gson;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Pool de conexiones para gestionar comunicaciones con múltiples peers.
 * Permite reutilizar clientes y ejecutar peticiones en paralelo.
 */
public class PeerConnectionPool {

    private final Map<String, PeerClient> clientPool;
    private final ExecutorService executorService;
    private final Gson gson;
    private final int maxThreads;
    
    // NUEVO: ID y puerto del peer local para pasarlo a los clientes
    private String localPeerId;
    private int localPeerPort;

    /**
     * Constructor con configuración por defecto.
     */
    public PeerConnectionPool() {
        this(10); // 10 threads por defecto
    }

    /**
     * Constructor con número de threads personalizado.
     *
     * @param maxThreads Número máximo de threads para peticiones paralelas
     */
    public PeerConnectionPool(int maxThreads) {
        this.clientPool = new ConcurrentHashMap<>();
        this.maxThreads = maxThreads;
        this.executorService = Executors.newFixedThreadPool(maxThreads);
        this.gson = new Gson();
        System.out.println("✓ [PeerConnectionPool] Inicializado con " + maxThreads + " threads");
    }
    
    /**
     * Configura el ID del peer local y su puerto P2P.
     * Debe llamarse después de que el PeerConnectionManager obtenga su ID.
     */
    public void configurarPeerLocal(String peerId, int peerPort) {
        this.localPeerId = peerId;
        this.localPeerPort = peerPort;
        System.out.println("✓ [PeerConnectionPool] Configurado peer local: " + peerId + " en puerto " + peerPort);
        
        // Limpiar el pool para que se recreen los clientes con la nueva configuración
        limpiarPool();
    }

    /**
     * Obtiene o crea un cliente para un peer específico.
     *
     * @param ip     Dirección IP del peer
     * @param puerto Puerto del peer
     * @return Cliente para el peer
     */
    public PeerClient getClient(String ip, int puerto) {
        String key = crearClave(ip, puerto);
        return clientPool.computeIfAbsent(key, k -> {
            System.out.println("→ [PeerConnectionPool] Creando nuevo cliente para: " + ip + ":" + puerto);
            
            // Si tenemos el ID y puerto local configurados, usar el constructor completo
            if (localPeerId != null && !localPeerId.isEmpty() && localPeerPort > 0) {
                return new PeerClient(gson, localPeerId, localPeerPort);
            } else {
                // Modo legacy (mostrará advertencia en PeerClient)
                System.err.println("⚠ [PeerConnectionPool] Peer local no configurado, usando modo legacy");
                return new PeerClient(gson);
            }
        });
    }

    /**
     * Envía una petición a un peer de forma síncrona.
     *
     * @param ip      Dirección IP del peer
     * @param puerto  Puerto del peer
     * @param request Petición a enviar
     * @return Respuesta del peer
     * @throws Exception si hay error en la comunicación
     */
    public DTOResponse enviarPeticion(String ip, int puerto, DTORequest request) throws Exception {
        PeerClient client = getClient(ip, puerto);
        return client.enviarPeticion(ip, puerto, request);
    }

    /**
     * Envía una petición a un peer de forma asíncrona.
     *
     * @param ip      Dirección IP del peer
     * @param puerto  Puerto del peer
     * @param request Petición a enviar
     * @return Future con la respuesta del peer
     */
    public Future<DTOResponse> enviarPeticionAsync(String ip, int puerto, DTORequest request) {
        return executorService.submit(() -> {
            PeerClient client = getClient(ip, puerto);
            return client.enviarPeticion(ip, puerto, request);
        });
    }

    /**
     * Verifica la conexión con un peer.
     *
     * @param ip     Dirección IP del peer
     * @param puerto Puerto del peer
     * @return true si el peer está disponible
     */
    public boolean verificarConexion(String ip, int puerto) {
        PeerClient client = getClient(ip, puerto);
        return client.verificarConexion(ip, puerto);
    }

    /**
     * Envía una petición a múltiples peers en paralelo.
     *
     * @param peers   Mapa de peers (clave: peerKey, valor: [ip, puerto])
     * @param request Petición a enviar
     * @return Mapa de respuestas (clave: peerKey, valor: respuesta)
     */
    public Map<String, Future<DTOResponse>> enviarPeticionMultiple(
            Map<String, String[]> peers, DTORequest request) {

        Map<String, Future<DTOResponse>> futures = new ConcurrentHashMap<>();

        for (Map.Entry<String, String[]> entry : peers.entrySet()) {
            String peerKey = entry.getKey();
            String[] peerInfo = entry.getValue();
            String ip = peerInfo[0];
            int puerto = Integer.parseInt(peerInfo[1]);

            Future<DTOResponse> future = enviarPeticionAsync(ip, puerto, request);
            futures.put(peerKey, future);
        }

        return futures;
    }

    /**
     * Broadcast: envía una petición a todos los peers registrados.
     *
     * @param peersInfo Lista de información de peers [[ip, puerto], ...]
     * @param request   Petición a enviar
     * @return Lista de futures con las respuestas
     */
    public Map<String, Future<DTOResponse>> broadcast(
            Map<UUID, String[]> peersInfo, DTORequest request) {

        Map<String, Future<DTOResponse>> futures = new ConcurrentHashMap<>();

        System.out.println("→ [PeerConnectionPool] Broadcasting a " + peersInfo.size() + " peers");

        for (Map.Entry<UUID, String[]> entry : peersInfo.entrySet()) {
            UUID peerId = entry.getKey();
            String[] info = entry.getValue();
            String ip = info[0];
            int puerto = Integer.parseInt(info[1]);

            Future<DTOResponse> future = enviarPeticionAsync(ip, puerto, request);
            futures.put(peerId.toString(), future);
        }

        return futures;
    }

    /**
     * Elimina un cliente del pool.
     *
     * @param ip     Dirección IP del peer
     * @param puerto Puerto del peer
     */
    public void removerCliente(String ip, int puerto) {
        String key = crearClave(ip, puerto);
        PeerClient removed = clientPool.remove(key);
        if (removed != null) {
            System.out.println("✓ [PeerConnectionPool] Cliente removido: " + ip + ":" + puerto);
        }
    }

    /**
     * Limpia todos los clientes del pool.
     */
    public void limpiarPool() {
        System.out.println("→ [PeerConnectionPool] Limpiando pool de " + clientPool.size() + " clientes");
        clientPool.clear();
        System.out.println("✓ [PeerConnectionPool] Pool limpiado");
    }

    /**
     * Cierra el pool y libera recursos.
     */
    public void cerrar() {
        System.out.println("→ [PeerConnectionPool] Cerrando pool...");
        limpiarPool();
        executorService.shutdown();
        System.out.println("✓ [PeerConnectionPool] Pool cerrado");
    }

    /**
     * Obtiene estadísticas del pool.
     *
     * @return Información sobre el estado del pool
     */
    public String obtenerEstadisticas() {
        return String.format(
                "PeerConnectionPool - Clientes activos: %d, Max threads: %d, Executor activo: %s",
                clientPool.size(),
                maxThreads,
                !executorService.isShutdown()
        );
    }

    /**
     * Obtiene el número de clientes en el pool.
     *
     * @return Número de clientes
     */
    public int getTamanoPool() {
        return clientPool.size();
    }

    /**
     * Crea una clave única para identificar un peer.
     */
    private String crearClave(String ip, int puerto) {
        return ip + ":" + puerto;
    }
}
