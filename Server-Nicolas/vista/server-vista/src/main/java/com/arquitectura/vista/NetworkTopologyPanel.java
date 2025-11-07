package com.arquitectura.vista;

import com.arquitectura.controlador.ServerViewController;

import javax.swing.*;
import java.awt.*;

/**
 * Panel para visualizar la topología de la red P2P.
 */
public class NetworkTopologyPanel extends JPanel {
    
    private final ServerViewController controller;
    private JTextArea topologyTextArea;
    private JLabel totalPeersLabel;
    private JLabel totalUsersLabel;
    private JLabel connectedUsersLabel;
    
    public NetworkTopologyPanel(ServerViewController controller) {
        this.controller = controller;
        initComponents();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Panel superior con título
        JPanel topPanel = new JPanel(new BorderLayout());
        
        JLabel titleLabel = new JLabel("Topología de Red P2P");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        topPanel.add(titleLabel, BorderLayout.NORTH);
        
        // Panel de estadísticas generales
        JPanel statsPanel = new JPanel(new GridLayout(2, 3, 10, 10));
        statsPanel.setBorder(BorderFactory.createTitledBorder("Estadísticas Generales"));
        
        totalPeersLabel = new JLabel("Total Peers: 0");
        totalPeersLabel.setFont(new Font("Arial", Font.BOLD, 14));
        
        totalUsersLabel = new JLabel("Total Usuarios: 0");
        totalUsersLabel.setFont(new Font("Arial", Font.BOLD, 14));
        
        connectedUsersLabel = new JLabel("Usuarios Conectados: 0");
        connectedUsersLabel.setFont(new Font("Arial", Font.BOLD, 14));
        connectedUsersLabel.setForeground(new Color(0, 150, 0));
        
        JLabel onlinePeersLabel = new JLabel("Peers Online: 0");
        onlinePeersLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        
        JLabel offlinePeersLabel = new JLabel("Peers Offline: 0");
        offlinePeersLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        
        JLabel publicChannelsLabel = new JLabel("Canales Públicos: 0");
        publicChannelsLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        
        statsPanel.add(totalPeersLabel);
        statsPanel.add(totalUsersLabel);
        statsPanel.add(connectedUsersLabel);
        statsPanel.add(onlinePeersLabel);
        statsPanel.add(offlinePeersLabel);
        statsPanel.add(publicChannelsLabel);
        
        topPanel.add(statsPanel, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);
        
        // Área de texto para mostrar la topología
        topologyTextArea = new JTextArea();
        topologyTextArea.setEditable(false);
        topologyTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        topologyTextArea.setMargin(new Insets(10, 10, 10, 10));
        
        JScrollPane scrollPane = new JScrollPane(topologyTextArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Visualización de Red"));
        add(scrollPane, BorderLayout.CENTER);
        
        // Panel inferior con botones
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton refreshButton = new JButton("Refrescar");
        refreshButton.addActionListener(e -> refreshReport());
        
        JButton detailedViewButton = new JButton("Vista Detallada");
        detailedViewButton.addActionListener(e -> showDetailedView());
        
        bottomPanel.add(refreshButton);
        bottomPanel.add(detailedViewButton);
        
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    public void refreshReport() {
        try {
            // Obtener estado de la red desde el controlador
            var networkState = controller.obtenerEstadoRed(true);
            
            if (networkState != null) {
                StringBuilder topology = new StringBuilder();
                topology.append("═══════════════════════════════════════════════════════════════\n");
                topology.append("                    TOPOLOGÍA DE RED P2P\n");
                topology.append("═══════════════════════════════════════════════════════════════\n\n");
                
                // Información de topología
                if (networkState.containsKey("topologia")) {
                    java.util.Map<?, ?> topoData = (java.util.Map<?, ?>) networkState.get("topologia");
                    
                    int totalPeers = topoData.get("totalPeers") != null ? 
                        Integer.parseInt(topoData.get("totalPeers").toString()) : 0;
                    int peersOnline = topoData.get("peersOnline") != null ? 
                        Integer.parseInt(topoData.get("peersOnline").toString()) : 0;
                    int peersOffline = topoData.get("peersOffline") != null ? 
                        Integer.parseInt(topoData.get("peersOffline").toString()) : 0;
                    
                    totalPeersLabel.setText("Total Peers: " + totalPeers);
                    
                    topology.append("PEERS EN LA RED:\n");
                    topology.append("───────────────────────────────────────────────────────────────\n");
                    topology.append(String.format("  Total: %d | Online: %d | Offline: %d\n\n", 
                        totalPeers, peersOnline, peersOffline));
                    
                    // Listar peers si están disponibles
                    if (topoData.containsKey("peers")) {
                        java.util.List<?> peers = (java.util.List<?>) topoData.get("peers");
                        
                        for (Object peerObj : peers) {
                            if (peerObj instanceof java.util.Map) {
                                java.util.Map<?, ?> peer = (java.util.Map<?, ?>) peerObj;
                                
                                String peerId = peer.get("peerId") != null ? 
                                    peer.get("peerId").toString().substring(0, 8) + "..." : "N/A";
                                String ip = peer.get("ip") != null ? peer.get("ip").toString() : "N/A";
                                String puerto = peer.get("puerto") != null ? peer.get("puerto").toString() : "N/A";
                                String estado = peer.get("estado") != null ? peer.get("estado").toString() : "OFFLINE";
                                int usuarios = peer.get("usuariosConectados") != null ? 
                                    Integer.parseInt(peer.get("usuariosConectados").toString()) : 0;
                                
                                String statusIcon = "ONLINE".equalsIgnoreCase(estado) ? "●" : "○";
                                topology.append(String.format("  %s Peer [%s]\n", statusIcon, peerId));
                                topology.append(String.format("     └─ %s:%s | %d usuarios\n\n", ip, puerto, usuarios));
                            }
                        }
                    }
                }
                
                // Información de usuarios
                if (networkState.containsKey("usuarios")) {
                    java.util.Map<?, ?> userData = (java.util.Map<?, ?>) networkState.get("usuarios");
                    
                    int totalUsers = userData.get("totalUsuarios") != null ? 
                        Integer.parseInt(userData.get("totalUsuarios").toString()) : 0;
                    int connectedUsers = userData.get("usuariosConectados") != null ? 
                        Integer.parseInt(userData.get("usuariosConectados").toString()) : 0;
                    
                    totalUsersLabel.setText("Total Usuarios: " + totalUsers);
                    connectedUsersLabel.setText("Usuarios Conectados: " + connectedUsers);
                    
                    topology.append("\nUSUARIOS EN LA RED:\n");
                    topology.append("───────────────────────────────────────────────────────────────\n");
                    topology.append(String.format("  Total: %d | Conectados: %d | Offline: %d\n\n", 
                        totalUsers, connectedUsers, totalUsers - connectedUsers));
                    
                    // Distribución por peer
                    if (userData.containsKey("distribucion")) {
                        java.util.List<?> distribucion = (java.util.List<?>) userData.get("distribucion");
                        
                        topology.append("  Distribución por Peer:\n");
                        for (Object distObj : distribucion) {
                            if (distObj instanceof java.util.Map) {
                                java.util.Map<?, ?> dist = (java.util.Map<?, ?>) distObj;
                                String peerId = dist.get("peerId") != null ? 
                                    dist.get("peerId").toString().substring(0, 8) + "..." : "N/A";
                                int cantidad = dist.get("cantidad") != null ? 
                                    Integer.parseInt(dist.get("cantidad").toString()) : 0;
                                
                                topology.append(String.format("    • Peer [%s]: %d usuarios\n", peerId, cantidad));
                            }
                        }
                    }
                }
                
                topology.append("\n═══════════════════════════════════════════════════════════════\n");
                topology.append("Última actualización: ").append(java.time.LocalDateTime.now().toString()).append("\n");
                
                topologyTextArea.setText(topology.toString());
                topologyTextArea.setCaretPosition(0);
            }
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error al cargar topología: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void showDetailedView() {
        JOptionPane.showMessageDialog(this,
            "Vista detallada con gráficos a implementar",
            "Info",
            JOptionPane.INFORMATION_MESSAGE);
    }
}
