package interfazGrafica.vistaUsuarios;

import controlador.usuarios.ControladorUsuarios;
import dto.usuario.DTOActualizarUsuario;
import dto.usuario.DTOCrearUsuario;
import dto.vista.DTOUsuarioVista;
import interfazGrafica.vistaUsuarios.componentes.BarraHerramientasUsuarios;
import interfazGrafica.vistaUsuarios.componentes.DialogoUsuario;
import interfazGrafica.vistaUsuarios.componentes.TablaUsuarios;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Panel principal que orquesta los componentes de la vista de usuarios
 * Ahora integrado con el controlador para usar la arquitectura completa
 */
public class PanelUsuarios extends JPanel {

    private BarraHerramientasUsuarios barraHerramientas;
    private TablaUsuarios tablaUsuarios;
    private ControladorUsuarios controlador;

    public PanelUsuarios(ControladorUsuarios controlador) {
        this.controlador = controlador;
        configurarPanel();
        inicializarComponentes();
        configurarEventos();
        cargarUsuariosDesdeBaseDatos();
    }

    private void configurarPanel() {
        this.setLayout(new BorderLayout(10, 10));
        this.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
    }

    private void inicializarComponentes() {
        // Instanciar componentes
        barraHerramientas = new BarraHerramientasUsuarios();
        tablaUsuarios = new TablaUsuarios();

        // Agregar al panel
        this.add(barraHerramientas, BorderLayout.NORTH);
        this.add(tablaUsuarios, BorderLayout.CENTER);
    }

    private void configurarEventos() {
        // Configurar listeners usando los métodos de los componentes
        barraHerramientas.setAddActionListener(e -> agregarUsuario());
        barraHerramientas.setDeleteActionListener(e -> eliminarUsuario());
        barraHerramientas.setEditActionListener(e -> editarUsuario());
    }

    private void agregarUsuario() {
        // Obtener el Frame padre para el diálogo modal
        Frame frame = (Frame) SwingUtilities.getWindowAncestor(this);
        DialogoUsuario dialogo = new DialogoUsuario(frame, false);
        dialogo.setVisible(true);

        // Si el usuario confirmó
        if (dialogo.isConfirmado()) {
            String username = dialogo.getUsername();
            String email = dialogo.getEmail();
            String password = dialogo.getPassword();
            String status = dialogo.getStatus();

            // Crear DTO para enviar al controlador
            DTOCrearUsuario dto = new DTOCrearUsuario();
            dto.setNombre(username);
            dto.setEmail(email);
            dto.setContrasena(password); // Usar la contraseña del formulario
            // TODO: Obtener el peerPadreId del servidor actual
            dto.setPeerPadreId(null); // Por ahora null

            // Llamar al controlador para crear el usuario
            DTOUsuarioVista usuarioCreado = controlador.crearUsuario(dto);

            if (usuarioCreado != null) {
                // Agregar a la tabla
                Object[] nuevoUsuario = {
                    usuarioCreado.getId(),
                    usuarioCreado.getNombre(),
                    usuarioCreado.getEmail(),
                    usuarioCreado.getEstado(),
                    usuarioCreado.getFechaCreacion(),
                    usuarioCreado.getPeerPadreId() != null ? usuarioCreado.getPeerPadreId() : "N/A"
                };

                tablaUsuarios.agregarUsuario(nuevoUsuario);
            }
        }
    }

    private void eliminarUsuario() {
        // Verificar si hay una fila seleccionada
        if (!tablaUsuarios.hayFilaSeleccionada()) {
            JOptionPane.showMessageDialog(this,
                "Por favor seleccione un usuario para eliminar",
                "Delete User",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Obtener datos del usuario seleccionado
        Object[] usuario = tablaUsuarios.obtenerUsuarioSeleccionado();
        String username = (String) usuario[1];
        String id = (String) usuario[0];

        // Confirmación
        int confirmacion = JOptionPane.showConfirmDialog(this,
            "¿Está seguro de eliminar el usuario '" + username + "'?",
            "Confirmar eliminación",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (confirmacion == JOptionPane.YES_OPTION) {
            // Llamar al controlador para eliminar (cambiar estado a OFFLINE)
            boolean eliminado = controlador.eliminarUsuario(id);

            if (eliminado) {
                tablaUsuarios.eliminarUsuarioSeleccionado();
            }
        }
    }

    private void editarUsuario() {
        // Verificar si hay una fila seleccionada
        if (!tablaUsuarios.hayFilaSeleccionada()) {
            JOptionPane.showMessageDialog(this,
                "Por favor seleccione un usuario para editar",
                "Edit User",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Obtener datos del usuario seleccionado
        int filaSeleccionada = tablaUsuarios.getFilaSeleccionada();
        Object[] usuario = tablaUsuarios.obtenerUsuarioSeleccionado();

        // Crear diálogo en modo edición
        Frame frame = (Frame) SwingUtilities.getWindowAncestor(this);
        DialogoUsuario dialogo = new DialogoUsuario(frame, true);

        // Pre-cargar los datos actuales
        dialogo.setDatosUsuario(
            (String) usuario[1], // username
            (String) usuario[2], // email
            (String) usuario[3]  // status
        );

        dialogo.setVisible(true);

        // Si el usuario confirmó
        if (dialogo.isConfirmado()) {
            // Crear DTO para actualizar
            DTOActualizarUsuario dto = new DTOActualizarUsuario();
            dto.setId((String) usuario[0]); // ID
            dto.setNombre(dialogo.getUsername());
            dto.setEmail(dialogo.getEmail());
            dto.setEstado(dialogo.getStatus().toUpperCase());

            // Llamar al controlador para actualizar
            DTOUsuarioVista usuarioActualizado = controlador.actualizarUsuario(dto);

            if (usuarioActualizado != null) {
                // Actualizar los datos en la tabla
                Object[] datosActualizados = {
                    usuarioActualizado.getId(),
                    usuarioActualizado.getNombre(),
                    usuarioActualizado.getEmail(),
                    usuarioActualizado.getEstado(),
                    usuarioActualizado.getFechaCreacion(),
                    usuarioActualizado.getPeerPadreId() != null ? usuarioActualizado.getPeerPadreId() : "N/A"
                };

                tablaUsuarios.actualizarUsuario(filaSeleccionada, datosActualizados);
            }
        }
    }

    /**
     * Carga los usuarios desde la base de datos usando el controlador
     */
    private void cargarUsuariosDesdeBaseDatos() {
        try {
            // Limpiar tabla antes de cargar
            tablaUsuarios.limpiarTabla();

            // Obtener usuarios desde el controlador
            List<DTOUsuarioVista> usuarios = controlador.listarUsuarios();

            // Agregar cada usuario a la tabla
            for (DTOUsuarioVista u : usuarios) {
                Object[] fila = {
                    u.getId(),
                    u.getNombre(),
                    u.getEmail(),
                    u.getEstado(),
                    u.getFechaCreacion(),
                    u.getPeerPadreId() != null ? u.getPeerPadreId() : "N/A"
                };
                tablaUsuarios.agregarUsuario(fila);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error al cargar usuarios: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Refresca la tabla con los datos actuales de la base de datos
     */
    public void refrescarTabla() {
        cargarUsuariosDesdeBaseDatos();
    }

    // Getters para acceder a los componentes si es necesario
    public TablaUsuarios getTablaUsuarios() {
        return tablaUsuarios;
    }

    public BarraHerramientasUsuarios getBarraHerramientas() {
        return barraHerramientas;
    }
}
