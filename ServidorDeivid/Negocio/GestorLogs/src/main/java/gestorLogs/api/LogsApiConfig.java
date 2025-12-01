package gestorLogs.api;

import dto.p2p.DTOPeerDetails;
import gestorLogs.GestorLogs;
import logger.LoggerCentral;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Configuración de Spring para la API de Logs
 * Define los beans necesarios para el funcionamiento de la API
 */
@Configuration
public class LogsApiConfig {

    private static final String TAG = "LogsApiConfig";
    // Instancia compartida del GestorLogs (inyectada desde fuera)
    private static GestorLogs gestorLogsCompartido;

    private static Supplier<List<DTOPeerDetails>> proveedorPeersCompartido;

    /**
     * Configura el GestorLogs que será usado por el API REST
     * Debe ser llamado ANTES de iniciar Spring Boot
     */
    public static void setGestorLogs(GestorLogs gestor) {
        gestorLogsCompartido = gestor;
        LoggerCentral.info(TAG, "GestorLogs compartido configurado para el API REST");
    }
    // Método para configurar el proveedor desde fuera (desde el Main o Controlador)
    public static void setProveedorPeers(Supplier<List<DTOPeerDetails>> supplier) {
        proveedorPeersCompartido = supplier;
        LoggerCentral.info(TAG, "Proveedor de Peers configurado para el API REST");
    }

    /**
     * Bean del GestorLogs
     * Retorna la instancia compartida que ya existe en la aplicación
     */
    @Bean
    public GestorLogs gestorLogs() {
        if (gestorLogsCompartido == null) {
            LoggerCentral.warn(TAG, "⚠️ No se configuró GestorLogs compartido, creando nueva instancia");
            gestorLogsCompartido = new GestorLogs();
        }
        LoggerCentral.info(TAG, "Usando GestorLogs compartido para el API REST");
        return gestorLogsCompartido;
    }
    @Bean
    public Supplier<List<DTOPeerDetails>> peerSupplier() {
        if (proveedorPeersCompartido == null) {
            LoggerCentral.warn(TAG, "⚠️ No se configuró proveedor de peers, retornando lista vacía por defecto");
            return ArrayList::new; // Retorna lista vacía si no se configuró nada
        }
        return proveedorPeersCompartido;
    }

    /**
     * Bean del controlador REST
     */
    @Bean
    public LogsRestController logsRestController(GestorLogs gestorLogs) {
        LoggerCentral.info(TAG, "Creando bean LogsRestController");
        return new LogsRestController(gestorLogs);
    }
    //Bean para el NetworkRestController
    @Bean
    public NetworkRestController networkRestController(Supplier<List<DTOPeerDetails>> peerSupplier) {
        return new NetworkRestController(peerSupplier);
    }
}
