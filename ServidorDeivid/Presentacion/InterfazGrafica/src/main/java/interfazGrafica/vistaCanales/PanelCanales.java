package interfazGrafica.vistaCanales;

import controlador.canales.ControladorCanales;
import dto.vista.DTOCanalVista;
import interfazGrafica.vistaCanales.componentes.TablaCanales;
import logger.LoggerCentral;
import observador.IObservador;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Panel principal que muestra la lista de canales del sistema
 * Implementa IObservador para actualizarse automáticamente con cambios
 */
public class PanelCanales extends JPanel implements IObservador {

    private static final String TAG = "PanelCanales";
    
    private TablaCanales tablaCanales;
    private ControladorCanales controlador;
    private JLabel lblTotalCanales;
    private JButton btnRefrescar;
    private JButton btnVerMiembros;

    public PanelCanales(ControladorCanales controlador) {
        this.controlador = controlador;
        configurarPanel();
        inicializarComponentes();
        configurarEventos();
        cargarCanalesDesdeBaseDatos();
    }

    private void configurarPanel() {
        this.setLayout(new BorderLayout(10, 10));
        this.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
    }

    private void inicializarComponentes() {
        // Panel superior con título y estadísticas
        JPanel panelSuperior = crearPanelSuperior();
        
        // Tabla de canales
        tablaCanales = new TablaCanales();

        // Panel inferior con información
        JPanel panelInferior = crearPanelInferior();

        // Agregar componentes
        this.add(panelSuperior, BorderLayout.NORTH);
        this.add(tablaCanales, BorderLayout.CENTER);
        this.add(panelInferior, BorderLayout.SOUTH);
    }

    private JPanel crearPanelSuperior() {
        JPanel panel = new JPanel(new BorderLayout(10, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        // Título
        JLabel lblTitulo = new JLabel("📢 Gestión de Canales");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        panel.add(lblTitulo, BorderLayout.WEST);

        // Panel de botones
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        
        btnVerMiembros = new JButton("👥 Ver Miembros");
        btnVerMiembros.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnVerMiembros.setEnabled(false);
        
        btnRefrescar = new JButton("🔄 Refrescar");
        btnRefrescar.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        panelBotones.add(btnVerMiembros);
        panelBotones.add(btnRefrescar);
        
        panel.add(panelBotones, BorderLayout.EAST);

        return panel;
    }

    private JPanel crearPanelInferior() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
            BorderFactory.createEmptyBorder(10, 0, 0, 0)
        ));

        lblTotalCanales = new JLabel("Total de canales: 0");
        lblTotalCanales.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblTotalCanales.setForeground(Color.DARK_GRAY);
        
        panel.add(lblTotalCanales);

        return panel;
    }

    private void configurarEventos() {
        // Botón refrescar
        btnRefrescar.addActionListener(e -> {
            LoggerCentral.info(TAG, "🔄 Refrescando lista de canales...");
            cargarCanalesDesdeBaseDatos();
        });

        // Botón ver miembros
        btnVerMiembros.addActionListener(e -> mostrarMiembrosCanal());

        // Habilitar/deshabilitar botón ver miembros según selección
        tablaCanales.getTabla().getSelectionModel().addListSelectionListener(e -> {
            btnVerMiembros.setEnabled(tablaCanales.hayFilaSeleccionada());
        });
    }

    private void mostrarMiembrosCanal() {
        if (!tablaCanales.hayFilaSeleccionada()) {
            JOptionPane.showMessageDialog(this,
                "Por favor seleccione un canal",
                "Ver Miembros",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        Object[] canal = tablaCanales.obtenerCanalSeleccionado();
        String canalId = (String) canal[0];
        String nombreCanal = (String) canal[1];

        // Obtener miembros del canal
        List<String> miembros = controlador.obtenerMiembrosCanal(canalId);

        // Mostrar diálogo con lista de miembros
        StringBuilder sb = new StringBuilder();
        sb.append("Miembros del canal '").append(nombreCanal).append("':\n\n");
        
        if (miembros.isEmpty()) {
            sb.append("(Sin miembros)");
        } else {
            for (int i = 0; i < miembros.size(); i++) {
                sb.append(i + 1).append(". ").append(miembros.get(i)).append("\n");
            }
        }

        JTextArea textArea = new JTextArea(sb.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(300, 200));

        JOptionPane.showMessageDialog(this,
            scrollPane,
            "👥 Miembros del Canal",
            JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Carga los canales desde la base de datos
     */
    private void cargarCanalesDesdeBaseDatos() {
        try {
            // Limpiar tabla
            tablaCanales.limpiarTabla();

            // Obtener canales desde el controlador
            List<DTOCanalVista> canales = controlador.listarCanales();

            // Agregar cada canal a la tabla
            for (DTOCanalVista c : canales) {
                Object[] fila = {
                    c.getId(),
                    c.getNombre(),
                    c.getTipo(),
                    c.getCreadorNombre(),
                    c.getNumeroMiembros(),
                    c.getFechaCreacion()
                };
                tablaCanales.agregarCanal(fila);
            }

            // Actualizar contador
            lblTotalCanales.setText("Total de canales: " + canales.size());
            LoggerCentral.info(TAG, "✅ Cargados " + canales.size() + " canales");

        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al cargar canales: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                "Error al cargar canales: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Refresca la tabla con los datos actuales
     */
    public void refrescarTabla() {
        cargarCanalesDesdeBaseDatos();
    }

    @Override
    public void actualizar(String tipo, Object datos) {
        LoggerCentral.debug(TAG, "📢 Evento recibido: " + tipo);

        switch (tipo) {
            case "NUEVO_CANAL":
            case "CANAL_CREADO":
            case "CANAL_ACTUALIZADO":
            case "CANAL_ELIMINADO":
            case "SINCRONIZACION_TERMINADA":
            case "SINCRONIZACION_P2P_TERMINADA":
            case "SINCRONIZADO_CANAL":
                SwingUtilities.invokeLater(() -> {
                    LoggerCentral.info(TAG, "🔄 Refrescando tabla por evento: " + tipo);
                    refrescarTabla();
                });
                break;

            default:
                LoggerCentral.debug(TAG, "Evento no manejado en PanelCanales: " + tipo);
        }
    }

    // Getters
    public TablaCanales getTablaCanales() {
        return tablaCanales;
    }
}

