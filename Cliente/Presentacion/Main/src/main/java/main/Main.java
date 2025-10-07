package main;

import interfazEscritorio.app.VentanaPrincipal;
import javafx.application.Application;

/**
 * Clase principal que inicia la aplicación JavaFX.
 */
public class Main {
    /**
     * Punto de entrada principal para la aplicación.
     * Este método se encarga de lanzar la interfaz gráfica de JavaFX.
     *
     * @param args Argumentos de la línea de comandos.
     */
    public static void main(String[] args) {
        // Lanza la aplicación JavaFX, usando VentanaPrincipal como la clase de inicio.
        // La clase VentanaPrincipal ya extiende Application, por lo que este es el método
        // correcto para iniciarla desde una clase principal separada.
        Application.launch(VentanaPrincipal.class, args);
    }
}
