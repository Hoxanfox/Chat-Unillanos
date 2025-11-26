package interfazGrafica.vistaPrincipal.componentes;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Panel que muestra el estado del sistema dividido en secciones
 * Incluye: Server Info, Network Status, Resources y Activity
 */
public class PanelSystemStatus extends JPanel {

    private JTextArea txtServerInfo;
    private JTextArea txtNetworkStatus;
    private JTextArea txtResources;
    private JTextArea txtRecentActivity;

    public PanelSystemStatus() {
        configurarPanel();
        inicializarComponentes();
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
     * Agrega una línea de estado (método legacy para compatibilidad)
     */
    public void agregarEstado(String mensaje) {
        // Determinar a qué sección pertenece el mensaje
        if (mensaje.contains("Server") || mensaje.contains("port") || mensaje.contains("Uptime") || mensaje.contains("backup")) {
            txtServerInfo.append(mensaje + "\n");
        } else if (mensaje.contains("Network") || mensaje.contains("P2P") || mensaje.contains("peer") || mensaje.contains("connection")) {
            txtNetworkStatus.append(mensaje + "\n");
        } else if (mensaje.contains("Memory") || mensaje.contains("CPU") || mensaje.contains("Database")) {
            txtResources.append(mensaje + "\n");
        } else {
            txtRecentActivity.append(mensaje + "\n");
        }
    }

    /**
     * Limpia todo el contenido (método legacy para compatibilidad)
     */
    public void limpiarEstado() {
        limpiarTodo();
    }

    /**
     * Establece el contenido completo (método legacy para compatibilidad)
     */
    public void setEstado(String contenido) {
        txtRecentActivity.setText(contenido);
        txtRecentActivity.setCaretPosition(0);
    }

    /**
     * Obtiene el contenido actual (método legacy para compatibilidad)
     */
    public String getEstado() {
        return txtServerInfo.getText() + "\n" +
                txtNetworkStatus.getText() + "\n" +
                txtResources.getText() + "\n" +
                txtRecentActivity.getText();
    }
}
