package interfazGrafica.features.conexiones;

import javax.swing.*;
import java.awt.*;

/**
 * Panel para la gestión de conexiones activas (vista mínima).
 */
public class ConexionesPanel extends JPanel {

    public ConexionesPanel() {
        init();
    }

    private void init() {
        setLayout(new BorderLayout(8,8));
        JLabel title = new JLabel("Conexiones");
        title.setHorizontalAlignment(SwingConstants.CENTER);
        add(title, BorderLayout.NORTH);

        JTextArea ta = new JTextArea("Aquí irá la lista de conexiones activas...");
        ta.setEditable(false);
        add(new JScrollPane(ta), BorderLayout.CENTER);
    }
}

