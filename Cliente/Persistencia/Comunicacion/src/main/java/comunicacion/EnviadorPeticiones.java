package comunicacion;

import com.google.gson.Gson;
import conexion.GestorConexion;
import dto.comunicacion.DTORequest;
import dto.gestionConexion.conexion.DTOSesion;

import java.io.PrintWriter;

/**
 * Implementación del componente encargado de enviar peticiones al servidor.
 */
public class EnviadorPeticiones implements IEnviadorPeticiones {

    private final GestorConexion gestorConexion;
    private final Gson gson;

    public EnviadorPeticiones() {
        this.gestorConexion = GestorConexion.getInstancia();
        this.gson = new Gson();
    }

    @Override
    public void enviar(DTORequest request) {
        // CORRECCIÓN: Se utiliza el nombre de método correcto 'getSesion()'.
        DTOSesion sesion = gestorConexion.getSesion();

        if (sesion != null && sesion.estaActiva()) {
            try {
                PrintWriter out = sesion.getOut();
                String jsonRequest = gson.toJson(request);
                out.println(jsonRequest);
                System.out.println(">> Petición enviada: " + jsonRequest);
            } catch (Exception e) {
                // Es una buena práctica verificar si el socket se cerró para dar un mensaje más útil.
                if (sesion.getSocket().isClosed()) {
                    System.err.println("Error al enviar la petición: la conexión parece estar cerrada.");
                } else {
                    System.err.println("Error al enviar la petición: " + e.getMessage());
                }
            }
        } else {
            System.err.println("No se puede enviar la petición, no hay sesión activa.");
        }
    }
}

