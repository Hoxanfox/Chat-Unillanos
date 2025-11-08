package com.arquitectura.vista;

import com.arquitectura.controlador.ServerViewController;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Panel para visualizar la distribución de usuarios en la red P2P.
 */
public class P2PUsersDistributionPanel extends JPanel {
    
    private final ServerViewController controller;
    private JTable usersTable;
    private DefaultTableModel tableModel;
    private JLabel totalUsersLabel;
    private JLabel connectedUsersLabel;
    private JLabel offlineUsersLabel;
    
    public P2PUsersDistributionPanel(ServerViewController controller) {
        this.controller = controller;
        initComponents();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Panel superior con título y estadísticas
        JPanel topPanel = new JPanel(new BorderLayout());
        
        JLabel titleLabel = new JLabel("Distribución de Usuarios P2P");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        topPanel.add(titleLabel, BorderLayout.NORTH);
        
        // Panel de estadísticas
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
        statsPanel.setBorder(BorderFactory.createTitledBorder("Estadísticas"));
        
        totalUsersLabel = new JLabel("Total Usuarios: 0");
        totalUsersLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        
        connectedUsersLabel = new JLabel("Conectados: 0");
        connectedUsersLabel.setFont(new Font("Arial", Font.BOLD, 14));
        connectedUsersLabel.setForeground(new Color(0, 150, 0));
        
        offlineUsersLabel = new JLabel("Offline: 0");
        offlineUsersLabel.setFont(new Font("Arial", Font.BOLD, 14));
        offlineUsersLabel.setForeground(new Color(200, 0, 0));
        
        statsPanel.add(totalUsersLabel);
        statsPanel.add(connectedUsersLabel);
        statsPanel.add(offlineUsersLabel);
        
        topPanel.add(statsPanel, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);
        
        // Tabla de usuarios
        String[] columnNames = {"Usuario ID", "Username", "Estado", "Peer ID", "Peer IP", "Peer Puerto"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        usersTable = new JTable(tableModel);
        usersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        usersTable.setRowHeight(25);
        usersTable.getTableHeader().setReorderingAllowed(false);
        
        // Ajustar anchos de columnas
        usersTable.getColumnModel().getColumn(0).setPreferredWidth(250);
        usersTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        usersTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        usersTable.getColumnModel().getColumn(3).setPreferredWidth(250);
        usersTable.getColumnModel().getColumn(4).setPreferredWidth(120);
        usersTable.getColumnModel().getColumn(5).setPreferredWidth(80);
        
        JScrollPane scrollPane = new JScrollPane(usersTable);
        add(scrollPane, BorderLayout.CENTER);
        
        // Panel inferior con botones
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton refreshButton = new JButton("Refrescar");
        refreshButton.addActionListener(e -> refreshReport());
        
        JButton syncButton = new JButton("Sincronizar Usuarios");
        syncButton.addActionListener(e -> syncUsers());
        
        bottomPanel.add(refreshButton);
        bottomPanel.add(syncButton);
        
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    public void refreshReport() {
        try {
            var usersData = controller.sincronizarUsuarios();
            
            // Limpiar tabla
            tableModel.setRowCount(0);
            
            int totalUsers = 0;
            int connectedUsers = 0;
            int offlineUsers = 0;
            
            if (usersData != null && usersData.containsKey("usuarios")) {
                java.util.List<?> usersList = (java.util.List<?>) usersData.get("usuarios");
                totalUsers = usersList.size();
                
                for (Object userObj : usersList) {
                    if (userObj instanceof java.util.Map) {
                        java.util.Map<?, ?> user = (java.util.Map<?, ?>) userObj;
                        
                        String usuarioId = user.get("usuarioId") != null ? user.get("usuarioId").toString() : "N/A";
                        String username = user.get("username") != null ? user.get("username").toString() : "N/A";
                        boolean conectado = user.get("conectado") != null && 
                            Boolean.parseBoolean(user.get("conectado").toString());
                        String estado = conectado ? "ONLINE" : "OFFLINE";
                        
                        String peerId = "N/A";
                        String peerIp = "N/A";
                        String peerPuerto = "N/A";
                        
                        if (conectado && user.get("peerId") != null) {
                            peerId = user.get("peerId").toString();
                            peerIp = user.get("peerIp") != null ? user.get("peerIp").toString() : "N/A";
                            peerPuerto = user.get("peerPuerto") != null ? user.get("peerPuerto").toString() : "N/A";
                            connectedUsers++;
                        } else {
                            offlineUsers++;
                        }
                        
                        tableModel.addRow(new Object[]{usuarioId, username, estado, peerId, peerIp, peerPuerto});
                    }
                }
            }
            
            // Actualizar estadísticas
            totalUsersLabel.setText("Total Usuarios: " + totalUsers);
            connectedUsersLabel.setText("Conectados: " + connectedUsers);
            offlineUsersLabel.setText("Offline: " + offlineUsers);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error al cargar usuarios: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void syncUsers() {
        try {
            refreshReport();
            JOptionPane.showMessageDialog(this,
                "Usuarios sincronizados exitosamente",
                "Éxito",
                JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error al sincronizar usuarios: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
}
