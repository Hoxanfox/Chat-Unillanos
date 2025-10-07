package transporte;

import dto.gestionConexion.conexion.DTOSesion;
import dto.gestionConexion.transporte.DTOConexion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Fábrica que crea conexiones TCP/IP y las empaqueta en un DTOSesion.
 * Esta clase ya no gestiona el estado de la conexión, solo la crea.
 */
public class TransporteTCP implements ITransporte {

    @Override
    public DTOSesion conectar(DTOConexion datosConexion) {
        try {
            System.out.println("Intentando conectar a " + datosConexion.getHost() + ":" + datosConexion.getPuerto());
            Socket socket = new Socket(datosConexion.getHost(), datosConexion.getPuerto());
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("Conexión establecida. Devolviendo recursos.");

            // Devuelve el DTO con los recursos puros.
            return new DTOSesion(socket, out, in);

        } catch (IOException e) {
            System.err.println("Error al crear la conexión: " + e.getMessage());
            // Si la conexión falla, devuelve null.
            return null;
        }
    }
}

