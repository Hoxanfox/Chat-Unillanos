package interfazGrafica.features.logs;

import javax.swing.*;
import java.awt.*;

/**
 * Panel para mostrar logs del sistema. Inicialmente muestra un área de texto.
 */
public class LogsPanel extends JPanel {

    private final JTextArea textArea = new JTextArea();

    public LogsPanel() {
        init();
    }

    private void init() {
        setLayout(new BorderLayout(8,8));
        JLabel title = new JLabel("System Logs");
        title.setHorizontalAlignment(SwingConstants.CENTER);
        add(title, BorderLayout.NORTH);

        textArea.setEditable(false);
        textArea.setText("Logs del sistema aparecerán aquí...");
        add(new JScrollPane(textArea), BorderLayout.CENTER);
    }

    public void appendLog(String line) {
        if (line == null) return;
        textArea.append(line + "\n");
    }
}

