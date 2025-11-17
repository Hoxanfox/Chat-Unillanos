package interfazGrafica;

import javax.swing.*;

/**
 * Clase lanzadora para la interfaz gráfica. Ejecutar este main arranca la UI.
 */
public class Launcher {
    public static void main(String[] args) {
        GUIConfig.applyLookAndFeel();
        SwingUtilities.invokeLater(() -> {
            MainWindow mw = new MainWindow();
            mw.setVisible(true);
        });
    }
}

