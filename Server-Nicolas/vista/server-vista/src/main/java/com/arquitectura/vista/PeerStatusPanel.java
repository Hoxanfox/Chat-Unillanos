package com.arquitectura.vista;

import com.arquitectura.controlador.ServerViewController;
import com.arquitectura.DTO.PeerDTO;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class PeerStatusPanel extends JPanel {

    private final ServerViewController controller;
    private JTable peersTable;
    private DefaultTableModel tableModel;
    private JLabel totalPeersLabel;
    private JLabel activePeersLabel;
    private JLabel offlinePeersLabel;
    private JLabel lastUpdateLabel;
    private Timer autoRefreshTimer;

    public PeerStatusPanel(ServerViewController controller) {
        this.controller = controller;
        initComponents();
        startAutoRefresh();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Panel superior con título y estadísticas
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // Panel central con tabla de peers
        JPanel tablePanel = createTablePanel();
        add(tablePanel, BorderLayout.CENTER);

        // Panel inferior con botones
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(48, 25, 52));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Título
        JLabel titleLabel = new JLabel("🌐 Estado de Peers P2P");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        panel.add(titleLabel, BorderLayout.NORTH);

        // Panel de estadísticas
        JPanel statsPanel = new JPanel(new GridLayout(2, 2, 10, 5));
        statsPanel.setBackground(new Color(48, 25, 52));

        totalPeersLabel = createStatLabel("Total de Peers: 0");
        activePeersLabel = createStatLabel("Peers Activos: 0");
        offlinePeersLabel = createStatLabel("Peers Offline: 0");
        lastUpdateLabel = createStatLabel("Última actualización: --");

        statsPanel.add(totalPeersLabel);
        statsPanel.add(activePeersLabel);
        statsPanel.add(offlinePeersLabel);
        statsPanel.add(lastUpdateLabel);

        panel.add(statsPanel, BorderLayout.CENTER);

        return panel;
    }

    private JLabel createStatLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.PLAIN, 14));
        label.setForeground(Color.WHITE);
        return label;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Columnas de la tabla
        String[] columnNames = {"ID", "Nombre", "IP", "Puerto", "Estado", "Último Latido"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        peersTable = new JTable(tableModel);
        peersTable.setFont(new Font("Monospaced", Font.PLAIN, 12));
        peersTable.setRowHeight(25);
        peersTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        peersTable.getTableHeader().setBackground(new Color(80, 60, 85));
        peersTable.getTableHeader().setForeground(Color.WHITE);

        // Renderer personalizado para la columna de estado
        peersTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String status = value.toString();
                
                if (status.equals("ONLINE")) {
                    c.setBackground(new Color(144, 238, 144)); // Verde claro
                    c.setForeground(Color.BLACK);
                    setText("🟢 ONLINE");
                } else if (status.equals("OFFLINE")) {
                    c.setBackground(new Color(255, 182, 193)); // Rojo claro
                    c.setForeground(Color.BLACK);
                    setText("🔴 OFFLINE");
                } else {
                    c.setBackground(new Color(255, 255, 224)); // Amarillo claro
                    c.setForeground(Color.BLACK);
                    setText("🟡 " + status);
                }
                
                if (isSelected) {
                    c.setBackground(table.getSelectionBackground());
                    c.setForeground(table.getSelectionForeground());
                }
                
                setHorizontalAlignment(CENTER);
                return c;
            }
        });

        // Ajustar anchos de columnas
        peersTable.getColumnModel().getColumn(0).setPreferredWidth(80);  // ID
        peersTable.getColumnModel().getColumn(1).setPreferredWidth(150); // Nombre
        peersTable.getColumnModel().getColumn(2).setPreferredWidth(120); // IP
        peersTable.getColumnModel().getColumn(3).setPreferredWidth(80);  // Puerto
        peersTable.getColumnModel().getColumn(4).setPreferredWidth(100); // Estado
        peersTable.getColumnModel().getColumn(5).setPreferredWidth(180); // Último Latido

        JScrollPane scrollPane = new JScrollPane(peersTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Lista de Peers"));
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

        panel.add(refreshButton);
        panel.add(autoRefreshToggle);

        return panel;
    }

    public void refreshReport() {
        try {
            List<PeerDTO> peers = controller.getAllPeers();
            
            // Limpiar tabla
            tableModel.setRowCount(0);

            // Contadores
            int totalPeers = peers.size();
            int activePeers = 0;
            int offlinePeers = 0;

            // Llenar tabla
            for (PeerDTO peer : peers) {
                String id = peer.getId().toString().substring(0, 8) + "...";
                String nombre = peer.getNombreServidor();
                String ip = peer.getIp();
                String puerto = String.valueOf(peer.getPuerto());
                String estado = peer.getConectado().toString();
                String ultimoLatido = peer.getUltimoLatido() != null 
                    ? peer.getUltimoLatido().toString() 
                    : "N/A";

                tableModel.addRow(new Object[]{id, nombre, ip, puerto, estado, ultimoLatido});

                if ("ONLINE".equals(estado)) {
                    activePeers++;
                } else if ("OFFLINE".equals(estado)) {
                    offlinePeers++;
                }
            }

            // Actualizar estadísticas
            totalPeersLabel.setText("Total de Peers: " + totalPeers);
            activePeersLabel.setText("Peers Activos: " + activePeers);
            offlinePeersLabel.setText("Peers Offline: " + offlinePeers);
            lastUpdateLabel.setText("Última actualización: " + java.time.LocalTime.now().toString());

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error al cargar peers: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void startAutoRefresh() {
        // Auto-refresco cada 10 segundos
        autoRefreshTimer = new Timer(10000, e -> refreshReport());
        autoRefreshTimer.start();
    }

    public void stopAutoRefresh() {
        if (autoRefreshTimer != null && autoRefreshTimer.isRunning()) {
            autoRefreshTimer.stop();
        }
    }
}
