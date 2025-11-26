package interfazGrafica.vistaUsuarios.componentes;

import javax.swing.*;
import java.awt.*;

/**
 * Diálogo modal para agregar o editar un usuario
 */
public class DialogoUsuario extends JDialog {

    private JTextField txtUsername;
    private JTextField txtEmail;
    private JPasswordField txtPassword;
    private JComboBox<String> cmbStatus;
    private JButton btnGuardar;
    private JButton btnCancelar;

    private boolean confirmado = false;
    private boolean esEdicion = false;

    public DialogoUsuario(Frame parent, boolean esEdicion) {
        super(parent, esEdicion ? "Edit User" : "Add User", true);
        this.esEdicion = esEdicion;
        configurarDialogo();
        inicializarComponentes();
    }

    private void configurarDialogo() {
        this.setSize(400, 300);
        this.setLocationRelativeTo(getParent());
        this.setResizable(false);
        this.setLayout(new BorderLayout(10, 10));
    }

    private void inicializarComponentes() {
        // Panel central con formulario
        JPanel panelFormulario = crearPanelFormulario();
        this.add(panelFormulario, BorderLayout.CENTER);

        // Panel inferior con botones
        JPanel panelBotones = crearPanelBotones();
        this.add(panelBotones, BorderLayout.SOUTH);

        // Padding
        ((JPanel)this.getContentPane()).setBorder(
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        );
    }

    private JPanel crearPanelFormulario() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Username
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Username:"), gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        txtUsername = new JTextField(20);
        panel.add(txtUsername, gbc);

        // Email
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        panel.add(new JLabel("Email:"), gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        txtEmail = new JTextField(20);
        panel.add(txtEmail, gbc);

        // Password (solo visible en modo creación)
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        JLabel lblPassword = new JLabel("Password:");
        panel.add(lblPassword, gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        txtPassword = new JPasswordField(20);
        panel.add(txtPassword, gbc);

        // En modo edición, ocultar el campo de contraseña
        if (esEdicion) {
            lblPassword.setVisible(false);
            txtPassword.setVisible(false);
        }

        // Status
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        panel.add(new JLabel("Status:"), gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        cmbStatus = new JComboBox<>(new String[]{"Active", "Inactive"});
        panel.add(cmbStatus, gbc);

        return panel;
    }

    private JPanel crearPanelBotones() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));

        btnGuardar = new JButton(esEdicion ? "Update" : "Save");
        btnCancelar = new JButton("Cancel");

        btnGuardar.setPreferredSize(new Dimension(100, 30));
        btnCancelar.setPreferredSize(new Dimension(100, 30));

        // Listeners
        btnGuardar.addActionListener(e -> guardar());
        btnCancelar.addActionListener(e -> cancelar());

        panel.add(btnCancelar);
        panel.add(btnGuardar);

        return panel;
    }

    private void guardar() {
        // Validaciones básicas
        if (txtUsername.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "El username no puede estar vacío",
                "Validation Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (txtEmail.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "El email no puede estar vacío",
                "Validation Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Validación básica de email
        if (!txtEmail.getText().contains("@")) {
            JOptionPane.showMessageDialog(this,
                "El email no es válido",
                "Validation Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        // En modo creación, validar contraseña
        if (!esEdicion) {
            String password = new String(txtPassword.getPassword());
            if (password.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "La contraseña no puede estar vacía",
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (password.length() < 6) {
                JOptionPane.showMessageDialog(this,
                    "La contraseña debe tener al menos 6 caracteres",
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        confirmado = true;
        this.dispose();
    }

    private void cancelar() {
        confirmado = false;
        this.dispose();
    }

    // Métodos públicos para obtener/establecer datos
    public void setDatosUsuario(String username, String email, String status) {
        txtUsername.setText(username);
        txtEmail.setText(email);
        cmbStatus.setSelectedItem(status);
    }

    public String getUsername() {
        return txtUsername.getText().trim();
    }

    public String getEmail() {
        return txtEmail.getText().trim();
    }

    public String getPassword() {
        return new String(txtPassword.getPassword()).trim();
    }

    public String getStatus() {
        return (String) cmbStatus.getSelectedItem();
    }

    public boolean isConfirmado() {
        return confirmado;
    }

    public boolean isEsEdicion() {
        return esEdicion;
    }
}
