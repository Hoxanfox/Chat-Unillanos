package gestorLogs.api;

import logger.LoggerCentral;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Aplicación Spring Boot para exponer los logs a través de REST API
 * Se inicia programáticamente desde la VentanaPrincipal
 */
@SpringBootApplication
public class LogsApiApplication {

    private static final String TAG = "LogsApiApplication";
    private static ConfigurableApplicationContext context;

    /**
     * Inicia el servidor REST API de forma programática
     * @param port Puerto en el que se ejecutará el servidor
     */
    public static void iniciar(int port) {
        if (context != null && context.isActive()) {
            LoggerCentral.warn(TAG, "El servidor REST API ya está en ejecución");
            return;
        }

        LoggerCentral.info(TAG, "========================================");
        LoggerCentral.info(TAG, "Iniciando Logs REST API Server");
        LoggerCentral.info(TAG, "========================================");

        String[] args = {
            "--server.port=" + port,
            "--spring.main.web-application-type=servlet"
        };

        new Thread(() -> {
            try {
                LoggerCentral.info(TAG, "Iniciando API en puerto " + port + "...");
                LoggerCentral.info(TAG, "Escaneando paquete: gestorLogs.api");
                LoggerCentral.info(TAG, "Tipo de aplicación: SERVLET (Web Server)");

                context = SpringApplication.run(LogsApiApplication.class, args);

                if (context != null && context.isActive()) {
                    LoggerCentral.info(TAG, "✓ Spring Boot context iniciado correctamente");

                    // Verificar si es un contexto web
                    if (context instanceof ServletWebServerApplicationContext) {
                        ServletWebServerApplicationContext webContext = (ServletWebServerApplicationContext) context;
                        int puertoReal = webContext.getWebServer().getPort();
                        LoggerCentral.info(TAG, "✓ Servidor Web Tomcat iniciado en puerto: " + puertoReal);
                    } else {
                        LoggerCentral.warn(TAG, "⚠️ El contexto NO es un servidor web - verificar dependencias");
                    }

                    LoggerCentral.info(TAG, "✓ Logs REST API iniciado exitosamente");
                    LoggerCentral.info(TAG, "✓ Endpoints disponibles en: http://localhost:" + port + "/api/logs");
                    LoggerCentral.info(TAG, "✓ Beans registrados: " + context.getBeanDefinitionCount());
                } else {
                    LoggerCentral.error(TAG, "✗ Error: Spring Boot context es null o inactivo");
                }

                LoggerCentral.info(TAG, "========================================");
            } catch (Exception e) {
                LoggerCentral.error(TAG, "✗ ERROR CRÍTICO al iniciar Spring Boot:");
                LoggerCentral.error(TAG, "  Mensaje: " + e.getMessage());
                LoggerCentral.error(TAG, "  Tipo: " + e.getClass().getName());
                e.printStackTrace();
                context = null;
            }
        }, "Thread-LogsRestAPI").start();
    }

    /**
     * Detiene el servidor REST API
     */
    public static void detener() {
        if (context != null && context.isActive()) {
            LoggerCentral.info(TAG, "Deteniendo Logs REST API Server");
            SpringApplication.exit(context, () -> 0);
            context = null;
            LoggerCentral.info(TAG, "✓ Logs REST API detenido");
        } else {
            LoggerCentral.warn(TAG, "El servidor REST API no está activo");
        }
    }

    /**
     * Verifica si el servidor REST API está activo
     */
    public static boolean estaActivo() {
        boolean activo = context != null && context.isActive();

        // Verificar también que sea un servidor web
        if (activo && context instanceof ServletWebServerApplicationContext) {
            ServletWebServerApplicationContext webContext = (ServletWebServerApplicationContext) context;
            try {
                int puerto = webContext.getWebServer().getPort();
                LoggerCentral.debug(TAG, "Estado del servidor: ACTIVO en puerto " + puerto);
                return true;
            } catch (Exception e) {
                LoggerCentral.debug(TAG, "Estado del servidor: ACTIVO pero sin web server");
                return false;
            }
        }

        LoggerCentral.debug(TAG, "Estado del servidor: " + (activo ? "ACTIVO (sin web)" : "INACTIVO"));
        return activo;
    }
}
