package interfazGrafica.vistaConexiones.componentes;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

/**
 * Componente que muestra la leyenda de colores para los grafos
 * Diferencia claramente entre Peers (cuadrados) y Usuarios (círculos)
 */
public class LeyendaGrafos extends JPanel {

    public LeyendaGrafos() {
        configurarPanel();
        inicializarComponentes();
    }

    private void configurarPanel() {
        this.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 5));
        this.setBackground(new Color(236, 240, 241));
        this.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(189, 195, 199), 1),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
    }

    private void inicializarComponentes() {
        // Leyenda para Peers (cuadrados)
        JLabel lblPeers = new JLabel("PEERS (■):");
        lblPeers.setFont(new Font("Arial", Font.BOLD, 11));
        this.add(lblPeers);

        this.add(crearIndicadorCuadrado(new Color(52, 152, 219), "Local Peer"));
        this.add(crearIndicadorCuadrado(new Color(46, 204, 113), "Online Peer"));
        this.add(crearIndicadorCuadrado(new Color(149, 165, 166), "Offline Peer"));

        // Separador
        JSeparator sep1 = new JSeparator(SwingConstants.VERTICAL);
        sep1.setPreferredSize(new Dimension(2, 20));
        this.add(sep1);

        // Leyenda para Usuarios (círculos)
        JLabel lblUsers = new JLabel("USERS (●):");
        lblUsers.setFont(new Font("Arial", Font.BOLD, 11));
        this.add(lblUsers);

        this.add(crearIndicadorCirculo(new Color(26, 188, 156), "User Online"));
        this.add(crearIndicadorCirculo(new Color(189, 195, 199), "User Offline"));
    }

    private JPanel crearIndicadorCuadrado(Color color, String texto) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        panel.setOpaque(false);

        // Panel personalizado para dibujar un cuadrado
        JPanel colorBox = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Rectangle2D rect = new Rectangle2D.Double(2, 2, 11, 11);
                g2d.setColor(color);
                g2d.fill(rect);
                g2d.setColor(color.darker());
                g2d.setStroke(new BasicStroke(1.5f));
                g2d.draw(rect);
            }
        };
        colorBox.setPreferredSize(new Dimension(15, 15));
        colorBox.setOpaque(false);

        // Etiqueta
        JLabel label = new JLabel(texto);
        label.setFont(new Font("Arial", Font.PLAIN, 10));

        panel.add(colorBox);
        panel.add(label);

        return panel;
    }

    private JPanel crearIndicadorCirculo(Color color, String texto) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        panel.setOpaque(false);

        // Panel personalizado para dibujar un círculo
        JPanel colorBox = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Ellipse2D circle = new Ellipse2D.Double(2, 2, 11, 11);
                g2d.setColor(color);
                g2d.fill(circle);
                g2d.setColor(color.darker());
                g2d.setStroke(new BasicStroke(1.5f));
                g2d.draw(circle);
            }
        };
        colorBox.setPreferredSize(new Dimension(15, 15));
        colorBox.setOpaque(false);

        // Etiqueta
        JLabel label = new JLabel(texto);
        label.setFont(new Font("Arial", Font.PLAIN, 10));

        panel.add(colorBox);
        panel.add(label);

        return panel;
    }
}
