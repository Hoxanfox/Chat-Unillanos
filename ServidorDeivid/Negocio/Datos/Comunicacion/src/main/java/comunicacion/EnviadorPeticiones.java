package comunicacion;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import conexion.GestorConexion;
import conexion.TipoPool;
import dto.comunicacion.DTORequest;
import dto.gestionConexion.conexion.DTOSesion;
import logger.LoggerCentral;

import java.io.PrintWriter;

/**
 * Implementación del componente encargado de enviar peticiones al servidor.
 */
public class EnviadorPeticiones implements IEnviadorPeticiones {

    private final GestorConexion gestorConexion;
    private final Gson gson;

    public EnviadorPeticiones() {
        this.gestorConexion = GestorConexion.getInstancia();
        // ✅ CORRECCIÓN: Configurar Gson para serializar campos nulos
        // Esto es necesario para que peerRemitenteId y peerDestinoId se incluyan aunque sean null
        this.gson = new GsonBuilder()
                .serializeNulls()
                .create();
    }

    @Override
    public void enviar(DTORequest request) {
        // Comportamiento previo: por defecto usar el pool de CLIENTES
        enviar(request, TipoPool.CLIENTES);
    }

    /**
     * Enviar una petición usando el pool seleccionado (CLIENTES o PEERS).
     */
    public void enviar(DTORequest request, TipoPool tipoPool) {
        DTOSesion sesion = null;
        if (tipoPool == TipoPool.PEERS) {
            sesion = gestorConexion.obtenerSesionPeer(2000);
        } else {
            sesion = gestorConexion.obtenerSesionCliente(2000);
        }

        if (sesion != null && sesion.estaActiva()) {
            try {
                PrintWriter out = sesion.getOut();
                String jsonRequest = gson.toJson(request);
                out.println(jsonRequest);
                out.flush(); // asegurar que el mensaje se envía inmediatamente
                LoggerCentral.info("[" + tipoPool + "] >> Petición enviada: " + jsonRequest);
            } catch (Exception e) {
                // Es una buena práctica verificar si el socket se cerró para dar un mensaje más útil.
                try {
                    if (sesion.getSocket() != null && sesion.getSocket().isClosed()) {
                        LoggerCentral.error("Error al enviar la petición: la conexión parece estar cerrada.");
                    } else {
                        LoggerCentral.error("Error al enviar la petición: " + e.getMessage());
                    }
                } catch (Exception ex) {
                    LoggerCentral.error("Error al comprobar el estado de la sesión: " + ex.getMessage());
                }
            } finally {
                // Liberar la sesión en el pool correspondiente
                if (tipoPool == TipoPool.PEERS) gestorConexion.liberarSesionPeer(sesion);
                else gestorConexion.liberarSesionCliente(sesion);
            }
        } else {
            LoggerCentral.warn("No se puede enviar la petición, no hay sesión activa disponible en el pool " + tipoPool + ".");
        }
    }

    /**
     * Enviar una petición específicamente a la sesión que coincida con la IP y puerto indicados (si existe en el pool).
     * Devuelve true si se envió correctamente, false en caso de timeout o error.
     */
    public boolean enviarA(String ip, int port, DTORequest request, TipoPool tipoPool) {
        DTOSesion sesion = gestorConexion.obtenerSesionPorDireccion(ip, port, 2000, tipoPool == TipoPool.PEERS);
        if (sesion == null) {
            LoggerCentral.warn("No se encontró sesión para " + ip + ":" + port + " en pool " + tipoPool + ".");
            return false;
        }

        try {
            if (!sesion.estaActiva()) {
                LoggerCentral.warn("La sesión encontrada ya no está activa para " + ip + ":" + port + ".");
                return false;
            }
            PrintWriter out = sesion.getOut();
            String jsonRequest = gson.toJson(request);
            out.println(jsonRequest);
            out.flush();
            LoggerCentral.info("[" + tipoPool + "] >> Petición enviada a " + ip + ":" + port + " -> " + jsonRequest);
            return true;
        } catch (Exception e) {
            LoggerCentral.error("Error enviando petición a " + ip + ":" + port + ": " + e.getMessage(), e);
            return false;
        } finally {
            // Liberar la sesión de vuelta al pool
            if (tipoPool == TipoPool.PEERS) gestorConexion.liberarSesionPeer(sesion);
            else gestorConexion.liberarSesionCliente(sesion);
        }
    }
}
