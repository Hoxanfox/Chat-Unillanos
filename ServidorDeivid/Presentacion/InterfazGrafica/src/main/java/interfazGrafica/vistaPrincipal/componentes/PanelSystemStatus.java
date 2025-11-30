package interfazGrafica.vistaPrincipal.componentes;

import controlador.dashboard.ControladorDashboard;
import logger.LoggerCentral;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Panel que muestra el estado del sistema dividido en secciones
 * Incluye: Server Info, Network Status, Resources y Activity
 * ✅ ACTUALIZADO: Ahora se integra con ControladorDashboard para actualizaciones automáticas
 */
public class PanelSystemStatus extends JPanel {

    private static final String TAG = "PanelSystemStatus";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private JTextArea txtServerInfo;
    private JTextArea txtNetworkStatus;
    private JTextArea txtResources;
    private JTextArea txtRecentActivity;

    // ✅ NUEVO: Controlador de Dashboard
    private ControladorDashboard controladorDashboard;

    public PanelSystemStatus() {
        configurarPanel();
        inicializarComponentes();
        inicializarControlador();
    }

    private void configurarPanel() {
        this.setLayout(new BorderLayout(10, 10));
        this.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
    }

    private void inicializarComponentes() {
        // Título principal
        JLabel lblTitulo = new JLabel("SYSTEM STATUS");
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 14));
        lblTitulo.setForeground(new Color(0, 102, 204));
        lblTitulo.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        // Panel principal con grid para las 4 secciones
        JPanel panelSecciones = new JPanel(new GridLayout(2, 2, 15, 15));
        panelSecciones.setBackground(Color.WHITE);

        // Crear las 4 secciones
        JPanel seccionServerInfo = crearSeccion("Server Information");
        JPanel seccionNetwork = crearSeccion("Network Status");
        JPanel seccionResources = crearSeccion("System Resources");
        JPanel seccionActivity = crearSeccion("Recent Activity");

        // Agregar áreas de texto a cada sección
        txtServerInfo = crearAreaTexto();
        txtNetworkStatus = crearAreaTexto();
        txtResources = crearAreaTexto();
        txtRecentActivity = crearAreaTexto();

        seccionServerInfo.add(new JScrollPane(txtServerInfo), BorderLayout.CENTER);
        seccionNetwork.add(new JScrollPane(txtNetworkStatus), BorderLayout.CENTER);
        seccionResources.add(new JScrollPane(txtResources), BorderLayout.CENTER);
        seccionActivity.add(new JScrollPane(txtRecentActivity), BorderLayout.CENTER);

        // Agregar secciones al panel
        panelSecciones.add(seccionServerInfo);
        panelSecciones.add(seccionNetwork);
        panelSecciones.add(seccionResources);
        panelSecciones.add(seccionActivity);

        // Agregar componentes
        this.add(lblTitulo, BorderLayout.NORTH);
        this.add(panelSecciones, BorderLayout.CENTER);
    }

    /**
     * ✅ NUEVO: Inicializa el controlador de dashboard y suscribe a eventos
     */
    private void inicializarControlador() {
        try {
            controladorDashboard = new ControladorDashboard();

            // Suscribir el controlador a eventos del servicio
            controladorDashboard.suscribirseAEventos();

            // Suscribir callbacks para actualizar la vista
            controladorDashboard.suscribirInfoServidor(this::actualizarServerInfoEnHilo);
            controladorDashboard.suscribirEstadoRed(this::actualizarNetworkStatusEnHilo);
            controladorDashboard.suscribirRecursos(this::actualizarResourcesEnHilo);
            controladorDashboard.suscribirActividades(this::agregarActividadEnHilo);

            // Iniciar el monitoreo
            controladorDashboard.iniciarMonitoreo();

            LoggerCentral.info(TAG, "✅ PanelSystemStatus conectado al ControladorDashboard");

            // Cargar datos iniciales
            actualizarTodo();

        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error inicializando controlador de dashboard: " + e.getMessage());
        }
    }

    /**
     * ✅ NUEVO: Actualiza toda la información del dashboard
     */
    private void actualizarTodo() {
        if (controladorDashboard == null || !controladorDashboard.estaActivo()) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            actualizarServerInfo(controladorDashboard.obtenerInfoServidor());
            actualizarNetworkStatus(controladorDashboard.obtenerEstadoRed());
            actualizarResources(controladorDashboard.obtenerRecursos());
        });
    }

    /**
     * ✅ NUEVO: Wrappers para actualizar en el hilo de Swing (thread-safe)
     */
    private void actualizarServerInfoEnHilo(String info) {
        SwingUtilities.invokeLater(() -> actualizarServerInfo(info));
    }

    private void actualizarNetworkStatusEnHilo(String status) {
        SwingUtilities.invokeLater(() -> actualizarNetworkStatus(status));
    }

    private void actualizarResourcesEnHilo(String resources) {
        SwingUtilities.invokeLater(() -> actualizarResources(resources));
    }

    private void agregarActividadEnHilo(String actividad) {
        SwingUtilities.invokeLater(() -> agregarActividad(actividad));
    }

    /**
     * Crea un panel de sección con título
     */
    private JPanel crearSeccion(String titulo) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        TitledBorder border = BorderFactory.createTitledBorder(
                new LineBorder(new Color(200, 200, 200), 1),
                titulo,
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 11),
                new Color(100, 100, 100)
        );
        panel.setBorder(border);

        return panel;
    }

    /**
     * Crea un área de texto configurada
     */
    private JTextArea crearAreaTexto() {
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        textArea.setBackground(new Color(252, 252, 252));
        textArea.setForeground(new Color(60, 60, 60));
        textArea.setMargin(new Insets(8, 8, 8, 8));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        return textArea;
    }

    /**
     * Actualiza la información del servidor
     */
    public void actualizarServerInfo(String info) {
        txtServerInfo.setText(info);
        txtServerInfo.setCaretPosition(0);
    }

    /**
     * Actualiza el estado de la red
     */
    public void actualizarNetworkStatus(String status) {
        txtNetworkStatus.setText(status);
        txtNetworkStatus.setCaretPosition(0);
    }

    /**
     * Actualiza los recursos del sistema
     */
    public void actualizarResources(String resources) {
        txtResources.setText(resources);
        txtResources.setCaretPosition(0);
    }

    /**
     * Agrega una actividad reciente
     */
    public void agregarActividad(String actividad) {
        txtRecentActivity.append(actividad + "\n");
        // Auto-scroll al final
        txtRecentActivity.setCaretPosition(txtRecentActivity.getDocument().getLength());
    }

    /**
     * Establece las actividades recientes completas
     */
    public void actualizarActividades(String actividades) {
        txtRecentActivity.setText(actividades);
        txtRecentActivity.setCaretPosition(0);
    }

    /**
     * Limpia toda la información del panel
     */
    public void limpiarTodo() {
        txtServerInfo.setText("");
        txtNetworkStatus.setText("");
        txtResources.setText("");
        txtRecentActivity.setText("");
    }

    /**
     * ✅ NUEVO: Obtiene el controlador de dashboard para uso externo
     */
    public ControladorDashboard getControladorDashboard() {
        return controladorDashboard;
    }

    /**
     * ✅ NUEVO: Limpieza al destruir el panel
     */
    public void destruir() {
        if (controladorDashboard != null) {
            controladorDashboard.detenerMonitoreo();
            LoggerCentral.info(TAG, "PanelSystemStatus destruido y monitoreo detenido");
        }
    }
}
