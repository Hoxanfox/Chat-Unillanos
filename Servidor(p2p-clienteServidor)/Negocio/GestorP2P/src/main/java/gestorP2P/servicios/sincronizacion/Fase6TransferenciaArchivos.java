package gestorP2P.servicios.sincronizacion;

import gestorP2P.servicios.ServicioTransferenciaArchivos;
import logger.LoggerCentral;

/**
 * Fase 6: Transferencia de Archivos Físicos
 *
 * Responsabilidad: Descargar archivos físicos después de sincronizar metadatos.
 * Verifica que los archivos referenciados en BD existan en el Bucket/.
 */
public class Fase6TransferenciaArchivos {

    private static final String TAG = "Fase6-Archivos";
    private static final String AZUL = "\u001B[34m";
    private static final String VERDE = "\u001B[32m";
    private static final String AMARILLO = "\u001B[33m";
    private static final String RESET = "\u001B[0m";

    private ServicioTransferenciaArchivos servicioTransferencia;

    public Fase6TransferenciaArchivos() {
    }

    /**
     * Configura el servicio de transferencia de archivos.
     */
    public void setServicioTransferencia(ServicioTransferenciaArchivos servicio) {
        this.servicioTransferencia = servicio;
        if (servicio != null) {
            LoggerCentral.info(TAG, VERDE + "✓ Servicio de transferencia configurado" + RESET);
        }
    }

    /**
     * Verifica y descarga archivos faltantes después de sincronizar metadatos.
     */
    public void verificarYDescargarFaltantes() {
        if (servicioTransferencia == null) {
            LoggerCentral.warn(TAG, AMARILLO + "⚠ Servicio de transferencia no configurado" + RESET);
            return;
        }

        LoggerCentral.info(TAG, AZUL + "🔄 Verificando archivos físicos faltantes..." + RESET);

        // Ejecutar en thread separado para no bloquear
        new Thread(() -> {
            try {
                Thread.sleep(1000); // Esperar que se persistan los metadatos
                servicioTransferencia.verificarYDescargarArchivosFaltantes();
                LoggerCentral.info(TAG, VERDE + "✓ Verificación de archivos completada" + RESET);
            } catch (Exception e) {
                LoggerCentral.error(TAG, "Error verificando archivos: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Verifica si el servicio de transferencia está configurado.
     */
    public boolean estaConfigurado() {
        return servicioTransferencia != null;
    }
}