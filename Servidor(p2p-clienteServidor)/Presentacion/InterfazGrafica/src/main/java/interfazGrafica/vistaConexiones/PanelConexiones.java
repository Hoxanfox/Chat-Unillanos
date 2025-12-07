package interfazGrafica.vistaConexiones;

import controlador.p2p.ControladorP2P;
import controlador.clienteServidor.ControladorClienteServidor;
import interfazGrafica.vistaConexiones.componentes.*;
import logger.LoggerCentral; // ✅ NUEVO
import observador.IObservador; // ✅ NUEVO

import javax.swing.*;
import java.awt.*;

/**
 * Panel principal de la vista de conexiones
 * Permite cambiar entre tres modos de visualización:
 * - Solo red P2P
 * - Solo red Cliente-Servidor
 * - Red completa (ambas integradas)
 * ✅ ACTUALIZADO: Ahora recibe controladores para grafos dinámicos
 * ✅ NUEVO: Implementa IObservador para escuchar eventos de topología P2P
 */
public class PanelConexiones extends JPanel implements IObservador { // ✅ NUEVO

    private static final String TAG = "PanelConexiones"; // ✅ NUEVO

    private GrafoP2P grafoP2P;
    private GrafoClienteServidor grafoCS;
    private GrafoRedCompleta grafoCompleto;
    private LeyendaGrafos leyenda;

    // ✅ NUEVO: Controladores
    private ControladorP2P controladorP2P;
    private ControladorClienteServidor controladorCS;

    // Wrappers con zoom
    private PanelGrafoConZoom wrapperP2P;
    private PanelGrafoConZoom wrapperCS;
    private PanelGrafoConZoom wrapperCompleto;

    // Componentes de UI
    private JPanel panelCentral;
    private CardLayout cardLayout;
    private JButton btnSoloP2P;
    private JButton btnSoloCS;
    private JButton btnRedCompleta;

    // Constantes para las vistas
    private static final String VISTA_P2P = "P2P";
    private static final String VISTA_CS = "CS";
    private static final String VISTA_COMPLETA = "COMPLETA";
    private static final String VISTA_DUAL = "DUAL";

    public PanelConexiones() {
        configurarPanel();
        inicializarComponentes();
        cargarDatosEjemplo();
        mostrarVistaDual(); // Vista por defecto
    }

    /**
     * ✅ NUEVO: Constructor con controladores para grafos dinámicos
     */
    public PanelConexiones(ControladorP2P controladorP2P, ControladorClienteServidor controladorCS) {
        this.controladorP2P = controladorP2P;
        this.controladorCS = controladorCS;

        configurarPanel();
        inicializarComponentes();
        // No cargar datos de ejemplo cuando hay controladores reales
        mostrarVistaDual();
    }

    private void configurarPanel() {
        this.setLayout(new BorderLayout(10, 10));
        this.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
    }

    private void inicializarComponentes() {
        // Panel superior con título y botones de control
        JPanel panelSuperior = crearPanelSuperior();
        this.add(panelSuperior, BorderLayout.NORTH);

        // Panel central con CardLayout para cambiar entre vistas
        cardLayout = new CardLayout();
        panelCentral = new JPanel(cardLayout);

        // Crear las diferentes vistas
        panelCentral.add(crearVistaDual(), VISTA_DUAL);
        panelCentral.add(crearVistaP2P(), VISTA_P2P);
        panelCentral.add(crearVistaCS(), VISTA_CS);
        panelCentral.add(crearVistaCompleta(), VISTA_COMPLETA);

        this.add(panelCentral, BorderLayout.CENTER);

        // Panel inferior con leyenda
        leyenda = new LeyendaGrafos();
        this.add(leyenda, BorderLayout.SOUTH);
    }

    private JPanel crearPanelSuperior() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        // Título
        JLabel lblTitulo = new JLabel("ACTIVE CONNECTIONS");
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 14));
        lblTitulo.setForeground(new Color(0, 102, 204));

        // Panel de botones de control
        JPanel panelBotones = crearPanelBotonesControl();

        panel.add(lblTitulo, BorderLayout.WEST);
        panel.add(panelBotones, BorderLayout.EAST);

        return panel;
    }

    private JPanel crearPanelBotonesControl() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));

        btnSoloP2P = crearBotonControl("P2P Network", VISTA_P2P);
        btnSoloCS = crearBotonControl("Client-Server", VISTA_CS);
        btnRedCompleta = crearBotonControl("Full Network", VISTA_COMPLETA);

        panel.add(new JLabel("View:"));
        panel.add(btnSoloP2P);
        panel.add(btnSoloCS);
        panel.add(btnRedCompleta);

        return panel;
    }

    private JButton crearBotonControl(String texto, String vista) {
        JButton boton = new JButton(texto);
        boton.setPreferredSize(new Dimension(120, 30));
        boton.setFocusPainted(false);
        boton.setFont(new Font("Arial", Font.PLAIN, 11));

        boton.addActionListener(e -> {
            cardLayout.show(panelCentral, vista);
            actualizarEstadoBotones(boton);
        });

        return boton;
    }

    private void actualizarEstadoBotones(JButton botonActivo) {
        // Resetear todos los botones
        btnSoloP2P.setBackground(null);
        btnSoloCS.setBackground(null);
        btnRedCompleta.setBackground(null);

        // Resaltar el botón activo
        botonActivo.setBackground(new Color(52, 152, 219));
        botonActivo.setForeground(Color.WHITE);
    }

    private JPanel crearVistaDual() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 15, 0));

        // Panel P2P con zoom (✅ ahora con controlador si está disponible)
        JPanel panelP2P = new JPanel(new BorderLayout(5, 5));
        panelP2P.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(189, 195, 199), 1),
            "P2P Network",
            javax.swing.border.TitledBorder.CENTER,
            javax.swing.border.TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 12),
            new Color(52, 152, 219)
        ));

        grafoP2P = (controladorP2P != null) ? new GrafoP2P(controladorP2P) : new GrafoP2P();
        wrapperP2P = new PanelGrafoConZoom(grafoP2P);
        panelP2P.add(wrapperP2P, BorderLayout.CENTER);

        // Panel Cliente-Servidor con zoom (✅ ahora con controlador si está disponible)
        JPanel panelCS = new JPanel(new BorderLayout(5, 5));
        panelCS.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(189, 195, 199), 1),
            "Client-Server Network",
            javax.swing.border.TitledBorder.CENTER,
            javax.swing.border.TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 12),
            new Color(52, 152, 219)
        ));

        grafoCS = (controladorCS != null) ? new GrafoClienteServidor(controladorCS) : new GrafoClienteServidor();
        wrapperCS = new PanelGrafoConZoom(grafoCS);
        panelCS.add(wrapperCS, BorderLayout.CENTER);

        panel.add(panelP2P);
        panel.add(panelCS);

        return panel;
    }

    private JPanel crearVistaP2P() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(189, 195, 199), 1),
            "P2P Network - Full View",
            javax.swing.border.TitledBorder.CENTER,
            javax.swing.border.TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 14),
            new Color(52, 152, 219)
        ));

        // Usar el wrapper con zoom
        panel.add(wrapperP2P, BorderLayout.CENTER);

        return panel;
    }

    private JPanel crearVistaCS() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(189, 195, 199), 1),
            "Client-Server Network - Full View",
            javax.swing.border.TitledBorder.CENTER,
            javax.swing.border.TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 14),
            new Color(52, 152, 219)
        ));

        // Usar el wrapper con zoom
        panel.add(wrapperCS, BorderLayout.CENTER);

        return panel;
    }

    private JPanel crearVistaCompleta() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(189, 195, 199), 1),
            "Complete Network - Integrated View",
            javax.swing.border.TitledBorder.CENTER,
            javax.swing.border.TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 14),
            new Color(52, 152, 219)
        ));

        // ✅ CORREGIDO: Pasar el controlador para que se suscriba a eventos de topología
        if (controladorP2P != null) {
            grafoCompleto = new GrafoRedCompleta(controladorP2P);
            LoggerCentral.info(TAG, "✅ GrafoRedCompleta creado con controlador (se suscribirá a topología)");
        } else {
            grafoCompleto = new GrafoRedCompleta();
            LoggerCentral.warn(TAG, "⚠️ GrafoRedCompleta creado sin controlador (modo estático)");
        }

        wrapperCompleto = new PanelGrafoConZoom(grafoCompleto);
        panel.add(wrapperCompleto, BorderLayout.CENTER);

        return panel;
    }

    private void mostrarVistaDual() {
        cardLayout.show(panelCentral, VISTA_DUAL);
    }

    private void cargarDatosEjemplo() {
        // Datos para P2P
        grafoP2P.agregarPeer("peer-local", "192.168.1.100", true, true);
        grafoP2P.agregarPeer("peer-001", "192.168.1.101", false, true);
        grafoP2P.agregarPeer("peer-002", "192.168.1.102", false, true);
        grafoP2P.agregarPeer("peer-003", "192.168.1.103", false, false);

        grafoP2P.agregarConexion("peer-local", "peer-001");
        grafoP2P.agregarConexion("peer-local", "peer-002");
        grafoP2P.agregarConexion("peer-001", "peer-003");

        // Datos para Cliente-Servidor
        grafoCS.agregarPeer("peer-local", "192.168.1.100");
        grafoCS.agregarPeer("peer-001", "192.168.1.101");
        grafoCS.agregarPeer("peer-002", "192.168.1.102");

        grafoCS.agregarUsuario("admin", "peer-local", true);
        grafoCS.agregarUsuario("john.doe", "peer-001", true);
        grafoCS.agregarUsuario("jane.smith", "peer-001", true);
        grafoCS.agregarUsuario("bob.wilson", "peer-002", false);
        grafoCS.agregarUsuario("alice.brown", "peer-002", true);

        // Datos para vista completa
        grafoCompleto.agregarPeer("peer-local", "192.168.1.100", true, true);
        grafoCompleto.agregarPeer("peer-001", "192.168.1.101", false, true);
        grafoCompleto.agregarPeer("peer-002", "192.168.1.102", false, true);
        grafoCompleto.agregarPeer("peer-003", "192.168.1.103", false, false);

        grafoCompleto.agregarConexionP2P("peer-local", "peer-001");
        grafoCompleto.agregarConexionP2P("peer-local", "peer-002");
        grafoCompleto.agregarConexionP2P("peer-001", "peer-003");

        grafoCompleto.agregarUsuario("admin", "peer-local", true);
        grafoCompleto.agregarUsuario("john.doe", "peer-001", true);
        grafoCompleto.agregarUsuario("jane.smith", "peer-001", true);
        grafoCompleto.agregarUsuario("bob.wilson", "peer-002", false);
        grafoCompleto.agregarUsuario("alice.brown", "peer-002", true);
    }

    // Métodos públicos para actualizar los grafos
    public void actualizarGrafos() {
        // ✅ CORREGIDO: Solo usar datos de ejemplo si NO hay controladores reales
        if (controladorP2P == null && controladorCS == null) {
            // Modo estático con datos de ejemplo
            grafoP2P.limpiar();
            grafoCS.limpiar();
            grafoCompleto.limpiar();
            cargarDatosEjemplo();
            LoggerCentral.debug(TAG, "Grafos actualizados con datos de ejemplo (sin controladores)");
        } else {
            // Modo dinámico: Los grafos se actualizan solos mediante observadores
            // NO hacer nada aquí, dejar que cada grafo se actualice desde su controlador
            LoggerCentral.debug(TAG, "Los grafos se actualizan automáticamente desde controladores (modo dinámico)");
        }
    }

    public GrafoP2P getGrafoP2P() {
        return grafoP2P;
    }

    public GrafoClienteServidor getGrafoCS() {
        return grafoCS;
    }

    public GrafoRedCompleta getGrafoCompleto() {
        return grafoCompleto;
    }

    // ✅ NUEVO: Implementación de IObservador para eventos de topología
    @Override
    public void actualizar(String tipo, Object datos) {
        LoggerCentral.debug(TAG, "📢 Evento recibido: " + tipo + " | Datos: " + datos);

        switch (tipo) {
            case "TOPOLOGIA_ACTUALIZADA":
            case "TOPOLOGIA_REMOTA_RECIBIDA":
                // Cuando cambia la topología P2P, actualizar los grafos
                SwingUtilities.invokeLater(() -> {
                    LoggerCentral.info(TAG, "🔄 Actualizando grafos por cambio en topología P2P");
                    actualizarGrafos();
                });
                break;

            case "PEER_CONECTADO":
            case "PEER_DESCONECTADO":
                // Cuando se conecta/desconecta un peer, actualizar grafos
                SwingUtilities.invokeLater(() -> {
                    LoggerCentral.info(TAG, "🔄 Actualizando grafos por cambio de conexión P2P: " + tipo);
                    actualizarGrafos();
                });
                break;

            default:
                LoggerCentral.debug(TAG, "Evento no manejado en PanelConexiones: " + tipo);
        }
    }
}
