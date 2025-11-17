package interfazGrafica.features.usuarios;

import javax.swing.*;
import java.awt.*;

/**
 * Panel para la gestión de usuarios (vista mínima).
 */
public class UsuariosPanel extends JPanel {

    public UsuariosPanel() {
        init();
    }

    private void init() {
        setLayout(new BorderLayout(8,8));
        JLabel title = new JLabel("Usuarios");
        title.setHorizontalAlignment(SwingConstants.CENTER);
        add(title, BorderLayout.NORTH);

        // Placeholder: tabla o lista de usuarios
        JTextArea ta = new JTextArea("Aquí irá la lista de usuarios...");
        ta.setEditable(false);
        add(new JScrollPane(ta), BorderLayout.CENTER);
    }
}

