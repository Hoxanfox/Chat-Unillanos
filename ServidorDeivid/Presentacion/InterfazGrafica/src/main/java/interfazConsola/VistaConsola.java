package interfazConsola;

import controlador.p2p.ControladorP2P;
import java.util.Scanner;

public class VistaConsola {

    private final ControladorP2P controlador;

    /**
     * Constructor SIN parámetros (El que usará el Main).
     * Se encarga de crear la instancia del Controlador automáticamente.
     */
    public VistaConsola() {
        // Aquí inicializamos la dependencia hacia abajo
        this.controlador = new ControladorP2P();
    }

    /**
     * Constructor CON parámetros (Para pruebas o inyección manual).
     */
    public VistaConsola(ControladorP2P controlador) {
        this.controlador = controlador;
    }

    public void mostrarInterfaz() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== SISTEMA P2P ===");
        System.out.println("Escribe 'START' para iniciar o 'EXIT' para salir.");

        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine();
            // Ahora es seguro usar controlador, nunca será null
            controlador.procesarComando(input);
        }
    }
}