package interfazGrafica.vistaTranscripcion.componentes;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Panel de filtros y búsqueda para transcripciones
 */
public class PanelFiltros extends JPanel {

    private static final Color COLOR_PRIMARY = new Color(52, 152, 219);
    private static final Color COLOR_WARNING = new Color(241, 196, 15);

    private JTextField campoBusqueda;
    private JComboBox<String> comboFiltroTipo;
    private ActionListener listenerBuscar;
    private ActionListener listenerLimpiar;

    public PanelFiltros() {
        inicializarUI();
    }

    private void inicializarUI() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(189, 195, 199), 1),
                new EmptyBorder(10, 10, 10, 10)
        ));

        // Título
        JLabel lblTitulo = new JLabel("🎤 Transcripción de Audios");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitulo.setForeground(COLOR_PRIMARY);
        add(lblTitulo, BorderLayout.WEST);

        // Panel de filtros y búsqueda
        JPanel panelFiltros = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        panelFiltros.setBackground(Color.WHITE);

        // Filtro por tipo
        panelFiltros.add(new JLabel("Tipo:"));
        comboFiltroTipo = new JComboBox<>(new String[]{"Todos", "Canales", "Contactos"});
        panelFiltros.add(comboFiltroTipo);

        // Búsqueda
        panelFiltros.add(new JLabel("Buscar:"));
        campoBusqueda = new JTextField(20);
        panelFiltros.add(campoBusqueda);

        JButton btnBuscar = new JButton("🔍");
        estilizarBoton(btnBuscar, COLOR_PRIMARY);
        panelFiltros.add(btnBuscar);

        JButton btnLimpiar = new JButton("✖");
        estilizarBoton(btnLimpiar, COLOR_WARNING);
        panelFiltros.add(btnLimpiar);

        add(panelFiltros, BorderLayout.EAST);

        // Configurar listeners
        btnBuscar.addActionListener(e -> {
            if (listenerBuscar != null) {
                listenerBuscar.actionPerformed(e);
            }
        });

        btnLimpiar.addActionListener(e -> {
            if (listenerLimpiar != null) {
                listenerLimpiar.actionPerformed(e);
            }
        });

        campoBusqueda.addActionListener(e -> {
            if (listenerBuscar != null) {
                listenerBuscar.actionPerformed(e);
            }
        });

        comboFiltroTipo.addActionListener(e -> {
            if (listenerBuscar != null) {
                listenerBuscar.actionPerformed(e);
            }
        });
    }

    private void estilizarBoton(JButton boton, Color color) {
        boton.setBackground(color);
        boton.setForeground(Color.WHITE);
        boton.setFocusPainted(false);
        boton.setBorderPainted(false);
        boton.setFont(new Font("Segoe UI", Font.BOLD, 11));
        boton.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    // Getters
    public String getTextoBusqueda() {
        return campoBusqueda.getText().trim();
    }

    public String getTipoSeleccionado() {
        return (String) comboFiltroTipo.getSelectedItem();
    }

    // Setters para listeners
    public void setListenerBuscar(ActionListener listener) {
        this.listenerBuscar = listener;
    }

    public void setListenerLimpiar(ActionListener listener) {
        this.listenerLimpiar = listener;
    }

    // Métodos públicos
    public void limpiar() {
        campoBusqueda.setText("");
        comboFiltroTipo.setSelectedIndex(0);
    }
}

