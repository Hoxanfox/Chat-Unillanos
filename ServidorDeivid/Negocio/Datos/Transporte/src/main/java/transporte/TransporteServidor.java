package transporte;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

import dto.gestionConexion.conexion.DTOSesion;
import logger.LoggerCentral;

/**
 * Servicio que acepta conexiones TCP entrantes y las registra mediante un handler suministrado por el llamador.
 * Se puede iniciar vinculando a una dirección/puerto concretos.
 */
public class TransporteServidor {

    // Handler simple para notificar sesiones aceptadas al módulo llamador
    public interface SesionHandler {
        void onSesionAceptada(DTOSesion sesion);
    }

    private ServerSocket serverSocket;
    private Thread hiloAceptador;
    private final AtomicBoolean arrancado = new AtomicBoolean(false);
    private boolean esPoolPeers = true;
    private SesionHandler handler;
    private int soTimeoutMs = 1000; // tiempo para desbloquear accept periódicamente

    public TransporteServidor() {}

    /**
     * Inicia el servidor enlazado a la dirección/puerto indicados.
     * @param host dirección local a la que enlazar (puede ser null o empty -> 0.0.0.0)
     * @param puerto puerto local
     * @param esPoolPeers true si las sesiones aceptadas deben tratarse como PEERS (solo para logging)
     * @param handler callback que recibirá cada DTOSesion aceptada (no puede ser null)
     * @throws IOException si falla el bind o la apertura del ServerSocket
     */
    public void iniciar(String host, int puerto, boolean esPoolPeers, SesionHandler handler) throws IOException {
        if (arrancado.get()) {
            LoggerCentral.warn("TransporteServidor ya está arrancado en el puerto " + (serverSocket != null ? serverSocket.getLocalPort() : "?"));
            return;
        }
        if (handler == null) throw new IllegalArgumentException("handler no puede ser null");
        this.handler = handler;
        this.esPoolPeers = esPoolPeers;

        serverSocket = new ServerSocket();
        if (host == null || host.trim().isEmpty()) {
            serverSocket.bind(new InetSocketAddress(puerto));
        } else {
            serverSocket.bind(new InetSocketAddress(host, puerto));
        }
        serverSocket.setSoTimeout(soTimeoutMs);
        arrancado.set(true);

        hiloAceptador = new Thread(this::bucleAceptador, "TransporteServidor-Acceptor-" + puerto);
        hiloAceptador.setDaemon(true);
        hiloAceptador.start();
        LoggerCentral.info("TransporteServidor iniciado en " + (host == null || host.isEmpty() ? "0.0.0.0" : host) + ":" + puerto + " -> pool=" + (esPoolPeers ? "PEERS" : "CLIENTES"));
    }

    private void bucleAceptador() {
        while (arrancado.get()) {
            try {
                Socket socket = serverSocket.accept(); // espera con timeout
                LoggerCentral.info("TransporteServidor: conexión entrante desde " + socket.getRemoteSocketAddress());
                try {
                    // envolver streams
                    LoggingPrintWriter out = new LoggingPrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    DTOSesion sesion = new DTOSesion(socket, out, in);

                    // Notificar al handler
                    try {
                        handler.onSesionAceptada(sesion);
                        LoggerCentral.debug("TransporteServidor: sesión entregada al handler -> " + sesion);
                    } catch (Exception e) {
                        LoggerCentral.error("TransporteServidor: error en handler al procesar sesión -> " + e.getMessage(), e);
                        try { socket.close(); } catch (IOException ignored) {}
                    }

                } catch (Exception e) {
                    LoggerCentral.error("TransporteServidor: error al envolver la conexión entrante: " + e.getMessage(), e);
                    try { socket.close(); } catch (IOException ignored) {}
                }
            } catch (IOException e) {
                // Puede ser un timeout de accept o error real
                if (!arrancado.get()) break;
                // Si es timeout, seguir; si es otra excepción, loggear y continuar
                LoggerCentral.debug("TransporteServidor: accept interrumpido/timeout o error menor: " + e.getMessage());
            }
        }
        LoggerCentral.info("TransporteServidor: bucle de aceptación finalizado.");
    }

    /**
     * Detiene el servidor (cierra el ServerSocket) y espera a que el hilo termine.
     */
    public void detener() {
        if (!arrancado.get()) return;
        arrancado.set(false);
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            LoggerCentral.debug("TransporteServidor: error cerrando ServerSocket: " + e.getMessage());
        }
        try {
            if (hiloAceptador != null) hiloAceptador.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        LoggerCentral.info("TransporteServidor detenido.");
    }

    /**
     * Ajusta el timeout usado en accept para permitir parada rápida (ms).
     */
    public void setSoTimeoutMs(int soTimeoutMs) {
        this.soTimeoutMs = Math.max(100, soTimeoutMs);
        try {
            if (serverSocket != null) serverSocket.setSoTimeout(this.soTimeoutMs);
        } catch (Exception ignored) {}
    }

    public boolean estaArrancado() {
        return arrancado.get();
    }
}
