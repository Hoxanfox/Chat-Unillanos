package interfazGrafica.vistaUsuarios.componentes;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Componente que representa la barra de herramientas superior
 * con el título y botones de acción (Add, Delete, Edit)
 */
public class BarraHerramientasUsuarios extends JPanel {

    private JButton btnAdd;
    private JButton btnDelete;
    private JButton btnEdit;
    private JLabel lblTitulo;

    public BarraHerramientasUsuarios() {
        configurarPanel();
        inicializarComponentes();
    }

    private void configurarPanel() {
        this.setLayout(new BorderLayout(10, 10));
    }

    private void inicializarComponentes() {
        // Título
        lblTitulo = new JLabel("USER MANAGEMENT");
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 14));
        lblTitulo.setForeground(new Color(0, 102, 204));

        // Panel de botones
        JPanel panelBotones = crearPanelBotones();

        this.add(lblTitulo, BorderLayout.WEST);
        this.add(panelBotones, BorderLayout.EAST);
    }

    private JPanel crearPanelBotones() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));

        btnAdd = crearBoton("Add");
        btnDelete = crearBoton("Delete");
        btnEdit = crearBoton("Edit");

        panel.add(btnAdd);
        panel.add(btnDelete);
        panel.add(btnEdit);

        return panel;
    }

    private JButton crearBoton(String texto) {
        JButton boton = new JButton(texto);
        boton.setPreferredSize(new Dimension(80, 30));
        boton.setFocusPainted(false);
        boton.setFont(new Font("Arial", Font.PLAIN, 12));
        return boton;
    }

    // Métodos para agregar listeners desde el componente padre
    public void setAddActionListener(ActionListener listener) {
        btnAdd.addActionListener(listener);
    }

    public void setDeleteActionListener(ActionListener listener) {
        btnDelete.addActionListener(listener);
    }

    public void setEditActionListener(ActionListener listener) {
        btnEdit.addActionListener(listener);
    }

    // Getters
    public JButton getBtnAdd() {
        return btnAdd;
    }

    public JButton getBtnDelete() {
        return btnDelete;
    }

    public JButton getBtnEdit() {
        return btnEdit;
    }

    public void setTitulo(String titulo) {
        lblTitulo.setText(titulo);
    }
}