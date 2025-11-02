package com.arquitectura.vista;


import com.arquitectura.controlador.ServerViewController;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;


public class ServerMainWindow extends JFrame {

    private final ServerViewController controller;
    private JPanel mainContentPanel;
    private CardLayout cardLayout;

    // Paneles personalizados para cada vista
    private RegisterUserPanel registerUserPanel;
    private UsersReportPanel usersReportPanel;
    private ChannelsReportPanel channelsReportPanel;
    private LogsReportPanel logsReportPanel;
    private ConnectedUsersReportPanel connectedUsersReportPanel;
    private TranscriptionsReportPanel transcriptionsReportPanel;
    private BroadcastMessagePanel broadcastMessagePanel;
    // Aquí añadirías los otros paneles de informes a medida que los crees

    public ServerMainWindow(ServerViewController controller) {
        this.controller = controller;
        initComponents();
        // El addListeners() ahora está dentro de createButtonPanel para mayor claridad
    }

    private void initComponents() {
        setTitle("Admin Panel - Chat Server");
        setSize(900, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(0, 0)); // Quitamos el gap para que el borde funcione bien

        // --- Panel Izquierdo (Botones) ---
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.WEST);


        // --- Panel Derecho (Contenido Principal) ---
        cardLayout = new CardLayout();
        mainContentPanel = new JPanel(cardLayout);
        mainContentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Creamos una instancia de nuestros paneles personalizados
        registerUserPanel = new RegisterUserPanel(controller);
        usersReportPanel = new UsersReportPanel(controller);
        channelsReportPanel = new ChannelsReportPanel(controller);
        logsReportPanel = new LogsReportPanel(controller);
        connectedUsersReportPanel = new ConnectedUsersReportPanel(controller);
        transcriptionsReportPanel = new TranscriptionsReportPanel(controller);
        broadcastMessagePanel = new BroadcastMessagePanel(controller);

        // Añadimos los paneles al CardLayout con un nombre único
        mainContentPanel.add(registerUserPanel, "REGISTER_USER_PANEL");
        mainContentPanel.add(usersReportPanel, "USERS_REPORT_PANEL");
        mainContentPanel.add(channelsReportPanel, "CHANNELS_REPORT_PANEL");
        mainContentPanel.add(logsReportPanel, "LOGS_REPORT_PANEL");
        mainContentPanel.add(connectedUsersReportPanel, "CONNECTED_USERS_PANEL");
        mainContentPanel.add(transcriptionsReportPanel, "TRANSCRIPTIONS_PANEL");
        mainContentPanel.add(broadcastMessagePanel, "BROADCAST_PANEL");

        add(mainContentPanel, BorderLayout.CENTER);
        cardLayout.show(mainContentPanel, "REGISTER_USER_PANEL");
    }

    // MÉTODO MODIFICADO CON LOS CAMBIOS DE ESTILO
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // --- 1. FONDO MORADO OSCURO ---
        panel.setBackground(new Color(48, 25, 52)); // RGB para un morado oscuro

        // --- 2. LÍNEA SEPARADORA ---
        Border lineBorder = BorderFactory.createMatteBorder(0, 0, 0, 1, Color.LIGHT_GRAY);
        Border paddingBorder = BorderFactory.createEmptyBorder(10, 10, 10, 10);
        panel.setBorder(BorderFactory.createCompoundBorder(lineBorder, paddingBorder));

        // Título "Acciones" con color blanco para que sea legible
        JLabel title = new JLabel("Acciones");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setAlignmentX(Component.LEFT_ALIGNMENT); // Alinear a la izquierda
        panel.add(title);
        panel.add(Box.createRigidArea(new Dimension(0, 20))); // Espacio más grande

        // todos los botones aquí
        JButton btnShowRegister = new JButton("Registrar Usuario");
        JButton btnShowRegisteredUsers = new JButton("Usuarios Registrados");
        JButton btnChannelsWithUsers = new JButton("Canales con usuarios");
        JButton btnShowConnectedUsers = new JButton("Usuarios Conectados");
        JButton btnShowTranscriptions = new JButton("Texto de Mensaje de audio");
        JButton btnBroadcast = new JButton("Enviar Mensaje Global");
        JButton btnLogs = new JButton("Logs");
        // ... otros botones ...

        // Guardamos los botones en un array para manipularlos fácilmente
        JButton[] buttons = {
                btnShowRegister,
                btnShowRegisteredUsers,
                btnChannelsWithUsers,
                btnShowConnectedUsers,
                btnShowTranscriptions,
                btnLogs,
                btnBroadcast
                // ... añade los otros botones aquí
        };

        int maxWidth = 0;

        for (JButton btn : buttons) {
            panel.add(btn);
            panel.add(Box.createRigidArea(new Dimension(0, 10))); // Espacio entre botones

            // Cambiamos el color del texto para que se vea bien
            btn.setForeground(Color.WHITE);
            btn.setBackground(new Color(80, 60, 85)); // Un morado un poco más claro
            btn.setAlignmentX(Component.LEFT_ALIGNMENT);

            // Calculamos el ancho preferido más grande
            if (btn.getPreferredSize().width > maxWidth) {
                maxWidth = btn.getPreferredSize().width;
            }
        }

        // --- 3. BOTONES GRANDES Y DEL MISMO TAMAÑO ---
        Dimension buttonSize = new Dimension(maxWidth + 20, 40); // Ancho máximo + padding, y 40 de alto
        for (JButton btn : buttons) {
            btn.setMaximumSize(buttonSize);
        }
        // Asignamos los listeners aquí mismo
        btnShowRegister.addActionListener(e -> cardLayout.show(mainContentPanel, "REGISTER_USER_PANEL"));

        btnShowRegisteredUsers.addActionListener(e -> {
            usersReportPanel.refreshReport(); // Carga o refresca los datos
            cardLayout.show(mainContentPanel, "USERS_REPORT_PANEL");
        });
        btnShowConnectedUsers.addActionListener(e -> {
            logsReportPanel.stopAutoRefresh();
            connectedUsersReportPanel.refreshReport();
            cardLayout.show(mainContentPanel, "CONNECTED_USERS_PANEL");
        });

        btnShowTranscriptions.addActionListener(e -> {
            logsReportPanel.stopAutoRefresh();
            transcriptionsReportPanel.refreshReport();
            cardLayout.show(mainContentPanel, "TRANSCRIPTIONS_PANEL");
        });
        btnChannelsWithUsers.addActionListener(e -> {
            channelsReportPanel.refreshReport(); // Carga o refresca los datos del nuevo informe
            cardLayout.show(mainContentPanel, "CHANNELS_REPORT_PANEL");
        });
        btnLogs.addActionListener(e -> {
            logsReportPanel.refreshReport(true); // Carga y mueve el scroll al inicio
            logsReportPanel.startAutoRefresh(); // Inicia el auto-refresco al entrar
            cardLayout.show(mainContentPanel, "LOGS_REPORT_PANEL");
        });
        btnBroadcast.addActionListener(e -> {
            logsReportPanel.stopAutoRefresh(); // Detener el refresco de logs si estaba activo
            cardLayout.show(mainContentPanel, "BROADCAST_PANEL");
        });


        return panel;
    }

    public void display() {
        setVisible(true);
    }
}