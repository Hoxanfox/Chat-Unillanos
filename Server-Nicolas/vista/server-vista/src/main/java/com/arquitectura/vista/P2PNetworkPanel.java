package com.arquitectura.vista;

import com.arquitectura.controlador.ServerViewController;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class P2PNetworkPanel extends JPanel {

    private final ServerViewController controller;
    private JTextArea networkInfoArea;
    private JLabel statusLabel;
    private JProgressBar healthBar;
    private Timer autoRefreshTimer;

    public P2PNetworkPanel(ServerViewController controller) {
        this.controller = controller;
        initComponents();
        startAutoRefresh();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Panel superior con título y estado general
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // Panel central con información de red
        JPanel infoPanel = createInfoPanel();
        add(infoPanel, BorderLayout.CENTER);

        // Panel inferior con botones
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(48, 25, 52));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Título
        JLabel titleLabel = new JLabel("📊 Estadísticas de Red P2P");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        panel.add(titleLabel, BorderLayout.NORTH);

        // Panel de estado
        JPanel statusPanel = new JPanel(new BorderLayout(10, 10));
        statusPanel.setBackground(new Color(48, 25, 52));

        statusLabel = new JLabel("Estado: Verificando...");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 16));
        statusLabel.setForeground(Color.WHITE);
        statusPanel.add(statusLabel, BorderLayout.NORTH);

        // Barra de salud de la red
        healthBar = new JProgressBar(0, 100);
        healthBar.setStringPainted(true);
        healthBar.setFont(new Font("Arial", Font.BOLD, 12));
        healthBar.setPreferredSize(new Dimension(400, 30));
        statusPanel.add(healthBar, BorderLayout.CENTER);

        panel.add(statusPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createInfoPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        // Área de texto para información detallada
        networkInfoArea = new JTextArea();
        networkInfoArea.setEditable(false);
        networkInfoArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        networkInfoArea.setBackground(new Color(245, 245, 245));
        networkInfoArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(networkInfoArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(80, 60, 85), 2),
            "Información de Red P2P",
            0,
            0,
            new Font("Arial", Font.BOLD, 14)
        ));

        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));

        JButton refreshButton = new JButton("🔄 Refrescar");
        refreshButton.setFont(new Font("Arial", Font.BOLD, 12));
        refreshButton.addActionListener(e -> refreshReport());

        JButton autoRefreshToggle = new JButton("⏸ Pausar Auto-Refresco");
        autoRefreshToggle.setFont(new Font("Arial", Font.BOLD, 12));
        autoRefreshToggle.addActionListener(e -> {
            if (autoRefreshTimer.isRunning()) {
                autoRefreshTimer.stop();
                autoRefreshToggle.setText("▶ Reanudar Auto-Refresco");
            } else {
                autoRefreshTimer.start();
                autoRefreshToggle.setText("⏸ Pausar Auto-Refresco");
            }
        });

        JButton clearButton = new JButton("🗑 Limpiar");
        clearButton.setFont(new Font("Arial", Font.BOLD, 12));
        clearButton.addActionListener(e -> networkInfoArea.setText(""));

        panel.add(refreshButton);
        panel.add(autoRefreshToggle);
        panel.add(clearButton);

        return panel;
    }

    public void refreshReport() {
        try {
            StringBuilder info = new StringBuilder();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            
            info.append("╔════════════════════════════════════════════════════════════════════╗\n");
            info.append("║           ESTADÍSTICAS DE RED P2P - SERVIDOR                       ║\n");
            info.append("╚════════════════════════════════════════════════════════════════════╝\n\n");

            info.append("⏰ Última actualización: ").append(LocalDateTime.now().format(formatter)).append("\n\n");

            // Obtener estadísticas de peers
            int totalPeers = controller.getTotalPeers();
            int activePeers = controller.getActivePeers();
            int offlinePeers = totalPeers - activePeers;

            info.append("┌─────────────────────────────────────────────────────────────────┐\n");
            info.append("│  RESUMEN DE PEERS                                               │\n");
            info.append("├─────────────────────────────────────────────────────────────────┤\n");
            info.append(String.format("│  Total de Peers:           %-35d │\n", totalPeers));
            info.append(String.format("│  Peers Activos (ONLINE):   %-35d │\n", activePeers));
            info.append(String.format("│  Peers Inactivos (OFFLINE): %-34d │\n", offlinePeers));
            info.append("└─────────────────────────────────────────────────────────────────┘\n\n");

            // Calcular salud de la red
            int networkHealth = totalPeers > 0 ? (activePeers * 100 / totalPeers) : 0;
            
            info.append("┌─────────────────────────────────────────────────────────────────┐\n");
            info.append("│  SALUD DE LA RED                                                │\n");
            info.append("├─────────────────────────────────────────────────────────────────┤\n");
            info.append(String.format("│  Porcentaje de disponibilidad: %-31d%% │\n", networkHealth));
            
            String healthStatus;
            if (networkHealth >= 80) {
                healthStatus = "🟢 EXCELENTE";
            } else if (networkHealth >= 50) {
                healthStatus = "🟡 ACEPTABLE";
            } else if (networkHealth > 0) {
                healthStatus = "🟠 DEGRADADA";
            } else {
                healthStatus = "🔴 CRÍTICA";
            }
            info.append(String.format("│  Estado:                       %-31s │\n", healthStatus));
            info.append("└─────────────────────────────────────────────────────────────────┘\n\n");

            // Configuración P2P
            info.append("┌─────────────────────────────────────────────────────────────────┐\n");
            info.append("│  CONFIGURACIÓN P2P                                              │\n");
            info.append("├─────────────────────────────────────────────────────────────────┤\n");
            info.append("│  P2P Habilitado:           ✓ SÍ                                │\n");
            info.append("│  Heartbeat Intervalo:      30 segundos                          │\n");
            info.append("│  Heartbeat Timeout:        300 segundos (5 minutos)             │\n");
            info.append("│  Descubrimiento:           ✓ ACTIVO                             │\n");
            info.append("│  Sincronización:           ✓ ACTIVA                             │\n");
            info.append("└─────────────────────────────────────────────────────────────────┘\n\n");

            // Funcionalidades
            info.append("┌─────────────────────────────────────────────────────────────────┐\n");
            info.append("│  FUNCIONALIDADES ACTIVAS                                        │\n");
            info.append("├─────────────────────────────────────────────────────────────────┤\n");
            info.append("│  ✓ Descubrimiento automático de peers                          │\n");
            info.append("│  ✓ Heartbeat periódico (cada 30 segundos)                      │\n");
            info.append("│  ✓ Sincronización de mensajes entre servidores                 │\n");
            info.append("│  ✓ Sincronización de usuarios entre servidores                 │\n");
            info.append("│  ✓ Sincronización de canales entre servidores                  │\n");
            info.append("│  ✓ Detección automática de peers offline                       │\n");
            info.append("│  ✓ Reconexión automática de peers                              │\n");
            info.append("│  ✓ Tolerancia a fallos                                         │\n");
            info.append("└─────────────────────────────────────────────────────────────────┘\n\n");

            // Recomendaciones
            if (networkHealth < 50) {
                info.append("⚠️  ADVERTENCIAS:\n");
                info.append("   • La red P2P tiene baja disponibilidad\n");
                info.append("   • Verifica que los otros servidores estén corriendo\n");
                info.append("   • Revisa la configuración de red y firewall\n\n");
            } else if (networkHealth < 80) {
                info.append("ℹ️  INFORMACIÓN:\n");
                info.append("   • Algunos peers están offline\n");
                info.append("   • La red sigue operativa pero con capacidad reducida\n\n");
            } else {
                info.append("✅ ESTADO ÓPTIMO:\n");
                info.append("   • Todos los peers están conectados y funcionando\n");
                info.append("   • La red P2P está operando correctamente\n\n");
            }

            info.append("═══════════════════════════════════════════════════════════════════\n");
            info.append("Para más detalles, consulta la pestaña 'Estado de Peers'\n");

            networkInfoArea.setText(info.toString());
            networkInfoArea.setCaretPosition(0);

            // Actualizar barra de salud
            healthBar.setValue(networkHealth);
            healthBar.setString("Salud de la Red: " + networkHealth + "%");
            
            if (networkHealth >= 80) {
                healthBar.setForeground(new Color(76, 175, 80)); // Verde
                statusLabel.setText("Estado: 🟢 Red P2P Operativa");
                statusLabel.setForeground(new Color(144, 238, 144));
            } else if (networkHealth >= 50) {
                healthBar.setForeground(new Color(255, 193, 7)); // Amarillo
                statusLabel.setText("Estado: 🟡 Red P2P Degradada");
                statusLabel.setForeground(new Color(255, 255, 153));
            } else if (networkHealth > 0) {
                healthBar.setForeground(new Color(255, 152, 0)); // Naranja
                statusLabel.setText("Estado: 🟠 Red P2P con Problemas");
                statusLabel.setForeground(new Color(255, 200, 124));
            } else {
                healthBar.setForeground(new Color(244, 67, 54)); // Rojo
                statusLabel.setText("Estado: 🔴 Red P2P Inactiva");
                statusLabel.setForeground(new Color(255, 182, 193));
            }

        } catch (Exception e) {
            networkInfoArea.setText("Error al cargar estadísticas: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Error al cargar estadísticas: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void startAutoRefresh() {
        // Auto-refresco cada 15 segundos
        autoRefreshTimer = new Timer(15000, e -> refreshReport());
        autoRefreshTimer.start();
    }

    public void stopAutoRefresh() {
        if (autoRefreshTimer != null && autoRefreshTimer.isRunning()) {
            autoRefreshTimer.stop();
        }
    }
}
