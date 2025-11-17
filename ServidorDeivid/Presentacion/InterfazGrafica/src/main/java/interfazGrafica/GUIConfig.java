package interfazGrafica;

import javax.swing.*;
import java.awt.*;

/**
 * Clase utilitaria para configurar la ventana gráfica (look-and-feel, tamaño, centrado, icono, etc.).
 */
public final class GUIConfig {

    private GUIConfig() {}

    public static void applyLookAndFeel() {
        try {
            // Preferir el look and feel del sistema
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Si falla, seguir con el LAF por defecto
        }
    }

    public static void configureFrame(JFrame frame) {
        if (frame == null) return;
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 640);
        frame.setMinimumSize(new Dimension(640, 480));
        frame.setLocationRelativeTo(null); // centrar
        // Puedes añadir un icono si existe: frame.setIconImage(...);
    }
}

