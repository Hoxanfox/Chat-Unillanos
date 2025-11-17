package transporte;

import dto.gestionConexion.conexion.DTOSesion;
import dto.gestionConexion.transporte.DTOConexion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import logger.LoggerCentral;

/**
 * Implementación de ITransporte que crea sesiones TCP/IP.
 * Es una clase sin estado, cuya única función es la de "creador" de sesiones.
 */
public class TransporteTCP implements ITransporte {

    @Override
    public DTOSesion conectar(DTOConexion datosConexion) {
        try {
            Socket socket = new Socket(datosConexion.getHost(), datosConexion.getPuerto());
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            LoggerCentral.info("Conexión establecida con " + datosConexion.getHost() + ":" + datosConexion.getPuerto());

            return new DTOSesion(socket, out, in);

        } catch (IOException e) {
            LoggerCentral.error("Error al crear la conexión", e);
            return null;
        }
    }
}
