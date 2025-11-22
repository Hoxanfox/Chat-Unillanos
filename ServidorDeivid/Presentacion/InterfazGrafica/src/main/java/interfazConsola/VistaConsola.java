package interfazConsola;

import controlador.p2p.ControladorConsola;
import java.util.Scanner;

/**
 * Interfaz de usuario (CLI) con estilo hacker/profesional y trazas de debug.
 */
public class VistaConsola {

    private final ControladorConsola controladorConsola;

    // --- CÓDIGOS DE COLOR ANSI PARA ESTILO ---
    private static final String RESET = "\u001B[0m";
    private static final String ROJO = "\u001B[31m";
    private static final String VERDE = "\u001B[32m";
    private static final String AMARILLO = "\u001B[33m";
    private static final String AZUL = "\u001B[34m";
    private static final String CYAN = "\u001B[36m";
    private static final String BLANCO_BRILLANTE = "\033[1;37m";

    public VistaConsola() {
        printDebug("Instanciando VistaConsola...");
        this.controladorConsola = new ControladorConsola();
        printDebug("ControladorConsola conectado correctamente.");
    }

    public void mostrarInterfaz() {
        Scanner scanner = new Scanner(System.in);
        printDebug("Scanner de entrada inicializado.");

        imprimirBanner();
        printDebug("Banner renderizado. Entrando al bucle principal (Event Loop).");

        while (true) {
            // Prompt estilo terminal Linux/Hacker
            System.out.print(CYAN + "\n┌──(Peer" + AMARILLO + "㉿" + CYAN + "Usuario)-[RedP2P]" + RESET);
            System.out.print(CYAN + "\n└─$ " + RESET);

            String input;
            try {
                input = scanner.nextLine();
            } catch (Exception e) {
                printError("Error crítico leyendo entrada: " + e.getMessage());
                break;
            }

            if (input == null || input.trim().isEmpty()) {
                continue;
            }

            // printDebug("Input capturado (Raw): '" + input + "'");

            long inicio = System.currentTimeMillis();

            // Delegamos el procesamiento de texto al Controlador de Consola
            controladorConsola.procesarComando(input);

            long fin = System.currentTimeMillis();
            // printDebug("Comando procesado en " + (fin - inicio) + "ms");
        }
        scanner.close();
    }

    private void imprimirBanner() {
        System.out.println(AZUL);
        System.out.println("██████╗ ██████╗ ██████╗     ███╗   ██╗███████╗████████╗");
        System.out.println("██╔══██╗╚════██╗██╔══██╗    ████╗  ██║██╔════╝╚══██╔══╝");
        System.out.println("██████╔╝ █████╔╝██████╔╝    ██╔██╗ ██║█████╗     ██║   ");
        System.out.println("██╔═══╝  ╚═══██╗██╔═══╝     ██║╚██╗██║██╔══╝     ██║   ");
        System.out.println("██║     ██████╔╝██║         ██║ ╚████║███████╗   ██║   ");
        System.out.println("╚═╝     ╚═════╝ ╚═╝         ╚═╝  ╚═══╝╚══════╝   ╚═╝   ");
        System.out.println(RESET);
        System.out.println(BLANCO_BRILLANTE + "       >>> SISTEMA P2P DESCENTRALIZADO v2.0 <<<       " + RESET);
        System.out.println(VERDE + "   Escribe 'HELP' para ver la lista de comandos." + RESET);
        System.out.println("========================================================");
    }

    private void printDebug(String msg) {
        System.out.println(AMARILLO + "[DEBUG-UI] " + msg + RESET);
    }

    private void printError(String msg) {
        System.out.println(ROJO + "[ERROR-UI] " + msg + RESET);
    }
}