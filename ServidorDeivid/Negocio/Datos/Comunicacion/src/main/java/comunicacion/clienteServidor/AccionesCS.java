package comunicacion.clienteServidor;

import comunicacion.IRouterMensajes;
import conexion.IGestorConexiones;
import dto.comunicacion.DTOResponse;
import com.google.gson.JsonObject;

/**
 * Contiene la lógica de las acciones relacionadas con el modelo Cliente-Servidor.
 * Ejemplos: Login de usuario, descargar archivo, consultar base de datos centralizada.
 */
public class AccionesCS {

    private final IGestorConexiones gestorConexiones;

    public AccionesCS(IGestorConexiones gestorConexiones) {
        this.gestorConexiones = gestorConexiones;
    }

    public void registrarAcciones(IRouterMensajes router) {

        // --- EJEMPLO: login ---
        router.registrarAccion("login", (datosJson, origenId) -> {
            System.out.println("[CS] Intento de login desde: " + origenId);

            // Lógica simulada
            if (datosJson != null && datosJson.isJsonObject()) {
                JsonObject credenciales = datosJson.getAsJsonObject();
                if (credenciales.has("usuario")) {
                    return new DTOResponse("login", "success", "Bienvenido usuario", null);
                }
            }
            return new DTOResponse("login", "error", "Credenciales inválidas", null);
        });
    }
}