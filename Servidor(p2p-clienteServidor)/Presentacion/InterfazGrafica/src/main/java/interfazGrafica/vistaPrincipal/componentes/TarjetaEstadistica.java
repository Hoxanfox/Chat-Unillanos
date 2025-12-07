package interfazGrafica.vistaPrincipal.componentes;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * Tarjeta que muestra estadísticas de un aspecto del sistema
 * Muestra título, total y cantidad activa
 */
public class TarjetaEstadistica extends JPanel {

    private String titulo;
    private Color colorTema;
    private JLabel lblTotal;
    private JLabel lblActivos;

    public TarjetaEstadistica(String titulo, Color colorTema) {
        this.titulo = titulo;
        this.colorTema = colorTema;
        configurarPanel();
        inicializarComponentes();
    }

    private void configurarPanel() {
        this.setLayout(new BorderLayout(10, 10));
        this.setBackground(Color.WHITE);
        this.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(230, 230, 230), 1, true),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
    }

    private void inicializarComponentes() {
        // Panel superior con título
        JPanel panelTitulo = new JPanel(new BorderLayout());
        panelTitulo.setBackground(Color.WHITE);

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 12));
        lblTitulo.setForeground(new Color(100, 100, 100));

        // Indicador de color
        JPanel indicadorColor = new JPanel();
        indicadorColor.setBackground(colorTema);
        indicadorColor.setPreferredSize(new Dimension(4, 20));

        panelTitulo.add(indicadorColor, BorderLayout.WEST);
        panelTitulo.add(lblTitulo, BorderLayout.CENTER);

        // Panel central con números
        JPanel panelNumeros = new JPanel();
        panelNumeros.setLayout(new BoxLayout(panelNumeros, BoxLayout.Y_AXIS));
        panelNumeros.setBackground(Color.WHITE);
        panelNumeros.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        // Total
        lblTotal = new JLabel("0");
        lblTotal.setFont(new Font("Arial", Font.BOLD, 36));
        lblTotal.setForeground(colorTema);
        lblTotal.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Activos
        lblActivos = new JLabel("0 active");
        lblActivos.setFont(new Font("Arial", Font.PLAIN, 14));
        lblActivos.setForeground(new Color(150, 150, 150));
        lblActivos.setAlignmentX(Component.LEFT_ALIGNMENT);

        panelNumeros.add(lblTotal);
        panelNumeros.add(Box.createRigidArea(new Dimension(0, 5)));
        panelNumeros.add(lblActivos);

        // Agregar componentes
        this.add(panelTitulo, BorderLayout.NORTH);
        this.add(panelNumeros, BorderLayout.CENTER);
    }

    /**
     * Actualiza los datos mostrados en la tarjeta
     */
    public void actualizarDatos(int total, int activos) {
        lblTotal.setText(String.valueOf(total));
        lblActivos.setText(activos + " active");
    }
}

