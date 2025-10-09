package gestionConexion;

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
 * Implementación que AHORA busca un archivo de configuración en
 * la carpeta raíz del proyecto (donde se ejecuta el JAR).
 */
public class GestionConexionImpl implements IGestionConexion {

    private final FabricaTransporte fabricaTransporte = new FabricaTransporte();
    private final GestorConexion gestorConexion = GestorConexion.getInstancia();
    // El nombre del archivo sin la barra inicial.
    private static final String CONFIG_FILE_NAME = "configuracion.txt";

    @Override
    public CompletableFuture<Boolean> conectar() {
        return CompletableFuture.supplyAsync(() -> {
            DTOConexion datosConexion = cargarConfiguracion();
            if (datosConexion == null) {
                System.err.println("GestionConexion: No se pudo cargar la configuración.");
                return false;
            }

            System.out.println("GestionConexion: Intentando conectar con los datos leídos...");
            DTOSesion sesion = fabricaTransporte.iniciarConexion(datosConexion);
            if (sesion != null && sesion.estaActiva()) {
                gestorConexion.setSesion(sesion);
                System.out.println("GestionConexion: Conexión exitosa y sesión guardada.");
                return true;
            }

            System.err.println("GestionConexion: La fábrica de transporte no pudo establecer una sesión activa.");
            return false;
        });
    }

    /**
     * Carga la configuración desde un archivo en la misma carpeta que el JAR.
     * @return un DTO con los datos de conexión, o null si no se puede leer.
     */
    private DTOConexion cargarConfiguracion() {
        // Se crea un objeto File que apunta al archivo en el directorio de trabajo actual.
        File configFile = new File(CONFIG_FILE_NAME);
        System.out.println("Buscando archivo de configuración en: " + configFile.getAbsolutePath());

        if (!configFile.exists()) {
            System.err.println("¡ERROR! No se encontró el archivo '" + CONFIG_FILE_NAME + "' en el directorio de ejecución.");
            return null;
        }

        // Se utiliza FileInputStream para leer un archivo del sistema de archivos.
        try (InputStream input = new FileInputStream(configFile)) {
            Properties prop = new Properties();
            prop.load(input);

            String ip = prop.getProperty("ip");
            int port = Integer.parseInt(prop.getProperty("port"));
            return new DTOConexion(ip, port);

        } catch (Exception e) {
            System.err.println("Error al leer o parsear el archivo de configuración: " + e.getMessage());
            return null;
        }
    }
}

