package gestionConexion;

import comunicacion.GestorRespuesta;
import conexion.GestorConexion;
import dto.gestionConexion.transporte.DTOConexion;
import dto.gestionConexion.conexion.DTOSesion;
import transporte.FabricaTransporte;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Su única responsabilidad es conectar, guardar la sesión e inicializar el sistema.
 */
public class GestionConexionImpl implements IGestionConexion {

    private static final Logger LOGGER = Logger.getLogger(GestionConexionImpl.class.getName());

    private final FabricaTransporte fabricaTransporte = new FabricaTransporte();
    private final GestorConexion gestorConexion = GestorConexion.getInstancia();
    private static final String CONFIG_FILE_NAME = "configuracion.txt";

    @Override
    public CompletableFuture<Boolean> conectar() {
        LOGGER.fine("[GestionConexion] (Thread:" + Thread.currentThread().getName() + ") => iniciar conectar()");
        return CompletableFuture.supplyAsync(() -> {
            DTOConexion datosConexion = cargarConfiguracion();
            if (datosConexion == null) {
                LOGGER.severe("[GestionConexion] No se pudo cargar la configuración. Abortando conexión.");
                return false;
            }

            LOGGER.info("[GestionConexion] Iniciando conexión a " + datosConexion.getHost() + ":" + datosConexion.getPuerto());
            DTOSesion sesion = null;
            try {
                sesion = fabricaTransporte.iniciarConexion(datosConexion);
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "[GestionConexion] Excepción al iniciar conexión", ex);
            }

            if (sesion != null && sesion.estaActiva()) {
                LOGGER.info("[GestionConexion] Sesión activa obtenida. Notificando GestorConexion...");
                try {
                    gestorConexion.setSesion(sesion);
                    LOGGER.info("[GestionConexion] GestorConexion.setSesion completado correctamente.");
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "[GestionConexion] Error al setear la sesión en GestorConexion", ex);
                }

                LOGGER.info("[GestionConexion] Iniciando escucha de respuestas (GestorRespuesta)...");
                try {
                    GestorRespuesta.getInstancia().iniciarEscucha();
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "[GestionConexion] Error al iniciar escucha de respuestas", ex);
                }

                LOGGER.info("[GestionConexion] Conexión completada con éxito.");
                return true;
            }

            LOGGER.warning("[GestionConexion] No se pudo establecer sesión o la sesión no está activa.");
            return false;
        });
    }

    private DTOConexion cargarConfiguracion() {
        Properties prop = new Properties();
        try (InputStream input = new FileInputStream(CONFIG_FILE_NAME)) {
            prop.load(input);
            String ip = prop.getProperty("ip");
            int port = Integer.parseInt(prop.getProperty("port"));
            LOGGER.info("[GestionConexion] Configuración cargada: ip=" + ip + ", port=" + port);
            return new DTOConexion(ip, port);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[GestionConexion] ¡ERROR! No se encontró o no se pudo leer el archivo '" + CONFIG_FILE_NAME + "'.", e);
            return null;
        }
    }
}
