package interfazGrafica.vistaTranscripcion.componentes;

import dto.transcripcion.DTOAudioTranscripcion;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Componente de tabla para mostrar audios
 */
public class TablaAudios extends JPanel {

    private static final Color COLOR_PRIMARY = new Color(52, 152, 219);

    private JTable tabla;
    private DefaultTableModel modelo;
    private Runnable listenerSeleccion;

    public TablaAudios() {
        inicializarUI();
    }

    private void inicializarUI() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createTitledBorder("Audios disponibles"));

        // Crear tabla
        String[] columnas = {"ID", "Remitente", "Origen", "Fecha", "Duración", "Estado"};
        modelo = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tabla = new JTable(modelo);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && listenerSeleccion != null) {
                listenerSeleccion.run();
            }
        });

        // Estilizar tabla
        tabla.setRowHeight(30);
        tabla.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabla.getTableHeader().setBackground(COLOR_PRIMARY);
        tabla.getTableHeader().setForeground(Color.WHITE);
        tabla.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        // Ocultar columna ID
        tabla.getColumnModel().getColumn(0).setMinWidth(0);
        tabla.getColumnModel().getColumn(0).setMaxWidth(0);

        JScrollPane scrollPane = new JScrollPane(tabla);
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Actualiza la tabla con una lista de audios
     */
    public void actualizarTabla(List<DTOAudioTranscripcion> audios) {
        modelo.setRowCount(0);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (DTOAudioTranscripcion audio : audios) {
            String origen = audio.isEsCanal() ?
                    "📢 " + audio.getNombreCanal() :
                    "👤 " + audio.getNombreContacto();

            String duracion = formatearDuracion(audio.getDuracionSegundos());
            String estado = audio.isTranscrito() ? "✅ Transcrito" : "⏳ Pendiente";

            modelo.addRow(new Object[]{
                    audio.getAudioId(),
                    audio.getNombreRemitente(),
                    origen,
                    audio.getFechaEnvio().format(formatter),
                    duracion,
                    estado
            });
        }
    }

    private String formatearDuracion(long segundos) {
        long minutos = segundos / 60;
        long segs = segundos % 60;
        return String.format("%d:%02d", minutos, segs);
    }

    /**
     * Obtiene el ID del audio seleccionado
     */
    public String getAudioIdSeleccionado() {
        int filaSeleccionada = tabla.getSelectedRow();
        if (filaSeleccionada >= 0) {
            return (String) modelo.getValueAt(filaSeleccionada, 0);
        }
        return null;
    }

    /**
     * Limpia la selección
     */
    public void limpiarSeleccion() {
        tabla.clearSelection();
    }

    /**
     * Establece el listener de selección
     */
    public void setListenerSeleccion(Runnable listener) {
        this.listenerSeleccion = listener;
    }
}

