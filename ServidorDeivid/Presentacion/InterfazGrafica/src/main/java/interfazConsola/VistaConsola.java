package interfazConsola;

import controlador.p2p.ControladorConsola;
import logger.LoggerCentral;
import java.util.Scanner;

public class VistaConsola {

    private static final String TAG = "VistaConsola";
    private final ControladorConsola controladorConsola;
    private static final String PROMPT = "\u001B[36m[P2P] > \u001B[0m";

    public VistaConsola() {
        LoggerCentral.info(TAG, "Inicializando VistaConsola...");

        // Conectamos tu LoggerCentral con la consola segura
        LoggerCentral.setPrinter(this::imprimirMensajeSeguro);
        LoggerCentral.debug(TAG, "Printer personalizado configurado en LoggerCentral.");

        LoggerCentral.info(TAG, "Cargando Vista...");
        this.controladorConsola = new ControladorConsola();
        LoggerCentral.info(TAG, "VistaConsola inicializada correctamente.");
    }

    public void mostrarInterfaz() {
        LoggerCentral.info(TAG, "Mostrando interfaz de consola interactiva...");

        try (Scanner scanner = new Scanner(System.in)) {
            imprimirBanner();
            LoggerCentral.debug(TAG, "Banner impreso. Iniciando loop de comandos...");

            int comandosEjecutados = 0;

            while (true) {
                System.out.print(PROMPT);

                String input = "";
                try {
                    if (scanner.hasNextLine()) {
                        input = scanner.nextLine();
                        LoggerCentral.debug(TAG, "Entrada recibida del usuario: [" + input + "]");
                    } else {
                        LoggerCentral.warn(TAG, "Scanner no tiene más líneas. Terminando interfaz.");
                        break;
                    }
                } catch (Exception e) {
                    LoggerCentral.error(TAG, "Error leyendo entrada del usuario: " + e.getMessage());
                    break;
                }

                if (input == null || input.trim().isEmpty()) {
                    LoggerCentral.debug(TAG, "Entrada vacía ignorada.");
                    continue;
                }

                comandosEjecutados++;
                LoggerCentral.info(TAG, "Procesando comando #" + comandosEjecutados + ": [" + input.trim() + "]");

                controladorConsola.procesarComando(input);

                if (input.trim().equalsIgnoreCase("EXIT")) {
                    LoggerCentral.info(TAG, "Comando EXIT detectado. Terminando interfaz.");
                    LoggerCentral.info(TAG, "Total de comandos ejecutados en esta sesión: " + comandosEjecutados);
                    break;
                }
            }

            LoggerCentral.info(TAG, "Interfaz de consola cerrada correctamente.");
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error fatal en la interfaz de consola: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Borra la línea del prompt, imprime el log y vuelve a pintar el prompt.
     */
    private void imprimirMensajeSeguro(String mensaje) {
        System.out.print("\r\033[K"); // Borrar línea actual
        System.out.println(mensaje);  // Imprimir log
        System.out.print(PROMPT);     // Repintar prompt
    }

    private void imprimirBanner() {
        LoggerCentral.debug(TAG, "Imprimiendo banner del sistema...");
        System.out.println("\n--- SISTEMA P2P ACTIVO ---\n");
    }
}