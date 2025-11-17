package transporte;

import dto.gestionConexion.conexion.DTOSesion;
import dto.gestionConexion.transporte.DTOConexion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import logger.LoggerCentral;

/**
 * Implementación de ITransporte que crea sesiones TCP/IP.
 * Es una clase sin estado, cuya única función es la de "creador" de sesiones.
 */
public class TransporteTCP implements ITransporte {

    // Timeout en milisegundos para la operación connect
    private static final int CONNECT_TIMEOUT_MS = 2000;

    @Override
    public DTOSesion conectar(DTOConexion datosConexion) {
        Socket socket = null;
        try {
            socket = new Socket();
            LoggerCentral.debug("TransporteTCP: socket creado. isBound=" + socket.isBound() + " isClosed=" + socket.isClosed());
            LoggerCentral.debug("TransporteTCP: intentando conectar a " + datosConexion.getHost() + ":" + datosConexion.getPuerto() + " con timeout=" + CONNECT_TIMEOUT_MS + "ms");

            socket.connect(new InetSocketAddress(datosConexion.getHost(), datosConexion.getPuerto()), CONNECT_TIMEOUT_MS);

            // Log de direcciones para diagnóstico
            try {
                LoggerCentral.debug("TransporteTCP: socket local=" + socket.getLocalSocketAddress() + " remote=" + socket.getRemoteSocketAddress());
                LoggerCentral.debug("TransporteTCP: conectado? " + socket.isConnected() + " cerrado? " + socket.isClosed());
                try {
                    int soTimeout = socket.getSoTimeout();
                    LoggerCentral.debug("TransporteTCP: soTimeout=" + soTimeout);
                } catch (Exception e) {
                    LoggerCentral.debug("TransporteTCP: no se pudo obtener soTimeout: " + e.getMessage());
                }
            } catch (Exception e) {
                LoggerCentral.debug("TransporteTCP: no se pudieron obtener direcciones del socket: " + e.getMessage());
            }

            // Envolvemos el OutputStream con un PrintWriter que registra lo que se envía
            PrintWriter out = new LoggingPrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            LoggerCentral.info("Conexión establecida con " + datosConexion.getHost() + ":" + datosConexion.getPuerto());

            return new DTOSesion(socket, out, in);

        } catch (SocketTimeoutException e) {
            LoggerCentral.error("Timeout al conectar con " + datosConexion.getHost() + ":" + datosConexion.getPuerto() + " (" + CONNECT_TIMEOUT_MS + "ms)", e);
            if (socket != null && !socket.isClosed()) {
                try { socket.close(); } catch (IOException ignored) {}
            }
            return null;
        } catch (IOException e) {
            LoggerCentral.error("Error al crear la conexión", e);
            if (socket != null && !socket.isClosed()) {
                try { socket.close(); } catch (IOException ignored) {}
            }
            return null;
        }
    }
}
