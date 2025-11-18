package transporte;

import dto.gestionConexion.conexion.DTOSesion;

import java.io.IOException;

/**
 * Interfaz que representa el servidor de transporte TCP (aceptador de conexiones).
 * Define el contrato público para iniciar/detener el servicio y recibir sesiones aceptadas.
 */
public interface ITransporteServidor {

    /**
     * Handler funcional que recibirá cada sesión aceptada por el servidor.
     */
    @FunctionalInterface
    interface SesionHandler {
        void onSesionAceptada(DTOSesion sesion);
    }

    void iniciar(String host, int puerto, boolean esPoolPeers, SesionHandler handler) throws IOException;

    void detener();

    void setSoTimeoutMs(int soTimeoutMs);

    boolean estaArrancado();
}

