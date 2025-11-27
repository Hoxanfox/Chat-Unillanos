package interfazGrafica.vistaUsuarios;

import controlador.usuarios.ControladorUsuarios;
import dto.usuario.DTOActualizarUsuario;
import dto.usuario.DTOCrearUsuario;
import dto.vista.DTOUsuarioVista;
import interfazGrafica.vistaUsuarios.componentes.BarraHerramientasUsuarios;
import interfazGrafica.vistaUsuarios.componentes.DialogoUsuario;
import interfazGrafica.vistaUsuarios.componentes.TablaUsuarios;
import interfazGrafica.util.GestorArchivosLocal;
import logger.LoggerCentral;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;

/**
 * Panel principal que orquesta los componentes de la vista de usuarios
 * Ahora integrado con el controlador para usar la arquitectura completa
 */
public class PanelUsuarios extends JPanel {

    private static final String TAG = "PanelUsuarios";
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
            File archivoFoto = dialogo.getArchivoFotoSeleccionado();

            // Guardar foto en Bucket si fue seleccionada
            String fileIdFoto = null;
            if (archivoFoto != null) {
                LoggerCentral.info(TAG, "Guardando foto de perfil para usuario: " + username);
                fileIdFoto = GestorArchivosLocal.guardarFotoUsuario(archivoFoto);

                if (fileIdFoto == null) {
                    JOptionPane.showMessageDialog(this,
                        "Error al guardar la foto de perfil. El usuario se creará sin foto.",
                        "Advertencia",
                        JOptionPane.WARNING_MESSAGE);
                } else {
                    LoggerCentral.info(TAG, "✓ Foto guardada con ID: " + fileIdFoto);
                }
            }

            // Crear DTO para enviar al controlador
            DTOCrearUsuario dto = new DTOCrearUsuario();
            dto.setNombre(username);
            dto.setEmail(email);
            dto.setContrasena(password);
            dto.setFoto(fileIdFoto); // Guardar el fileId de la foto
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
                LoggerCentral.info(TAG, "✅ Usuario creado exitosamente con foto: " +
                    (fileIdFoto != null ? fileIdFoto : "sin foto"));
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
            File archivoFoto = dialogo.getArchivoFotoSeleccionado();
            String fileIdFoto = null;

            // Si se seleccionó una nueva foto, guardarla
            if (archivoFoto != null) {
                LoggerCentral.info(TAG, "Actualizando foto de perfil para usuario: " + usuario[0]);
                fileIdFoto = GestorArchivosLocal.guardarFotoUsuario(archivoFoto);

                if (fileIdFoto == null) {
                    JOptionPane.showMessageDialog(this,
                        "Error al guardar la nueva foto. El usuario se actualizará sin cambiar la foto.",
                        "Advertencia",
                        JOptionPane.WARNING_MESSAGE);
                } else {
                    LoggerCentral.info(TAG, "✓ Nueva foto guardada con ID: " + fileIdFoto);
                }
            }

            // Crear DTO para actualizar
            DTOActualizarUsuario dto = new DTOActualizarUsuario();
            dto.setId((String) usuario[0]); // ID
            dto.setNombre(dialogo.getUsername());
            dto.setEmail(dialogo.getEmail());
            dto.setEstado(dialogo.getStatus().toUpperCase());

            // Solo actualizar foto si se seleccionó una nueva
            if (fileIdFoto != null) {
                dto.setFoto(fileIdFoto);
            }

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
                LoggerCentral.info(TAG, "✅ Usuario actualizado exitosamente" +
                    (fileIdFoto != null ? " con nueva foto: " + fileIdFoto : ""));
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
