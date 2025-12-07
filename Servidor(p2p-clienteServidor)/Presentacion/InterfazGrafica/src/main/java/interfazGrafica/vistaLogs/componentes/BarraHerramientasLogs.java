package interfazGrafica.vistaLogs.componentes;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Barra de herramientas para el panel de logs
 * Contiene el título y botones de Clear y Export
 */
public class BarraHerramientasLogs extends JPanel {

    private JButton btnClear;
    private JButton btnExport;

    public BarraHerramientasLogs() {
        configurarPanel();
        inicializarComponentes();
    }

    private void configurarPanel() {
        this.setLayout(new BorderLayout(10, 0));
    }

    private void inicializarComponentes() {
        // Título a la izquierda
        JLabel lblTitulo = new JLabel("SYSTEM LOGS");
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 14));
        lblTitulo.setForeground(new Color(0, 102, 204));

        // Panel de botones a la derecha
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));

        btnClear = new JButton("Clear");
        btnClear.setPreferredSize(new Dimension(80, 28));
        btnClear.setFocusPainted(false);

        btnExport = new JButton("Export");
        btnExport.setPreferredSize(new Dimension(80, 28));
        btnExport.setFocusPainted(false);

        panelBotones.add(btnClear);
        panelBotones.add(btnExport);

        // Agregar componentes al panel
        this.add(lblTitulo, BorderLayout.WEST);
        this.add(panelBotones, BorderLayout.EAST);
    }

    // Métodos para configurar listeners desde el panel principal
    public void setClearActionListener(ActionListener listener) {
        btnClear.addActionListener(listener);
    }

    public void setExportActionListener(ActionListener listener) {
        btnExport.addActionListener(listener);
    }
}