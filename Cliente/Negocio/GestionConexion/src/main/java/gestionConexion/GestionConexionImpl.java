package gestionConexion;

import comunicacion.GestorRespuesta;
import conexion.GestorConexion;
import dto.gestionConexion.transporte.DTOConexion;
import dto.gestionConexion.conexion.DTOSesion;
import transporte.FabricaTransporte;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/**
 * Su única responsabilidad es conectar, guardar la sesión e inicializar el sistema.
 */
public class GestionConexionImpl implements IGestionConexion {

    private final FabricaTransporte fabricaTransporte = new FabricaTransporte();
    private final GestorConexion gestorConexion = GestorConexion.getInstancia();
    private static final String CONFIG_FILE_NAME = "configuracion.txt";

    @Override
    public CompletableFuture<Boolean> conectar() {
        return CompletableFuture.supplyAsync(() -> {
            DTOConexion datosConexion = cargarConfiguracion();
            if (datosConexion == null) return false;

            DTOSesion sesion = fabricaTransporte.iniciarConexion(datosConexion);

            if (sesion != null && sesion.estaActiva()) {
                gestorConexion.setSesion(sesion);
                GestorRespuesta.getInstancia().iniciarEscucha();

                return true;
            }
            return false;
        });
    }

    private DTOConexion cargarConfiguracion() {
        Properties prop = new Properties();
        try (InputStream input = new FileInputStream(new File(CONFIG_FILE_NAME))) {
            prop.load(input);
            String ip = prop.getProperty("ip");
            int port = Integer.parseInt(prop.getProperty("port"));
            return new DTOConexion(ip, port);
        } catch (Exception e) {
            System.err.println("¡ERROR! No se encontró o no se pudo leer el archivo '" + CONFIG_FILE_NAME + "'.");
            return null;
        }
    }
}
