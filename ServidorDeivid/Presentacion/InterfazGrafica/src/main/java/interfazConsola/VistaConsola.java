package interfazConsola;

import controlador.p2p.ControladorConsola;
import java.util.Scanner;

public class VistaConsola {

    private final ControladorConsola controladorConsola;

    // --- COLORES ---
    private static final String RESET = "\u001B[0m";
    private static final String VERDE = "\u001B[32m";
    private static final String AMARILLO = "\u001B[33m";
    private static final String AZUL = "\u001B[34m";
    private static final String CYAN = "\u001B[36m";
    private static final String BLANCO_BR = "\033[1;37m";

    public VistaConsola() {
        System.out.println(AMARILLO + "[UI] Cargando Vista..." + RESET);
        this.controladorConsola = new ControladorConsola();
    }

    public void mostrarInterfaz() {
        Scanner scanner = new Scanner(System.in);
        imprimirBanner();

        while (true) {
            System.out.print(CYAN + "\n┌──(Peer" + AMARILLO + "㉿" + CYAN + "Usuario)-[P2P]" + RESET);
            System.out.print(CYAN + "\n└─$ " + RESET);

            String input = "";
            try {
                input = scanner.nextLine();
            } catch (Exception e) {
                break; // Salir si se cierra el stream
            }

            if (input == null || input.trim().isEmpty()) continue;

            controladorConsola.procesarComando(input);
        }
        scanner.close();
    }

    private void imprimirBanner() {
        System.out.println(AZUL);
        System.out.println("██████╗ ██████╗ ██████╗");
        System.out.println("██╔══██╗╚════██╗██╔══██╗");
        System.out.println("██████╔╝ █████╔╝██████╔╝");
        System.out.println("██╔═══╝  ╚═══██╗██╔═══╝ ");
        System.out.println("██║     ██████╔╝██║     ");
        System.out.println("╚═╝     ╚═════╝ ╚═╝     ");
        System.out.println(RESET);
        System.out.println(BLANCO_BR + "   SISTEMA P2P v2.0   " + RESET);
        System.out.println(VERDE + "   Escribe HELP       " + RESET);
        System.out.println("========================");
    }
}