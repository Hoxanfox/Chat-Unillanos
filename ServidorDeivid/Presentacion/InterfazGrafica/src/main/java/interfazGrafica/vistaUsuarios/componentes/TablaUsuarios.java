package interfazGrafica.vistaUsuarios.componentes;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Componente que encapsula la tabla de usuarios
 * con su modelo y configuración
 */
public class TablaUsuarios extends JPanel {

    private JTable tabla;
    private DefaultTableModel modelo;
    private JScrollPane scrollPane;

    private static final String[] COLUMNAS = {
        "ID", "Username", "Email", "Status", "Last Active", "Peer ID"
    };

    public TablaUsuarios() {
        configurarPanel();
        inicializarTabla();
    }

    private void configurarPanel() {
        this.setLayout(new BorderLayout());
    }

    private void inicializarTabla() {
        // Crear modelo no editable
        modelo = new DefaultTableModel(COLUMNAS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // Crear tabla
        tabla = new JTable(modelo);
        configurarAparienciaTabla();

        // Agregar a scroll pane
        scrollPane = new JScrollPane(tabla);
        this.add(scrollPane, BorderLayout.CENTER);
    }

    private void configurarAparienciaTabla() {
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setRowHeight(25);
        tabla.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        tabla.setFont(new Font("Arial", Font.PLAIN, 11));

        // Ajustar anchos de columnas
        tabla.getColumnModel().getColumn(0).setPreferredWidth(50);  // ID
        tabla.getColumnModel().getColumn(1).setPreferredWidth(150); // Username
        tabla.getColumnModel().getColumn(2).setPreferredWidth(200); // Email
        tabla.getColumnModel().getColumn(3).setPreferredWidth(80);  // Status
        tabla.getColumnModel().getColumn(4).setPreferredWidth(120); // Last Active
        tabla.getColumnModel().getColumn(5).setPreferredWidth(100); // Peer ID
    }

    // Métodos públicos para manipular la tabla
    public void agregarUsuario(Object[] datos) {
        modelo.addRow(datos);
    }

    public void eliminarUsuarioSeleccionado() {
        int filaSeleccionada = tabla.getSelectedRow();
        if (filaSeleccionada != -1) {
            modelo.removeRow(filaSeleccionada);
        }
    }

    public void actualizarUsuario(int fila, Object[] datos) {
        if (fila >= 0 && fila < modelo.getRowCount()) {
            for (int i = 0; i < datos.length && i < modelo.getColumnCount(); i++) {
                modelo.setValueAt(datos[i], fila, i);
            }
        }
    }

    public void limpiarTabla() {
        modelo.setRowCount(0);
    }

    public Object[] obtenerUsuarioSeleccionado() {
        int filaSeleccionada = tabla.getSelectedRow();
        if (filaSeleccionada == -1) {
            return null;
        }

        Object[] datos = new Object[modelo.getColumnCount()];
        for (int i = 0; i < modelo.getColumnCount(); i++) {
            datos[i] = modelo.getValueAt(filaSeleccionada, i);
        }
        return datos;
    }

    public int getFilaSeleccionada() {
        return tabla.getSelectedRow();
    }

    public boolean hayFilaSeleccionada() {
        return tabla.getSelectedRow() != -1;
    }

    public int getCantidadFilas() {
        return modelo.getRowCount();
    }

    // Getters
    public JTable getTabla() {
        return tabla;
    }

    public DefaultTableModel getModelo() {
        return modelo;
    }
}
