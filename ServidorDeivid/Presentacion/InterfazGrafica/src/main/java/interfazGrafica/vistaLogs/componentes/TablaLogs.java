package interfazGrafica.vistaLogs.componentes;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;

/**
 * Componente de tabla para mostrar los logs del sistema
 * Columnas: Time, Level, Source, Message
 */
public class TablaLogs extends JPanel {

    private JTable tabla;
    private DefaultTableModel modeloTabla;
    private JScrollPane scrollPane;

    // Nombres de las columnas
    private static final String[] COLUMNAS = {"Time", "Level", "Source", "Message"};

    // Anchos preferidos de las columnas
    private static final int[] ANCHOS_COLUMNAS = {150, 80, 150, 400};

    public TablaLogs() {
        configurarPanel();
        inicializarTabla();
    }

    private void configurarPanel() {
        this.setLayout(new BorderLayout());
    }

    private void inicializarTabla() {
        // Crear modelo de tabla no editable
        modeloTabla = new DefaultTableModel(COLUMNAS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // Crear tabla
        tabla = new JTable(modeloTabla);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setRowHeight(25);
        tabla.setShowGrid(true);
        tabla.setGridColor(new Color(230, 230, 230));
        tabla.getTableHeader().setReorderingAllowed(false);

        // Configurar anchos de columnas
        for (int i = 0; i < ANCHOS_COLUMNAS.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(ANCHOS_COLUMNAS[i]);
        }

        // Estilizar el encabezado
        JTableHeader header = tabla.getTableHeader();
        header.setBackground(new Color(240, 240, 240));
        header.setFont(new Font("Arial", Font.BOLD, 12));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 35));

        // Renderer personalizado para la columna Level
        tabla.getColumnModel().getColumn(1).setCellRenderer(new LevelCellRenderer());

        // Renderer para alinear texto
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        tabla.getColumnModel().getColumn(1).setCellRenderer(new LevelCellRenderer());

        // Agregar scroll
        scrollPane = new JScrollPane(tabla);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

        this.add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Agrega un nuevo log a la tabla
     */
    public void agregarLog(Object[] log) {
        modeloTabla.addRow(log);
        // Auto-scroll al último registro
        int ultimaFila = tabla.getRowCount() - 1;
        if (ultimaFila >= 0) {
            tabla.scrollRectToVisible(tabla.getCellRect(ultimaFila, 0, true));
        }
    }

    /**
     * Limpia todos los logs de la tabla
     */
    public void limpiarLogs() {
        modeloTabla.setRowCount(0);
    }

    /**
     * Obtiene todos los logs como matriz de objetos
     */
    public Object[][] obtenerTodosLosLogs() {
        int filas = modeloTabla.getRowCount();
        int columnas = modeloTabla.getColumnCount();
        Object[][] datos = new Object[filas][columnas];

        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < columnas; j++) {
                datos[i][j] = modeloTabla.getValueAt(i, j);
            }
        }
        return datos;
    }

    /**
     * Obtiene el número de logs en la tabla
     */
    public int obtenerCantidadLogs() {
        return modeloTabla.getRowCount();
    }

    /**
     * Renderer personalizado para la columna Level
     * Colorea el texto según el nivel: INFO, WARNING, ERROR
     */
    private class LevelCellRenderer extends DefaultTableCellRenderer {
        public LevelCellRenderer() {
            setHorizontalAlignment(JLabel.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (!isSelected && value != null) {
                String level = value.toString().toUpperCase();
                switch (level) {
                    case "INFO":
                        c.setForeground(new Color(0, 150, 0)); // Verde
                        break;
                    case "WARNING":
                        c.setForeground(new Color(255, 140, 0)); // Naranja
                        break;
                    case "ERROR":
                        c.setForeground(new Color(200, 0, 0)); // Rojo
                        break;
                    default:
                        c.setForeground(Color.BLACK);
                }
            } else if (isSelected) {
                c.setForeground(table.getSelectionForeground());
            }

            return c;
        }
    }
}


