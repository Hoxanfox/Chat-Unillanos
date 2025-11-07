package com.arquitectura.vista;

import com.arquitectura.controlador.ServerViewController;
import com.arquitectura.DTO.p2p.PeerResponseDto;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Panel para mostrar el reporte de peers (servidores) conectados.
 * Muestra información sobre los otros servidores en la red P2P.
 */
public class PeersReportPanel extends JPanel {

    private final ServerViewController controller;
    private JTable table;
    private DefaultTableModel tableModel;
    private JButton btnRefresh;
    private JLabel lblStatus;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public PeersReportPanel(ServerViewController controller) {
        this.controller = controller;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Panel Superior (Título y Botón de Refrescar) ---
        JPanel topPanel = new JPanel(new BorderLayout(10, 0));

        JLabel title = new JLabel("Servidores Conectados (Peers)");
        title.setFont(new Font("Arial", Font.BOLD, 18));
        topPanel.add(title, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnRefresh = new JButton("Refrescar");
        btnRefresh.addActionListener(e -> refreshReport());
        buttonPanel.add(btnRefresh);
        topPanel.add(buttonPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // --- Tabla de Peers ---
        String[] columnNames = {
            "ID Peer",
            "Nombre Servidor",
            "IP",
            "Puerto",
            "Estado",
            "Último Latido"
        };

        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Tabla no editable
            }
        };

        table = new JTable(tableModel);
        table.setRowHeight(25);
        table.getTableHeader().setReorderingAllowed(false);

        // Ajustar ancho de columnas
        table.getColumnModel().getColumn(0).setPreferredWidth(100); // ID
        table.getColumnModel().getColumn(1).setPreferredWidth(150); // Nombre
        table.getColumnModel().getColumn(2).setPreferredWidth(120); // IP
        table.getColumnModel().getColumn(3).setPreferredWidth(80);  // Puerto
        table.getColumnModel().getColumn(4).setPreferredWidth(100); // Estado
        table.getColumnModel().getColumn(5).setPreferredWidth(150); // Último Latido

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        // --- Panel Inferior (Estado) ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblStatus = new JLabel("Listo");
        lblStatus.setForeground(Color.BLUE);
        bottomPanel.add(lblStatus);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * Refresca el reporte cargando los datos desde el controlador.
     */
    public void refreshReport() {
        try {
            lblStatus.setText("Cargando peers...");
            lblStatus.setForeground(Color.BLUE);

            // Limpiar tabla
            tableModel.setRowCount(0);

            // Obtener peers desde el controlador
            List<PeerResponseDto> peers = controller.obtenerPeersDisponibles();

            if (peers == null || peers.isEmpty()) {
                lblStatus.setText("No hay peers conectados en este momento");
                lblStatus.setForeground(Color.ORANGE);
                return;
            }

            // Llenar tabla
            for (PeerResponseDto peer : peers) {
                Object[] row = new Object[6];
                row[0] = peer.getPeerId().toString().substring(0, 8) + "..."; // ID corto
                row[1] = peer.getNombreServidor() != null ? peer.getNombreServidor() : "N/A";
                row[2] = peer.getIp();
                row[3] = peer.getPuerto();
                row[4] = peer.getConectado();
                row[5] = peer.getUltimoLatido() != null
                    ? peer.getUltimoLatido().format(DATE_FORMATTER)
                    : "N/A";

                tableModel.addRow(row);
            }

            // Aplicar colores según el estado
            table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value,
                        boolean isSelected, boolean hasFocus, int row, int column) {
                    Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                    if (!isSelected) {
                        String estado = (String) tableModel.getValueAt(row, 4);
                        if ("ONLINE".equalsIgnoreCase(estado)) {
                            c.setBackground(new Color(200, 255, 200)); // Verde claro
                        } else if ("OFFLINE".equalsIgnoreCase(estado)) {
                            c.setBackground(new Color(255, 200, 200)); // Rojo claro
                        } else {
                            c.setBackground(new Color(255, 255, 200)); // Amarillo claro
                        }
                    } else {
                        c.setBackground(table.getSelectionBackground());
                    }

                    return c;
                }
            });

            lblStatus.setText("Total de peers: " + peers.size() + " (" +
                contarPeersActivos(peers) + " activos)");
            lblStatus.setForeground(new Color(0, 128, 0));

        } catch (Exception e) {
            lblStatus.setText("Error al cargar peers: " + e.getMessage());
            lblStatus.setForeground(Color.RED);
            e.printStackTrace();
        }
    }

    /**
     * Cuenta cuántos peers están en estado ONLINE.
     */
    private int contarPeersActivos(List<PeerResponseDto> peers) {
        return (int) peers.stream()
            .filter(p -> "ONLINE".equalsIgnoreCase(p.getConectado()))
            .count();
    }
}

