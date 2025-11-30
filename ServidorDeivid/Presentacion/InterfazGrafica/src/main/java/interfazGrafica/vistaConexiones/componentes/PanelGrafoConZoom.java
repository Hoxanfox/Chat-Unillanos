package interfazGrafica.vistaConexiones.componentes;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Panel con capacidades de zoom y scroll para visualizar grafos grandes
 */
public class PanelGrafoConZoom extends JPanel {

    private JPanel grafoPanel;
    private JScrollPane scrollPane;
    private double escalaZoom = 1.0;
    private static final double ZOOM_MIN = 0.5;
    private static final double ZOOM_MAX = 2.0;
    private static final double ZOOM_STEP = 0.1;

    private JButton btnZoomIn;
    private JButton btnZoomOut;
    private JButton btnResetZoom;
    private JLabel lblZoom;

    public PanelGrafoConZoom(JPanel grafoPanel) {
        this.grafoPanel = grafoPanel;
        configurarPanel();
        inicializarComponentes();
        configurarEventos();
    }

    private void configurarPanel() {
        this.setLayout(new BorderLayout(5, 5));
    }

    private void inicializarComponentes() {
        // Panel de controles de zoom
        JPanel panelControles = crearPanelControles();
        this.add(panelControles, BorderLayout.NORTH);

        // Wrapper para el grafo con tamaño preferido grande
        JPanel wrapperGrafo = new JPanel(new GridBagLayout()) {
            @Override
            public Dimension getPreferredSize() {
                Dimension original = grafoPanel.getPreferredSize();
                return new Dimension(
                    (int)(original.width * escalaZoom),
                    (int)(original.height * escalaZoom)
                );
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;

                // Aplicar escala
                g2d.scale(escalaZoom, escalaZoom);

                // Centrar el grafo en el panel
                int offsetX = (int)((getWidth() / escalaZoom - grafoPanel.getWidth()) / 2);
                int offsetY = (int)((getHeight() / escalaZoom - grafoPanel.getHeight()) / 2);
                g2d.translate(Math.max(0, offsetX), Math.max(0, offsetY));
            }
        };
        wrapperGrafo.setBackground(Color.WHITE);

        // Agregar el grafo al wrapper
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        wrapperGrafo.add(grafoPanel, gbc);

        // Scroll pane para el grafo
        scrollPane = new JScrollPane(wrapperGrafo);
        scrollPane.setWheelScrollingEnabled(true);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);

        this.add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel crearPanelControles() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 2));
        panel.setBackground(new Color(236, 240, 241));

        JLabel lblControl = new JLabel("Zoom:");
        lblControl.setFont(new Font("Arial", Font.PLAIN, 10));

        btnZoomOut = crearBotonZoom("-", "Alejar");
        btnResetZoom = crearBotonZoom("100%", "Restablecer zoom");
        btnZoomIn = crearBotonZoom("+", "Acercar");

        lblZoom = new JLabel("100%");
        lblZoom.setFont(new Font("Arial", Font.BOLD, 10));
        lblZoom.setPreferredSize(new Dimension(45, 20));

        panel.add(lblControl);
        panel.add(btnZoomOut);
        panel.add(lblZoom);
        panel.add(btnZoomIn);
        panel.add(btnResetZoom);

        return panel;
    }

    private JButton crearBotonZoom(String texto, String tooltip) {
        JButton boton = new JButton(texto);
        boton.setPreferredSize(new Dimension(45, 22));
        boton.setFont(new Font("Arial", Font.PLAIN, 10));
        boton.setFocusPainted(false);
        boton.setToolTipText(tooltip);
        return boton;
    }

    private void configurarEventos() {
        // Zoom in
        btnZoomIn.addActionListener(e -> aplicarZoom(escalaZoom + ZOOM_STEP));

        // Zoom out
        btnZoomOut.addActionListener(e -> aplicarZoom(escalaZoom - ZOOM_STEP));

        // Reset zoom
        btnResetZoom.addActionListener(e -> aplicarZoom(1.0));

        // Zoom con rueda del mouse (Ctrl + Scroll)
        scrollPane.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (e.isControlDown()) {
                    e.consume();
                    if (e.getWheelRotation() < 0) {
                        aplicarZoom(escalaZoom + ZOOM_STEP);
                    } else {
                        aplicarZoom(escalaZoom - ZOOM_STEP);
                    }
                }
            }
        });
    }

    private void aplicarZoom(double nuevoZoom) {
        // Limitar el zoom
        nuevoZoom = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, nuevoZoom));

        if (nuevoZoom != escalaZoom) {
            escalaZoom = nuevoZoom;

            // Actualizar label
            lblZoom.setText(String.format("%.0f%%", escalaZoom * 100));

            // Actualizar el panel
            scrollPane.getViewport().getView().revalidate();
            scrollPane.getViewport().getView().repaint();
        }
    }

    public void resetZoom() {
        aplicarZoom(1.0);
    }

    public JPanel getGrafoPanel() {
        return grafoPanel;
    }
}

