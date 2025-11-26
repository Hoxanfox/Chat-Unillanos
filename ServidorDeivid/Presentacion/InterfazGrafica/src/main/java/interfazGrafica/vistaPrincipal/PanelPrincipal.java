package interfazGrafica.vistaPrincipal;

import interfazGrafica.vistaPrincipal.componentes.PanelOverview;
import interfazGrafica.vistaPrincipal.componentes.PanelSystemStatus;

import javax.swing.*;
import java.awt.*;

/**
 * Panel principal del dashboard de administración
 * Muestra una vista general del sistema con estadísticas y estado
 */
public class PanelPrincipal extends JPanel {

    private PanelOverview panelOverview;
    private PanelSystemStatus panelSystemStatus;

    public PanelPrincipal() {
        configurarPanel();
        inicializarComponentes();
        cargarDatosEjemplo();
    }

    private void configurarPanel() {
        this.setLayout(new BorderLayout(10, 10));
        this.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
    }

    private void inicializarComponentes() {
        // Panel superior con overview (3 cards)
        panelOverview = new PanelOverview();

        // Panel inferior con system status
        panelSystemStatus = new PanelSystemStatus();

        // Agregar componentes
        this.add(panelOverview, BorderLayout.NORTH);
        this.add(panelSystemStatus, BorderLayout.CENTER);
    }

    /**
     * Actualiza las estadísticas del overview
     */
    public void actualizarEstadisticas(int totalUsuarios, int usuariosActivos,
                                       int totalCanales, int canalesActivos,
                                       int totalConexiones, int conexionesActivas) {
        panelOverview.actualizarEstadisticas(totalUsuarios, usuariosActivos,
                totalCanales, canalesActivos,
                totalConexiones, conexionesActivas);
    }

    /**
     * Agrega una línea de información al system status
     */
    public void agregarEstado(String mensaje) {
        panelSystemStatus.agregarEstado(mensaje);
    }

    /**
     * Limpia toda la información del system status
     */
    public void limpiarEstado() {
        panelSystemStatus.limpiarEstado();
    }

    /**
     * Carga datos de ejemplo para demostración
     */
    private void cargarDatosEjemplo() {
        // Actualizar estadísticas de ejemplo
        actualizarEstadisticas(
                45, 12,  // Usuarios: 45 total, 12 activos
                8, 5,    // Canales: 8 total, 5 activos
                23, 18   // Conexiones: 23 total, 18 activas
        );

        // Información del servidor
        String serverInfo =
            "Status: Running\n" +
            "Port: 8080\n" +
            "Started: 2025-11-20 08:15:32\n" +
            "Uptime: 5 days, 3 hours, 45 minutes\n" +
            "Version: 1.0.0\n" +
            "Environment: Production\n" +
            "Last Restart: 2025-11-20 08:15:30\n" +
            "Configuration: Loaded";
        panelSystemStatus.actualizarServerInfo(serverInfo);

        // Estado de la red
        String networkStatus =
            "Client-Server Network:\n" +
            "  • Active Connections: 18\n" +
            "  • Pending: 2\n" +
            "  • Total Handled: 1,247\n\n" +
            "P2P Network:\n" +
            "  • Connected Peers: 3\n" +
            "  • Synchronization: Active\n" +
            "  • Latency: 45ms avg\n" +
            "  • Traffic: Normal";
        panelSystemStatus.actualizarNetworkStatus(networkStatus);

        // Recursos del sistema
        String resources =
            "CPU Usage: 15%\n" +
            "  • User: 8%\n" +
            "  • System: 7%\n\n" +
            "Memory:\n" +
            "  • Used: 512 MB / 2048 MB\n" +
            "  • Usage: 25%\n\n" +
            "Database:\n" +
            "  • Connection Pool: 8/20\n" +
            "  • Active Queries: 3\n" +
            "  • Status: Connected\n\n" +
            "Storage:\n" +
            "  • Used: 15.3 GB / 100 GB";
        panelSystemStatus.actualizarResources(resources);

        // Actividad reciente
        String actividades =
            "[11:23:45] User 'john_doe' logged in\n" +
            "[11:22:18] New channel 'project-alpha' created\n" +
            "[11:21:05] Peer 'peer_003' connected\n" +
            "[11:20:32] Backup completed successfully\n" +
            "[11:19:50] User 'jane_smith' logged out\n" +
            "[11:18:15] Channel 'general' updated\n" +
            "[11:17:42] Warning: High traffic detected\n" +
            "[11:16:20] Configuration reloaded\n" +
            "[11:15:08] Database optimization completed\n" +
            "[11:14:35] Security scan: No threats found";
        panelSystemStatus.actualizarActividades(actividades);
    }
}
