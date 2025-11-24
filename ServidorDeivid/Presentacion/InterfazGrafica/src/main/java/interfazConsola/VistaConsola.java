package interfazConsola;

import controlador.p2p.ControladorConsola;
import logger.LoggerCentral; // IMPORTANTE: Usamos tu paquete logger
import java.util.Scanner;

public class VistaConsola {

    private final ControladorConsola controladorConsola;
    private static final String PROMPT = "\u001B[36m[P2P] > \u001B[0m";

    public VistaConsola() {
        // Conectamos tu LoggerCentral con la consola segura
        LoggerCentral.setPrinter(this::imprimirMensajeSeguro);

        LoggerCentral.info("UI", "Cargando Vista...");
        this.controladorConsola = new ControladorConsola();
    }

    public void mostrarInterfaz() {
        try (Scanner scanner = new Scanner(System.in)) {
            imprimirBanner();

            while (true) {
                System.out.print(PROMPT);

                String input = "";
                try {
                    if (scanner.hasNextLine()) {
                        input = scanner.nextLine();
                    } else {
                        break;
                    }
                } catch (Exception e) { break; }

                if (input == null || input.trim().isEmpty()) continue;

                controladorConsola.procesarComando(input);

                if (input.trim().equalsIgnoreCase("EXIT")) break;
            }
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
        // ... (Tu banner ASCII aquí, puedes usar LoggerCentral.info para imprimirlo si quieres que quede en el log) ...
        System.out.println("\n--- SISTEMA P2P ACTIVO ---\n");
    }
}