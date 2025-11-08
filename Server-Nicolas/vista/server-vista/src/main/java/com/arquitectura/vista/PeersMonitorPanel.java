package com.arquitectura.vista;

import com.arquitectura.controlador.ServerViewController;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Panel para monitorear el estado de los peers en la red P2P.
 */
public class PeersMonitorPanel extends JPanel {
    
    private final ServerViewController controller;
    private JTable peersTable;
    private DefaultTableModel tableModel;
    private JLabel totalPeersLabel;
    private JLabel onlinePeersLabel;
    private JLabel offlinePeersLabel;
    private Timer autoRefreshTimer;
    
    public PeersMonitorPanel(ServerViewController controller) {
        this.controller = controller;
        initComponents();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Panel superior con título y estadísticas
        JPanel topPanel = new JPanel(new BorderLayout());
        
        JLabel titleLabel = new JLabel("Monitoreo de Peers P2P");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        topPanel.add(titleLabel, BorderLayout.NORTH);
        
        // Panel de estadísticas
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
        statsPanel.setBorder(BorderFactory.createTitledBorder("Estadísticas"));
        
        totalPeersLabel = new JLabel("Total Peers: 0");
        totalPeersLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        
        onlinePeersLabel = new JLabel("Online: 0");
        onlinePeersLabel.setFont(new Font("Arial", Font.BOLD, 14));
        onlinePeersLabel.setForeground(new Color(0, 150, 0));
        
        offlinePeersLabel = new JLabel("Offline: 0");
        offlinePeersLabel.setFont(new Font("Arial", Font.BOLD, 14));
        offlinePeersLabel.setForeground(new Color(200, 0, 0));
        
        statsPanel.add(totalPeersLabel);
        statsPanel.add(onlinePeersLabel);
        statsPanel.add(offlinePeersLabel);
        
        topPanel.add(statsPanel, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);
        
        // Tabla de peers
        String[] columnNames = {"Peer ID", "IP", "Puerto", "Estado", "Usuarios Conectados", "Último Latido"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        peersTable = new JTable(tableModel);
        peersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        peersTable.setRowHeight(25);
        peersTable.getTableHeader().setReorderingAllowed(false);
        
        // Ajustar anchos de columnas
        peersTable.getColumnModel().getColumn(0).setPreferredWidth(250);
        peersTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        peersTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        peersTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        peersTable.getColumnModel().getColumn(4).setPreferredWidth(150);
        peersTable.getColumnModel().getColumn(5).setPreferredWidth(150);
        
        JScrollPane scrollPane = new JScrollPane(peersTable);
        add(scrollPane, BorderLayout.CENTER);
        
        // Panel inferior con botones
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton refreshButton = new JButton("Refrescar");
        refreshButton.addActionListener(e -> refreshReport());
        
        JButton pingAllButton = new JButton("Ping a Todos");
        pingAllButton.addActionListener(e -> pingAllPeers());
        
        JButton cleanupButton = new JButton("Limpiar Offline");
        cleanupButton.setToolTipText("Eliminar peers que están offline");
        cleanupButton.addActionListener(e -> cleanupOfflinePeers());
        
        bottomPanel.add(refreshButton);
        bottomPanel.add(pingAllButton);
        bottomPanel.add(cleanupButton);
        
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    public void refreshReport() {
        try {
            var peersData = controller.listarPeersDisponibles();
            
            // Limpiar tabla
            tableModel.setRowCount(0);
            
            int totalPeers = 0;
            int onlinePeers = 0;
            int offlinePeers = 0;
            
            if (peersData != null && peersData.containsKey("peers")) {
                java.util.List<?> peersList = (java.util.List<?>) peersData.get("peers");
                totalPeers = peersList.size();
                
                for (Object peerObj : peersList) {
                    if (peerObj instanceof java.util.Map) {
                        java.util.Map<?, ?> peer = (java.util.Map<?, ?>) peerObj;
                        
                        String peerId = peer.get("peerId") != null ? peer.get("peerId").toString() : "N/A";
                        String ip = peer.get("ip") != null ? peer.get("ip").toString() : "N/A";
                        String puerto = peer.get("puerto") != null ? peer.get("puerto").toString() : "N/A";
                        String estado = peer.get("conectado") != null ? peer.get("conectado").toString() : "OFFLINE";
                        String usuariosConectados = "0"; // Por defecto
                        String ultimoLatido = "N/A";
                        
                        // Normalizar localhost a 127.0.0.1 para evitar confusión
                        if ("localhost".equalsIgnoreCase(ip)) {
                            ip = "127.0.0.1 (localhost)";
                        }
                        
                        if ("ONLINE".equalsIgnoreCase(estado)) {
                            onlinePeers++;
                        } else {
                            offlinePeers++;
                        }
                        
                        tableModel.addRow(new Object[]{peerId, ip, puerto, estado, usuariosConectados, ultimoLatido});
                    }
                }
            }
            
            // Actualizar estadísticas
            totalPeersLabel.setText("Total Peers: " + totalPeers);
            onlinePeersLabel.setText("Online: " + onlinePeers);
            offlinePeersLabel.setText("Offline: " + offlinePeers);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error al cargar peers: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void pingAllPeers() {
        try {
            // Mostrar diálogo de progreso
            JDialog progressDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), 
                "Ping en progreso", true);
            progressDialog.setLayout(new BorderLayout(10, 10));
            progressDialog.setSize(300, 100);
            progressDialog.setLocationRelativeTo(this);
            
            JLabel messageLabel = new JLabel("Enviando ping a todos los peers...", SwingConstants.CENTER);
            progressDialog.add(messageLabel, BorderLayout.CENTER);
            
            // Ejecutar ping en un hilo separado
            SwingWorker<java.util.List<java.util.Map<String, Object>>, Void> worker = 
                new SwingWorker<java.util.List<java.util.Map<String, Object>>, Void>() {
                
                @Override
                protected java.util.List<java.util.Map<String, Object>> doInBackground() throws Exception {
                    return controller.pingAllPeers();
                }
                
                @Override
                protected void done() {
                    progressDialog.dispose();
                    try {
                        java.util.List<java.util.Map<String, Object>> results = get();
                        
                        // Construir mensaje con resultados
                        StringBuilder message = new StringBuilder();
                        message.append("Resultados del Ping:\n\n");
                        
                        int successful = 0;
                        int failed = 0;
                        
                        for (java.util.Map<String, Object> result : results) {
                            String peerId = result.get("peerId") != null ? 
                                result.get("peerId").toString().substring(0, 8) + "..." : "N/A";
                            boolean success = result.get("success") != null && 
                                Boolean.parseBoolean(result.get("success").toString());
                            
                            if (success) {
                                successful++;
                                int latencia = result.get("latencia") != null ? 
                                    Integer.parseInt(result.get("latencia").toString()) : 0;
                                message.append(String.format("✓ Peer [%s]: %d ms\n", peerId, latencia));
                            } else {
                                failed++;
                                String error = result.get("error") != null ? 
                                    result.get("error").toString() : "Error desconocido";
                                message.append(String.format("✗ Peer [%s]: %s\n", peerId, error));
                            }
                        }
                        
                        message.append(String.format("\nTotal: %d | Exitosos: %d | Fallidos: %d", 
                            results.size(), successful, failed));
                        
                        JOptionPane.showMessageDialog(PeersMonitorPanel.this,
                            message.toString(),
                            "Resultados de Ping",
                            JOptionPane.INFORMATION_MESSAGE);
                        
                        // Refrescar la tabla
                        refreshReport();
                        
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(PeersMonitorPanel.this,
                            "Error al hacer ping: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            
            worker.execute();
            progressDialog.setVisible(true);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error al iniciar ping: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public void startAutoRefresh() {
        if (autoRefreshTimer == null) {
            autoRefreshTimer = new Timer(5000, e -> refreshReport());
            autoRefreshTimer.start();
        }
    }
    
    public void stopAutoRefresh() {
        if (autoRefreshTimer != null) {
            autoRefreshTimer.stop();
            autoRefreshTimer = null;
        }
    }
    
    private void cleanupOfflinePeers() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "¿Está seguro de que desea eliminar todos los peers OFFLINE?\n" +
            "Esta acción no se puede deshacer.",
            "Confirmar Limpieza",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            JOptionPane.showMessageDialog(this,
                "Funcionalidad de limpieza a implementar en el backend.\n" +
                "Por ahora, los peers OFFLINE se pueden identificar en la tabla.",
                "Info",
                JOptionPane.INFORMATION_MESSAGE);
            
            // Aquí se implementaría la llamada al backend para eliminar peers offline
            // controller.cleanupOfflinePeers();
            // refreshReport();
        }
    }
}
