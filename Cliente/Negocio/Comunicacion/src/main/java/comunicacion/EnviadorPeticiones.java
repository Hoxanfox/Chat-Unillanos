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
    private final Gson gson; // Dependencia para serializar a JSON

    public EnviadorPeticiones() {
        this.gestorConexion = GestorConexion.getInstancia();
        this.gson = new Gson();
        // NOTA: Para que esto funcione, necesitas añadir la librería Gson a tu proyecto.
    }

    @Override
    public void enviar(DTORequest request) {
        DTOSesion sesion = gestorConexion.getSesionActiva();

        if (sesion != null) {
            try {
                PrintWriter out = sesion.getOut();
                String jsonRequest = gson.toJson(request);
                out.println(jsonRequest);
                System.out.println(">> Petición enviada: " + jsonRequest);
            } catch (Exception e) {
                System.err.println("Error al enviar la petición: " + e.getMessage());
            }
        } else {
            System.err.println("No se puede enviar la petición, no hay sesión activa.");
        }
    }
}
