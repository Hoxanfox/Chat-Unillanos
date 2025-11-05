package transporte;

import dto.gestionConexion.conexion.DTOSesion;
import dto.gestionConexion.transporte.DTOConexion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

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
            System.out.println("Conexión establecida. Devolviendo recursos en DTOSesion.");

            return new DTOSesion(socket, out, in);

        } catch (IOException e) {
            System.err.println("Error al crear la conexión: " + e.getMessage());
            return null;
        }
    }
}

