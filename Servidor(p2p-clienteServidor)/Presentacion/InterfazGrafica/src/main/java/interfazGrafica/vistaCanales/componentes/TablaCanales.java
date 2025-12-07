package interfazGrafica.vistaCanales.componentes;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * Componente que encapsula la tabla de canales
 * con su modelo y configuración
 */
public class TablaCanales extends JPanel {

    private JTable tabla;
    private DefaultTableModel modelo;
    private JScrollPane scrollPane;

    private static final String[] COLUMNAS = {
        "ID", "Nombre", "Tipo", "Creador", "Miembros", "Fecha Creación"
    };

    public TablaCanales() {
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
        tabla.setRowHeight(28);
        tabla.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabla.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        // Colores alternados para filas
        tabla.setFillsViewportHeight(true);

        // Ajustar anchos de columnas
        tabla.getColumnModel().getColumn(0).setPreferredWidth(80);   // ID
        tabla.getColumnModel().getColumn(1).setPreferredWidth(200);  // Nombre
        tabla.getColumnModel().getColumn(2).setPreferredWidth(100);  // Tipo
        tabla.getColumnModel().getColumn(3).setPreferredWidth(150);  // Creador
        tabla.getColumnModel().getColumn(4).setPreferredWidth(80);   // Miembros
        tabla.getColumnModel().getColumn(5).setPreferredWidth(150);  // Fecha

        // Centrar columna de miembros
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        tabla.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);

        // Renderizador para tipo con colores
        tabla.getColumnModel().getColumn(2).setCellRenderer(new TipoCanelRenderer());
    }

    /**
     * Renderizador personalizado para la columna de tipo
     */
    private static class TipoCanelRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            Component c = super.getTableCellRendererComponent(table, value, 
                    isSelected, hasFocus, row, column);
            
            setHorizontalAlignment(JLabel.CENTER);
            
            if (!isSelected && value != null) {
                String tipo = value.toString();
                if ("PUBLICO".equals(tipo)) {
                    setBackground(new Color(200, 230, 200)); // Verde claro
                    setForeground(new Color(0, 100, 0));
                } else if ("PRIVADO".equals(tipo)) {
                    setBackground(new Color(255, 220, 200)); // Naranja claro
                    setForeground(new Color(150, 80, 0));
                } else {
                    setBackground(table.getBackground());
                    setForeground(table.getForeground());
                }
            }
            
            return c;
        }
    }

    // Métodos públicos para manipular la tabla
    public void agregarCanal(Object[] datos) {
        modelo.addRow(datos);
    }

    public void limpiarTabla() {
        modelo.setRowCount(0);
    }

    public Object[] obtenerCanalSeleccionado() {
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

