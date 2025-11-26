package interfazGrafica;

import controlador.p2p.ControladorP2P;
import controlador.clienteServidor.ControladorClienteServidor;
import controlador.usuarios.ControladorUsuarios;
import gestorUsuarios.GestorUsuarios;
import gestorP2P.servicios.ServicioSincronizacionDatos;
import servicio.usuario.ServicioGestionUsuarios;
import interfazGrafica.vistaUsuarios.PanelUsuarios;
import interfazGrafica.vistaConexiones.PanelConexiones;
import interfazGrafica.vistaLogs.PanelLogs;
import interfazGrafica.vistaPrincipal.PanelPrincipal;
import logger.LoggerCentral;
import observador.IObservador;

import javax.swing.*;
import java.awt.*;

/**
 * Ventana principal - SOLO conoce controladores
 * ✅ Construye toda la arquitectura de capas internamente
 */
public class VentanaPrincipal extends JFrame implements IObservador {

    private static final String TAG = "VentanaPrincipal";

    private JTabbedPane tabbedPane;
    private PanelPrincipal panelPrincipal;
    private PanelUsuarios panelUsuarios;
    private PanelConexiones panelConexiones;
    private PanelLogs panelLogs;

    private ControladorP2P controladorP2P;
    private ControladorClienteServidor controladorCS;
    private ControladorUsuarios controladorUsuarios;

    // Servicios para integración P2P
    private ServicioSincronizacionDatos servicioSincronizacion;

    private volatile boolean p2pIniciado = false;
    private volatile boolean csIniciado = false;

    public VentanaPrincipal() {
        configurarVentana();
        inicializarControladores();
        inicializarComponentes();
        this.setVisible(true);
        iniciarServiciosEnSecuencia();
    }

    private void configurarVentana() {
        this.setTitle("CHAT SERVER ADMINISTRATION");
        this.setSize(900, 600);
        this.setMinimumSize(new Dimension(800, 500));
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setLayout(new BorderLayout());
    }

    private void inicializarControladores() {
        LoggerCentral.info(TAG, "Inicializando controladores...");

        // Controladores P2P y Cliente-Servidor
        controladorP2P = new ControladorP2P();
        controladorCS = new ControladorClienteServidor();

        // Construir arquitectura de capas para Usuarios
        construirArquitecturaUsuarios();

        LoggerCentral.info(TAG, "✓ Todos los controladores inicializados");
    }

    /**
     * Construye toda la arquitectura de capas para la gestión de usuarios:
     * Controlador → Servicio → Gestor → Repositorio
     * + Integración con sincronización P2P
     */
    private void construirArquitecturaUsuarios() {
        LoggerCentral.info(TAG, "🔧 Construyendo arquitectura de gestión de usuarios...");

        // 1. Capa de Negocio: GestorUsuarios
        GestorUsuarios gestorUsuarios = new GestorUsuarios();

        // 2. Capa de Servicio: ServicioGestionUsuarios
        ServicioGestionUsuarios servicioUsuarios = new ServicioGestionUsuarios(gestorUsuarios);

        // 3. Obtener el servicio de sincronización P2P del ServicioP2P (NO crear uno nuevo)
        // Este servicio YA está conectado a la red y tiene el gestor de conexiones configurado
        servicio.p2p.ServicioP2P servicioP2PInterno = controladorP2P.getServicioP2PInterno();
        if (servicioP2PInterno != null) {
            servicioSincronizacion = servicioP2PInterno.getServicioSincronizacion();

            if (servicioSincronizacion != null) {
                // 4. Conectar servicio de usuarios con sincronización P2P
                servicioUsuarios.setServicioSincronizacion(servicioSincronizacion);
                LoggerCentral.info(TAG, "✓ ServicioGestionUsuarios conectado con ServicioSincronizacionDatos existente");

                // 5. Registrar GestorUsuarios como observador para cambios desde otros peers
                gestorUsuarios.registrarObservador(servicioSincronizacion);
                LoggerCentral.info(TAG, "✓ GestorUsuarios registrado como observador del ServicioSincronizacionDatos");
            } else {
                LoggerCentral.warn(TAG, "⚠️ ServicioSincronizacionDatos no disponible en ServicioP2P");
            }
        } else {
            LoggerCentral.warn(TAG, "⚠️ No se pudo obtener ServicioP2P interno");
        }

        // 6. Capa de Presentación: ControladorUsuarios
        controladorUsuarios = new ControladorUsuarios(servicioUsuarios);

        LoggerCentral.info(TAG, "✓ Arquitectura de usuarios construida:");
        LoggerCentral.info(TAG, "  Interfaz → ControladorUsuarios → ServicioGestionUsuarios → GestorUsuarios → Repositorio");
        LoggerCentral.info(TAG, "  Con sincronización P2P integrada (usando el ServicioSincronizacionDatos de la red P2P)");
    }

    private void inicializarComponentes() {
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Arial", Font.PLAIN, 12));

        panelPrincipal = new PanelPrincipal();
        panelUsuarios = new PanelUsuarios(controladorUsuarios);
        panelConexiones = new PanelConexiones(controladorP2P, controladorCS);
        panelLogs = new PanelLogs();

        tabbedPane.addTab("Dashboard", panelPrincipal);
        tabbedPane.addTab("Users", panelUsuarios);
        tabbedPane.addTab("Channels", crearPanelTemporal("CHANNELS"));
        tabbedPane.addTab("Connections", panelConexiones);
        tabbedPane.addTab("Logs", panelLogs);

        this.add(tabbedPane, BorderLayout.CENTER);
    }


    private void iniciarServiciosEnSecuencia() {
        LoggerCentral.info(TAG, "═══════════════════════════════════════════");
        LoggerCentral.info(TAG, "    INICIANDO SERVICIOS EN SECUENCIA");
        LoggerCentral.info(TAG, "═══════════════════════════════════════════");

        SwingUtilities.invokeLater(() -> panelPrincipal.agregarEstado("⚡ Iniciando red P2P..."));
        controladorP2P.suscribirseAEventosConexion();

        new Thread(() -> {
            try {
                LoggerCentral.info(TAG, "🚀 PASO 1: Iniciando red P2P...");
                controladorP2P.iniciarRed();
                Thread.sleep(2000);

                if (controladorP2P.isRedIniciada()) {
                    p2pIniciado = true;
                    LoggerCentral.info(TAG, "✓ Red P2P iniciada correctamente");

                    SwingUtilities.invokeLater(() -> {
                        panelPrincipal.agregarEstado("✓ Red P2P iniciada correctamente");
                        panelPrincipal.agregarEstado("⚡ Iniciando servidor Cliente-Servidor...");
                    });

                    iniciarClienteServidor();
                } else {
                    LoggerCentral.error(TAG, "✗ Error: P2P no se inició correctamente");
                    SwingUtilities.invokeLater(() ->
                        panelPrincipal.agregarEstado("✗ ERROR: P2P no se inició correctamente")
                    );
                }
            } catch (Exception e) {
                LoggerCentral.error(TAG, "Error iniciando P2P: " + e.getMessage());
                SwingUtilities.invokeLater(() ->
                    panelPrincipal.agregarEstado("✗ ERROR iniciando P2P: " + e.getMessage())
                );
            }
        }, "Thread-InicioP2P").start();
    }

    private void iniciarClienteServidor() {
        LoggerCentral.info(TAG, "═══════════════════════════════════════════");
        LoggerCentral.info(TAG, "🚀 PASO 2: Iniciando Cliente-Servidor...");
        LoggerCentral.info(TAG, "═══════════════════════════════════════════");

        new Thread(() -> {
            try {
                controladorCS.iniciarServidorAutomatico();
                Thread.sleep(1000);

                if (controladorCS.isServidorActivo()) {
                    csIniciado = true;
                    LoggerCentral.info(TAG, "✓ Servidor Cliente-Servidor iniciado correctamente");
                    conectarServiciosParaTopologia();

                    SwingUtilities.invokeLater(() -> {
                        panelPrincipal.agregarEstado("✓ Servidor Cliente-Servidor iniciado correctamente");
                        panelPrincipal.agregarEstado("✓ Servicios integrados P2P ↔ Cliente-Servidor");
                        panelPrincipal.agregarEstado("");
                        panelPrincipal.agregarEstado("═══════════════════════════════════════════");
                        panelPrincipal.agregarEstado("✅ SISTEMA COMPLETAMENTE OPERATIVO");
                        panelPrincipal.agregarEstado("═══════════════════════════════════════════");
                        actualizarEstadisticasDashboard();
                    });
                } else {
                    LoggerCentral.error(TAG, "✗ Error: Cliente-Servidor no se inició correctamente");
                    SwingUtilities.invokeLater(() ->
                        panelPrincipal.agregarEstado("✗ ERROR: Cliente-Servidor no se inició")
                    );
                }
            } catch (Exception e) {
                LoggerCentral.error(TAG, "Error iniciando Cliente-Servidor: " + e.getMessage());
                SwingUtilities.invokeLater(() ->
                    panelPrincipal.agregarEstado("✗ ERROR iniciando CS: " + e.getMessage())
                );
            }
        }, "Thread-InicioCS").start();
    }

    private void conectarServiciosParaTopologia() {
        try {
            LoggerCentral.info(TAG, "🔗 Conectando servicios P2P ↔ Cliente-Servidor...");

            servicio.p2p.ServicioP2P servicioP2P = controladorP2P.getServicioP2PInterno();
            servicio.clienteServidor.IServicioClienteControl servicioCS = controladorCS.getServicioClienteInterno();

            if (servicioP2P != null && servicioCS != null) {
                servicioP2P.setServicioCliente(servicioCS);
                LoggerCentral.info(TAG, "✓ Servicios conectados para sincronización de topología");
                controladorP2P.forzarActualizacionTopologia();
                LoggerCentral.info(TAG, "✓ Topología actualizada con información de clientes");
            } else {
                LoggerCentral.warn(TAG, "No se pudieron conectar los servicios para topología");
            }
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error conectando servicios: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void actualizarEstadisticasDashboard() {
        try {
            int peersActivos = (int) controladorP2P.obtenerListaPeers().stream()
                    .filter(p -> "ONLINE".equalsIgnoreCase(p.getEstado()))
                    .count();

            int totalClientes = controladorCS.getNumeroClientesConectados();
            int clientesAutenticados = (int) controladorCS.getSesionesActivas().stream()
                    .filter(dto.cliente.DTOSesionCliente::estaAutenticado)
                    .count();

            int conexionesTotales = peersActivos + totalClientes;
            int conexionesActivas = peersActivos + clientesAutenticados;

            panelPrincipal.actualizarEstadisticas(
                    totalClientes, clientesAutenticados,
                    0, 0,
                    conexionesTotales, conexionesActivas
            );
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error actualizando estadísticas: " + e.getMessage());
        }
    }

    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        LoggerCentral.debug(TAG, "Evento recibido: " + tipoDeDato);

        switch (tipoDeDato) {
            case "RED_INICIADA":
                SwingUtilities.invokeLater(() -> {
                    if (!p2pIniciado) {
                        p2pIniciado = true;
                        panelPrincipal.agregarEstado("✓ Red P2P lista: " + datos);
                        panelPrincipal.agregarEstado("⚡ Iniciando Cliente-Servidor...");
                        iniciarClienteServidor();
                    } else if (!csIniciado) {
                        csIniciado = true;
                        panelPrincipal.agregarEstado("✓ Cliente-Servidor listo: " + datos);
                    }
                });
                break;

            case "PEER_CONECTADO":
            case "CLIENTE_CONECTADO":
            case "PEER_OFFLINE":
            case "CLIENTE_DESCONECTADO":
                SwingUtilities.invokeLater(this::actualizarEstadisticasDashboard);
                break;

            default:
                LoggerCentral.debug(TAG, "Evento no manejado: " + tipoDeDato);
        }
    }

    private JPanel crearPanelTemporal(String texto) {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel(texto, SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 24));
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }
}
