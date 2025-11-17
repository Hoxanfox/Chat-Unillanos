package interfazGrafica.features.channels;

import javax.swing.*;
import java.awt.*;

/**
 * Panel para gestionar canales (vista mínima).
 */
public class ChannelsPanel extends JPanel {

    public ChannelsPanel() {
        init();
    }

    private void init() {
        setLayout(new BorderLayout(8,8));
        JLabel title = new JLabel("Channels");
        title.setHorizontalAlignment(SwingConstants.CENTER);
        add(title, BorderLayout.NORTH);

        JTextArea ta = new JTextArea("Aquí irá la lista de canales...");
        ta.setEditable(false);
        add(new JScrollPane(ta), BorderLayout.CENTER);
    }
}


