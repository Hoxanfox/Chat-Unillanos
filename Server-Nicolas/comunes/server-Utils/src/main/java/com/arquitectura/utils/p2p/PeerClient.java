package com.arquitectura.utils.p2p;

import com.arquitectura.DTO.Comunicacion.DTORequest;
import com.arquitectura.DTO.Comunicacion.DTOResponse;
import com.google.gson.Gson;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * Cliente para comunicación entre servidores (peer-to-peer).
 * Permite enviar peticiones de un servidor a otro en la red P2P.
 */
public class PeerClient {

    private final Gson gson;
    private static final int DEFAULT_TIMEOUT = 10000; // 10 segundos
    private static final int BUFFER_SIZE = 8192;

    public PeerClient() {
        this.gson = new Gson();
    }

    public PeerClient(Gson gson) {
        this.gson = gson;
    }

    /**
     * Envía una petición a otro peer y espera la respuesta.
     *
     * @param ip      Dirección IP del peer destino
     * @param puerto  Puerto del peer destino
     * @param request Petición a enviar
     * @return Respuesta del peer
     * @throws Exception si hay error en la comunicación
     */
    public DTOResponse enviarPeticion(String ip, int puerto, DTORequest request) throws Exception {
        return enviarPeticion(ip, puerto, request, DEFAULT_TIMEOUT);
    }

    /**
     * Envía una petición a otro peer con timeout personalizado.
     *
     * @param ip       Dirección IP del peer destino
     * @param puerto   Puerto del peer destino
     * @param request  Petición a enviar
     * @param timeout  Timeout en milisegundos
     * @return Respuesta del peer
     * @throws Exception si hay error en la comunicación
     */
    public DTOResponse enviarPeticion(String ip, int puerto, DTORequest request, int timeout) throws Exception {
        System.out.println("→ [PeerClient] Conectando a peer: " + ip + ":" + puerto);

        Socket socket = null;
        BufferedReader reader = null;
        PrintWriter writer = null;

        try {
            // Crear conexión con timeout
            socket = new Socket(ip, puerto);
            socket.setSoTimeout(timeout);

            // Configurar streams
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            // Serializar y enviar petición
            String requestJson = gson.toJson(request);
            System.out.println("→ [PeerClient] Enviando petición: " + request.getAction());
            writer.println(requestJson);
            writer.flush();

            // Leer respuesta
            System.out.println("→ [PeerClient] Esperando respuesta...");
            String responseJson = reader.readLine();

            if (responseJson == null || responseJson.trim().isEmpty()) {
                throw new IOException("Respuesta vacía del peer");
            }

            // Deserializar respuesta
            DTOResponse response = gson.fromJson(responseJson, DTOResponse.class);
            System.out.println("✓ [PeerClient] Respuesta recibida: " + response.getStatus());

            return response;

        } catch (SocketTimeoutException e) {
            System.err.println("✗ [PeerClient] Timeout al comunicarse con peer: " + ip + ":" + puerto);
            throw new Exception("Timeout al comunicarse con el peer", e);

        } catch (IOException e) {
            System.err.println("✗ [PeerClient] Error de I/O: " + e.getMessage());
            throw new Exception("Error de comunicación con el peer: " + e.getMessage(), e);

        } finally {
            // Cerrar recursos
            cerrarRecursos(reader, writer, socket);
        }
    }

    /**
     * Verifica si un peer está disponible y responde.
     *
     * @param ip     Dirección IP del peer
     * @param puerto Puerto del peer
     * @return true si el peer está disponible, false en caso contrario
     */
    public boolean verificarConexion(String ip, int puerto) {
        return verificarConexion(ip, puerto, 3000); // 3 segundos de timeout
    }

    /**
     * Verifica si un peer está disponible con timeout personalizado.
     *
     * @param ip      Dirección IP del peer
     * @param puerto  Puerto del peer
     * @param timeout Timeout en milisegundos
     * @return true si el peer está disponible, false en caso contrario
     */
    public boolean verificarConexion(String ip, int puerto, int timeout) {
        Socket socket = null;
        try {
            socket = new Socket(ip, puerto);
            socket.setSoTimeout(timeout);
            System.out.println("✓ [PeerClient] Peer disponible: " + ip + ":" + puerto);
            return true;

        } catch (Exception e) {
            System.out.println("✗ [PeerClient] Peer no disponible: " + ip + ":" + puerto);
            return false;

        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    // Ignorar error al cerrar
                }
            }
        }
    }

    /**
     * Envía una petición sin esperar respuesta (fire and forget).
     *
     * @param ip      Dirección IP del peer destino
     * @param puerto  Puerto del peer destino
     * @param request Petición a enviar
     * @throws Exception si hay error al enviar
     */
    public void enviarPeticionSinRespuesta(String ip, int puerto, DTORequest request) throws Exception {
        System.out.println("→ [PeerClient] Enviando petición sin esperar respuesta a: " + ip + ":" + puerto);

        Socket socket = null;
        PrintWriter writer = null;

        try {
            socket = new Socket(ip, puerto);
            socket.setSoTimeout(3000); // Timeout corto

            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            String requestJson = gson.toJson(request);
            writer.println(requestJson);
            writer.flush();

            System.out.println("✓ [PeerClient] Petición enviada");

        } catch (IOException e) {
            System.err.println("✗ [PeerClient] Error al enviar petición: " + e.getMessage());
            throw new Exception("Error al enviar petición al peer", e);

        } finally {
            cerrarRecursos(null, writer, socket);
        }
    }

    /**
     * Cierra los recursos de forma segura.
     */
    private void cerrarRecursos(BufferedReader reader, PrintWriter writer, Socket socket) {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                System.err.println("Error al cerrar reader: " + e.getMessage());
            }
        }

        if (writer != null) {
            try {
                writer.close();
            } catch (Exception e) {
                System.err.println("Error al cerrar writer: " + e.getMessage());
            }
        }

        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Error al cerrar socket: " + e.getMessage());
            }
        }
    }

    /**
     * Obtiene información sobre el estado de la conexión.
     *
     * @param ip     Dirección IP del peer
     * @param puerto Puerto del peer
     * @return Información de diagnóstico
     */
    public String obtenerInfoConexion(String ip, int puerto) {
        boolean disponible = verificarConexion(ip, puerto);
        return String.format("Peer %s:%d - Estado: %s", ip, puerto, disponible ? "DISPONIBLE" : "NO DISPONIBLE");
    }
}
