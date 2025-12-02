package interfazGrafica;

import controlador.p2p.ControladorP2P;
import controlador.clienteServidor.ControladorClienteServidor;
import controlador.usuarios.ControladorUsuarios;
import controlador.canales.ControladorCanales;
import controlador.logs.ControladorLogs;
import controlador.logs.ControladorLogsApi;
import controlador.transcripcion.ControladorTranscripcion;
import gestorUsuarios.GestorUsuarios;
import gestorCanales.GestorCanales;
import gestorLogs.GestorLogs;
import gestorP2P.servicios.ServicioSincronizacionDatos;
import gestorLogs.api.LogsApiConfig;
import servicio.usuario.ServicioGestionUsuarios;
import servicio.canales.ServicioGestionCanales;
import servicio.logs.ServicioLogs;
import interfazGrafica.vistaUsuarios.PanelUsuarios;
import interfazGrafica.vistaCanales.PanelCanales;
import interfazGrafica.vistaConexiones.PanelConexiones;
import interfazGrafica.vistaLogs.PanelLogs;
import interfazGrafica.vistaTranscripcion.PanelTranscripcionAudios;
import interfazGrafica.vistaPrincipal.PanelPrincipal;
import logger.LoggerCentral;
import observador.IObservador;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Ventana principal - SOLO conoce controladores
 * ✅ Construye toda la arquitectura de capas internamente
 */
public class VentanaPrincipal extends JFrame implements IObservador {

    private static final String TAG = "VentanaPrincipal";

    // Indicador visual de sincronización P2P
    private JLabel lblEstadoSyncP2P;

    private JTabbedPane tabbedPane;
    private PanelPrincipal panelPrincipal;
    private PanelUsuarios panelUsuarios;
    private PanelCanales panelCanales;
    private PanelConexiones panelConexiones;
    private PanelLogs panelLogs;
    private PanelTranscripcionAudios panelTranscripcion;

    private ControladorP2P controladorP2P;
    private ControladorClienteServidor controladorCS;
    private ControladorUsuarios controladorUsuarios;
    private ControladorCanales controladorCanales;
    private ControladorLogs controladorLogs;
    private ControladorLogsApi controladorLogsApi;
    private ControladorTranscripcion controladorTranscripcion;

    // Servicios para integración P2P
    private ServicioSincronizacionDatos servicioSincronizacion;

    private volatile boolean p2pIniciado = false;
    private volatile boolean csIniciado = false;
    private volatile boolean apiRestIniciado = false;

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
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setLayout(new BorderLayout());

        // Manejar cierre de ventana para liberar recursos correctamente
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cerrarRecursosYSalir();
            }
        });

        // Barra superior con estado de sincronización
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblEstadoSyncP2P = new JLabel("Sync P2P: IDLE");
        lblEstadoSyncP2P.setOpaque(true);
        lblEstadoSyncP2P.setBackground(Color.GRAY);
        lblEstadoSyncP2P.setForeground(Color.WHITE);
        lblEstadoSyncP2P.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        topBar.add(lblEstadoSyncP2P);

        this.add(topBar, BorderLayout.NORTH);
    }

    /**
     * Cierra todos los recursos (conexiones, hilos, pools) antes de cerrar la aplicación.
     * Esto previene memory leaks y threads huérfanos.
     */
    private void cerrarRecursosYSalir() {
        LoggerCentral.info(TAG, "═══════════════════════════════════════════");
        LoggerCentral.info(TAG, "    CERRANDO RECURSOS DEL SERVIDOR...");
        LoggerCentral.info(TAG, "═══════════════════════════════════════════");

        try {
            // 1. Detener API REST de Logs
            if (controladorLogsApi != null) {
                LoggerCentral.info(TAG, "Deteniendo API REST de Logs...");
                controladorLogsApi.detenerApiRest();
            }

            // 2. Detener servicio de transcripción
            if (controladorTranscripcion != null) {
                LoggerCentral.info(TAG, "Deteniendo servicio de transcripción...");
                controladorTranscripcion.detenerServicio();
            }

            // 3. Detener servidor Cliente-Servidor
            if (controladorCS != null) {
                LoggerCentral.info(TAG, "Deteniendo servidor Cliente-Servidor...");
                controladorCS.detenerServidor();
            }

            // 4. Detener red P2P
            if (controladorP2P != null) {
                LoggerCentral.info(TAG, "Deteniendo red P2P...");
                controladorP2P.detenerRed();
            }

            // 5. Cerrar pool de conexiones MySQL
            LoggerCentral.info(TAG, "Cerrando pool de conexiones MySQL...");
            repositorio.comunicacion.MySQLManager.getInstance().close();

            LoggerCentral.info(TAG, "✓ Todos los recursos cerrados correctamente");

        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error cerrando recursos: " + e.getMessage());
        }

        LoggerCentral.info(TAG, "═══════════════════════════════════════════");
        LoggerCentral.info(TAG, "    SERVIDOR DETENIDO - SALIENDO...");
        LoggerCentral.info(TAG, "═══════════════════════════════════════════");

        // Cerrar la aplicación
        this.dispose();
        System.exit(0);
    }

    private void inicializarControladores() {
        LoggerCentral.info(TAG, "Inicializando controladores...");

        // Controladores P2P y Cliente-Servidor
        controladorP2P = new ControladorP2P();
        controladorCS = new ControladorClienteServidor();

        // Controlador de transcripción (la inicialización del modelo Vosk se hace automáticamente)
        controladorTranscripcion = new ControladorTranscripcion();

        // Construir arquitectura de capas para Usuarios
        construirArquitecturaUsuarios();

        // Construir arquitectura de capas para Canales
        construirArquitecturaCanales();

        // Construir arquitectura de capas para Logs
        construirArquitecturaLogs();

        // Suscribir ventana principal como observador de la sincronización P2P
        if (servicioSincronizacion != null) {
            LoggerCentral.info(TAG, "Registrando VentanaPrincipal como observador de ServicioSincronizacionDatos...");
            servicioSincronizacion.registrarObservador(this);
            LoggerCentral.info(TAG, "✅ VentanaPrincipal suscrita a eventos de ServicioSincronizacionDatos");
        } else {
            LoggerCentral.warn(TAG,
                    "⚠️ servicioSincronizacion es null al inicializar controladores; la ventana no recibirá eventos de sync todavía.");
        }

        LoggerCentral.info(TAG, "✓ Todos los controladores inicializados");
    }

    /**
     * Construye toda la arquitectura de capas para la gestión de logs:
     * Controlador → Servicio → Gestor
     * + API REST con Spring Boot
     */
    private void construirArquitecturaLogs() {
        LoggerCentral.info(TAG, "🔧 Construyendo arquitectura de gestión de logs...");

        // 1. Capa de Negocio: GestorLogs
        GestorLogs gestorLogs = new GestorLogs();

        // 2. Capa de Servicio: ServicioLogs
        ServicioLogs servicioLogs = new ServicioLogs(gestorLogs);

        // 3. Capa de Presentación: ControladorLogs (para la interfaz)
        controladorLogs = new ControladorLogs(servicioLogs);

        // 4. Controlador para el API REST
        controladorLogsApi = new ControladorLogsApi(servicioLogs);

        // ✅ NUEVO: Configurar el proveedor de peers para el API REST
        // Esto permite que el endpoint /api/network/peers devuelva la lista real de
        // peers
        servicio.p2p.ServicioP2P servicioP2P = controladorP2P.getServicioP2PInterno();
        if (servicioP2P != null) {
            LogsApiConfig.setProveedorPeers(() -> servicioP2P.obtenerListaPeers());
            LoggerCentral.info(TAG, "✓ Proveedor de peers configurado para el API REST");
        } else {
            LoggerCentral.warn(TAG, "⚠️ No se pudo configurar proveedor de peers (ServicioP2P es null)");
        }

        LoggerCentral.info(TAG, "✓ Arquitectura de logs construida:");
        LoggerCentral.info(TAG, "  Interfaz → ControladorLogs → ServicioLogs → GestorLogs");
        LoggerCentral.info(TAG, "  API REST → ControladorLogsApi → ServicioLogs → GestorLogs");
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

        // 3. Obtener el servicio de sincronización P2P del ServicioP2P (NO crear uno
        // nuevo)
        // Este servicio YA está conectado a la red y tiene el gestor de conexiones
        // configurado
        servicio.p2p.ServicioP2P servicioP2DInterno = controladorP2P.getServicioP2PInterno();
        if (servicioP2DInterno != null) {
            servicioSincronizacion = servicioP2DInterno.getServicioSincronizacion();

            if (servicioSincronizacion != null) {
                // 4. Conectar servicio de usuarios con sincronización P2P
                servicioUsuarios.setServicioSincronizacion(servicioSincronizacion);
                LoggerCentral.info(TAG,
                        "✓ ServicioGestionUsuarios conectado con ServicioSincronizacionDatos existente");

                // 5. Registrar GestorUsuarios como observador para cambios desde otros peers
                gestorUsuarios.registrarObservador(servicioSincronizacion);
                LoggerCentral.info(TAG, "✓ GestorUsuarios registrado como observador del ServicioSincronizacionDatos");

                // ✅ NUEVO: 6. Configurar el peer local en GestorUsuarios para asignación
                // automática
                try {
                    java.util.UUID peerLocalId = servicioP2DInterno.getIdPeerLocal();
                    if (peerLocalId != null) {
                        gestorUsuarios.setPeerLocalId(peerLocalId);
                        LoggerCentral.info(TAG, "✅ Peer local configurado en GestorUsuarios: " + peerLocalId);
                    } else {
                        LoggerCentral.warn(TAG, "⚠️ No se pudo obtener el ID del peer local");
                    }
                } catch (Exception e) {
                    LoggerCentral.error(TAG, "Error configurando peer local: " + e.getMessage());
                }
            } else {
                LoggerCentral.warn(TAG, "⚠️ ServicioSincronizacionDatos no disponible en ServicioP2P");
            }
        } else {
            LoggerCentral.warn(TAG, "⚠️ No se pudo obtener ServicioP2P interno");
        }

        // 7. Capa de Presentación: ControladorUsuarios
        controladorUsuarios = new ControladorUsuarios(servicioUsuarios);

        LoggerCentral.info(TAG, "✓ Arquitectura de usuarios construida:");
        LoggerCentral.info(TAG,
                "  Interfaz → ControladorUsuarios → ServicioGestionUsuarios → GestorUsuarios → Repositorio");
        LoggerCentral.info(TAG,
                "  Con sincronización P2P integrada (usando el ServicioSincronizacionDatos de la red P2P)");
    }

    /**
     * Construye la arquitectura de capas para la gestión de canales:
     * Controlador → Servicio → Gestor → Repositorio
     */
    private void construirArquitecturaCanales() {
        LoggerCentral.info(TAG, "🔧 Construyendo arquitectura de gestión de canales...");

        // 1. Capa de Negocio: GestorCanales
        GestorCanales gestorCanales = new GestorCanales();

        // 2. Capa de Servicio: ServicioGestionCanales
        ServicioGestionCanales servicioCanales = new ServicioGestionCanales(gestorCanales);

        // 3. Capa de Presentación: ControladorCanales
        controladorCanales = new ControladorCanales(servicioCanales);

        LoggerCentral.info(TAG, "✓ Arquitectura de canales construida:");
        LoggerCentral.info(TAG,
                "  Interfaz → ControladorCanales → ServicioGestionCanales → GestorCanales → Repositorio");
    }

    private void inicializarComponentes() {
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Arial", Font.PLAIN, 12));

        panelPrincipal = new PanelPrincipal();
        panelUsuarios = new PanelUsuarios(controladorUsuarios);
        panelCanales = new PanelCanales(controladorCanales);
        panelConexiones = new PanelConexiones(controladorP2P, controladorCS);
        panelLogs = new PanelLogs();
        panelTranscripcion = new PanelTranscripcionAudios(controladorTranscripcion);

        // Conectar el PanelLogs con su controlador
        panelLogs.setControlador(controladorLogs);

        // ✅ NUEVO: Suscribir PanelUsuarios como observador de eventos de autenticación
        suscribirObservadoresUI();

        tabbedPane.addTab("Dashboard", panelPrincipal);
        tabbedPane.addTab("Users", panelUsuarios);
        tabbedPane.addTab("Channels", panelCanales);
        tabbedPane.addTab("Connections", panelConexiones);
        tabbedPane.addTab("Logs", panelLogs);
        tabbedPane.addTab("Transcription", panelTranscripcion);

        this.add(tabbedPane, BorderLayout.CENTER);
    }

    /**
     * ✅ MEJORADO: Suscribe los paneles de la UI como observadores de los servicios
     */
    private void suscribirObservadoresUI() {
        try {
            // 1. Suscribir PanelUsuarios al ServicioCliente (eventos CS: autenticación,
            // conexión/desconexión)
            servicio.clienteServidor.IServicioClienteControl servicioCS = controladorCS.getServicioClienteInterno();
            if (servicioCS instanceof servicio.clienteServidor.ServicioCliente) {
                servicio.clienteServidor.ServicioCliente servicioClienteImpl = (servicio.clienteServidor.ServicioCliente) servicioCS;
                servicioClienteImpl.registrarObservador(panelUsuarios);
                LoggerCentral.info(TAG, "✓ PanelUsuarios suscrito a eventos de ServicioCliente (CS)");

                // Suscribir GrafoClienteServidor también a los eventos de autenticación
                if (panelConexiones != null && panelConexiones.getGrafoCS() != null) {
                    servicioClienteImpl.registrarObservador(panelConexiones.getGrafoCS());
                    LoggerCentral.info(TAG, "✓ GrafoClienteServidor suscrito a eventos de autenticación");
                }
            }

            // ✅ NUEVO: 2. Suscribir PanelUsuarios al ServicioSincronizacionDatos (eventos
            // P2P: sincronización terminada)
            if (servicioSincronizacion != null) {
                servicioSincronizacion.registrarObservador(panelUsuarios);
                LoggerCentral.info(TAG, "✅ PanelUsuarios suscrito a eventos de ServicioSincronizacionDatos P2P");
                LoggerCentral.info(TAG,
                        "   → El panel se actualizará automáticamente cuando termine la sincronización P2P");

                // ✅ NUEVO: 3. Suscribir PanelCanales al ServicioSincronizacionDatos
                servicioSincronizacion.registrarObservador(panelCanales);
                LoggerCentral.info(TAG, "✅ PanelCanales suscrito a eventos de ServicioSincronizacionDatos P2P");
            } else {
                LoggerCentral.warn(TAG, "⚠️ ServicioSincronizacionDatos no disponible para suscribir paneles");
            }

        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error suscribiendo observadores UI: " + e.getMessage());
        }
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
                    SwingUtilities
                            .invokeLater(() -> panelPrincipal.agregarEstado("✗ ERROR: P2P no se inició correctamente"));
                }
            } catch (Exception e) {
                LoggerCentral.error(TAG, "Error iniciando P2P: " + e.getMessage());
                SwingUtilities
                        .invokeLater(() -> panelPrincipal.agregarEstado("✗ ERROR iniciando P2P: " + e.getMessage()));
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
                        panelPrincipal.agregarEstado("⚡ Iniciando API REST de Logs...");
                    });

                    // Iniciar API REST de Logs
                    iniciarApiRestLogs();
                } else {
                    LoggerCentral.error(TAG, "✗ Error: Cliente-Servidor no se inició correctamente");
                    SwingUtilities
                            .invokeLater(() -> panelPrincipal.agregarEstado("✗ ERROR: Cliente-Servidor no se inició"));
                }
            } catch (Exception e) {
                LoggerCentral.error(TAG, "Error iniciando Cliente-Servidor: " + e.getMessage());
                SwingUtilities
                        .invokeLater(() -> panelPrincipal.agregarEstado("✗ ERROR iniciando CS: " + e.getMessage()));
            }
        }, "Thread-InicioCS").start();
    }

    /**
     * 🆕 PASO 3: Iniciar API REST de Logs
     */
    private void iniciarApiRestLogs() {
        LoggerCentral.info(TAG, "═══════════════════════════════════════════");
        LoggerCentral.info(TAG, "🚀 PASO 3: Iniciando API REST de Logs...");
        LoggerCentral.info(TAG, "═══════════════════════════════════════════");

        new Thread(() -> {
            try {
                // Iniciar en puerto 7000 (configurado en application.properties)
                boolean iniciado = controladorLogsApi.iniciarApiRest(7000);

                if (iniciado) {
                    apiRestIniciado = true;
                    LoggerCentral.info(TAG, "✓ API REST de Logs iniciado correctamente");

                    SwingUtilities.invokeLater(() -> {
                        panelPrincipal.agregarEstado("✓ API REST de Logs iniciado en puerto 7000");
                        panelPrincipal.agregarEstado("  → http://localhost:7000/api/logs");
                        panelPrincipal.agregarEstado("");
                        panelPrincipal.agregarEstado("═══════════════════════════════════════════");
                        panelPrincipal.agregarEstado("✅ SISTEMA COMPLETAMENTE OPERATIVO");
                        panelPrincipal.agregarEstado("═══════════════════════════════════════════");
                        actualizarEstadisticasDashboard();
                    });
                } else {
                    LoggerCentral.warn(TAG, "⚠️ API REST de Logs no se inició (posiblemente puerto ocupado)");
                    SwingUtilities.invokeLater(() -> {
                        panelPrincipal.agregarEstado("⚠️ API REST no iniciado (puerto puede estar ocupado)");
                        panelPrincipal.agregarEstado("");
                        panelPrincipal.agregarEstado("═══════════════════════════════════════════");
                        panelPrincipal.agregarEstado("✅ SISTEMA OPERATIVO (sin API REST)");
                        panelPrincipal.agregarEstado("═══════════════════════════════════════════");
                        actualizarEstadisticasDashboard();
                    });
                }
            } catch (Exception e) {
                LoggerCentral.error(TAG, "Error iniciando API REST: " + e.getMessage());
                SwingUtilities.invokeLater(() -> {
                    panelPrincipal.agregarEstado("✗ ERROR iniciando API REST: " + e.getMessage());
                    panelPrincipal.agregarEstado("");
                    panelPrincipal.agregarEstado("═══════════════════════════════════════════");
                    panelPrincipal.agregarEstado("✅ SISTEMA OPERATIVO (sin API REST)");
                    panelPrincipal.agregarEstado("═══════════════════════════════════════════");
                    actualizarEstadisticasDashboard();
                });
            }
        }, "Thread-InicioApiRest").start();
    }

    private void conectarServiciosParaTopologia() {
        try {
            LoggerCentral.info(TAG, "🔗 Conectando servicios P2P ↔ Cliente-Servidor...");

            servicio.p2p.ServicioP2P servicioP2P = controladorP2P.getServicioP2PInterno();
            servicio.clienteServidor.IServicioClienteControl servicioCS = controladorCS.getServicioClienteInterno();

            if (servicioP2P != null && servicioCS != null) {
                // ✅ 1. Conexión P2P → CS (para topología)
                servicioP2P.setServicioCliente(servicioCS);
                LoggerCentral.info(TAG, "✓ ServicioP2P conectado con ServicioCliente para topología");

                // ✅ 2. Conexión CS → P2P (para sincronización de mensajes/canales)
                ServicioSincronizacionDatos servicioSync = servicioP2P.getServicioSincronizacion();
                if (servicioSync != null) {
                    servicioCS.setServicioSincronizacionP2P(servicioSync);
                    LoggerCentral.info(TAG, "✅ Servicio de sincronización P2P inyectado en servicios CS");

                    // ✅ 3. NUEVO: Conexión P2P → CS (para notificar cuando termina sincronización)
                    gestorClientes.servicios.ServicioNotificacionCliente servicioNotificacionCliente = servicioCS
                            .getServicioNotificacion();
                    if (servicioNotificacionCliente != null) {
                        servicioSync.setServicioNotificacionCliente(servicioNotificacionCliente);
                        LoggerCentral.info(TAG,
                                "✅ ServicioNotificacionCliente inyectado en ServicioSincronizacionDatos");
                    } else {
                        LoggerCentral.warn(TAG, "⚠️ No se pudo obtener ServicioNotificacionCliente");
                    }
                } else {
                    LoggerCentral.warn(TAG, "⚠️ No se pudo obtener ServicioSincronizacionDatos");
                }

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
                    conexionesTotales, conexionesActivas);
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error actualizando estadísticas: " + e.getMessage());
        }
    }

    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        LoggerCentral.debug(TAG, "Evento recibido: " + tipoDeDato + " | datos=" + datos);

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

            case "SINCRONIZACION_P2P_INICIADA":
                LoggerCentral.info(TAG, "🔔 Evento SINCRONIZACION_P2P_INICIADA recibido en VentanaPrincipal");
                SwingUtilities.invokeLater(() -> {
                    if (lblEstadoSyncP2P != null) {
                        lblEstadoSyncP2P.setText("Sync P2P: RUNNING");
                        lblEstadoSyncP2P.setBackground(Color.ORANGE);
                    }
                    if (panelPrincipal != null) {
                        panelPrincipal.agregarEstado("🔄 Sincronización P2P iniciada");
                    }
                });
                break;

            case "SINCRONIZACION_P2P_TERMINADA":
                LoggerCentral.info(TAG,
                        "🔔 Evento SINCRONIZACION_P2P_TERMINADA recibido en VentanaPrincipal (datos=" + datos + ")");
                SwingUtilities.invokeLater(() -> {
                    boolean huboCambios = datos instanceof Boolean && (Boolean) datos;
                    if (lblEstadoSyncP2P != null) {
                        lblEstadoSyncP2P.setText("Sync P2P: OK" + (huboCambios ? " (changes)" : ""));
                        lblEstadoSyncP2P.setBackground(new Color(0, 128, 0));
                    }
                    if (panelPrincipal != null) {
                        panelPrincipal.agregarEstado(
                                "✅ Sincronización P2P terminada" + (huboCambios ? " con cambios" : " sin cambios"));
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
