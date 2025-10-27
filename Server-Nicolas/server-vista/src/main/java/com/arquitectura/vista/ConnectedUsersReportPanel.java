package com.arquitectura.vista;

import com.arquitectura.DTO.usuarios.UserResponseDto;
import com.arquitectura.controlador.ServerViewController;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class ConnectedUsersReportPanel extends JPanel {

    private final ServerViewController controller;
    private JTable usersTable;
    private DefaultTableModel tableModel;

    public ConnectedUsersReportPanel(ServerViewController controller) {
        this.controller = controller;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Título del panel
        JLabel title = new JLabel("Informe de Usuarios Conectados", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 16));
        add(title, BorderLayout.NORTH);

        // Modelo de la tabla (no editable)
        tableModel = new DefaultTableModel(new String[]{"ID", "Username", "Email"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        usersTable = new JTable(tableModel);
        usersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(usersTable), BorderLayout.CENTER);

        // Panel de botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnDisconnect = new JButton("Desconectar Usuario Seleccionado");
        buttonPanel.add(btnDisconnect);
        add(buttonPanel, BorderLayout.SOUTH);

        // Listener para el botón de desconectar
        btnDisconnect.addActionListener(e -> disconnectSelectedUser());
    }

    private void disconnectSelectedUser() {
        int selectedRow = usersTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Por favor, seleccione un usuario de la tabla.", "Ningún usuario seleccionado", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Obtenemos el ID de la primera columna de la fila seleccionada
        Integer userId = (Integer) tableModel.getValueAt(selectedRow, 0);
        String username = (String) tableModel.getValueAt(selectedRow, 1);

        int confirmation = JOptionPane.showConfirmDialog(this,
                "¿Está seguro de que desea desconectar a '" + username + "' (ID: " + userId + ")?",
                "Confirmar Desconexión",
                JOptionPane.YES_NO_OPTION);

        if (confirmation == JOptionPane.YES_OPTION) {
            controller.disconnectUser(userId);
            JOptionPane.showMessageDialog(this, "Se ha enviado la orden de desconexión para el usuario " + username + ".", "Desconexión Forzada", JOptionPane.INFORMATION_MESSAGE);
            // Refrescamos la lista para ver el cambio
            refreshReport();
        }
    }

    public void refreshReport() {
        // Limpiar la tabla antes de llenarla
        tableModel.setRowCount(0);

        List<UserResponseDto> users = controller.obtenerUsuariosConectados();

        if (users != null && !users.isEmpty()) {
            for (UserResponseDto user : users) {
                tableModel.addRow(new Object[]{user.getUserId(), user.getUsername(), user.getEmail()});
            }
        }
    }
}