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
import observador.IObservador;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;

/**
 * Panel principal que orquesta los componentes de la vista de usuarios
 * Ahora integrado con el controlador para usar la arquitectura completa
 * ✅ NUEVO: Implementa IObservador para recibir notificaciones de cambios
 */
public class PanelUsuarios extends JPanel implements IObservador {

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

            // ✅ ARQUITECTURA CORRECTA: Vista -> Controlador -> Servicio -> Gestor -> Repositorio
            String fileIdFoto = null;
            if (archivoFoto != null) {
                LoggerCentral.info(TAG, "📸 Procesando foto de perfil para usuario: " + username);

                // 1. Guardar archivo FÍSICO (solo en Bucket/)
                GestorArchivosLocal.ArchivoInfo archivoInfo = GestorArchivosLocal.guardarFotoUsuario(archivoFoto);

                if (archivoInfo == null) {
                    JOptionPane.showMessageDialog(this,
                        "Error al guardar la foto de perfil. El usuario se creará sin foto.",
                        "Advertencia",
                        JOptionPane.WARNING_MESSAGE);
                } else {
                    LoggerCentral.info(TAG, "✓ Archivo físico guardado: " + archivoInfo.fileId);

                    // 2. Registrar archivo en BD usando el CONTROLADOR (respeta arquitectura)
                    boolean registrado = controlador.registrarArchivo(
                        archivoInfo.fileId,
                        archivoInfo.nombreOriginal,
                        archivoInfo.mimeType,
                        archivoInfo.tamanio,
                        archivoInfo.hash
                    );

                    if (registrado) {
                        fileIdFoto = archivoInfo.fileId;
                        LoggerCentral.info(TAG, "✅ Foto registrada completamente (físico + BD): " + fileIdFoto);
                    } else {
                        LoggerCentral.error(TAG, "❌ Error al registrar archivo en BD, eliminando archivo físico...");
                        GestorArchivosLocal.eliminarFotoUsuario(archivoInfo.fileId);
                        JOptionPane.showMessageDialog(this,
                            "Error al registrar la foto en la base de datos. El usuario se creará sin foto.",
                            "Advertencia",
                            JOptionPane.WARNING_MESSAGE);
                    }
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

            // ✅ ARQUITECTURA CORRECTA: Si se seleccionó una nueva foto, procesarla
            if (archivoFoto != null) {
                LoggerCentral.info(TAG, "📸 Actualizando foto de perfil para usuario: " + usuario[0]);

                // 1. Guardar archivo FÍSICO
                GestorArchivosLocal.ArchivoInfo archivoInfo = GestorArchivosLocal.guardarFotoUsuario(archivoFoto);

                if (archivoInfo == null) {
                    JOptionPane.showMessageDialog(this,
                        "Error al guardar la nueva foto. El usuario se actualizará sin cambiar la foto.",
                        "Advertencia",
                        JOptionPane.WARNING_MESSAGE);
                } else {
                    LoggerCentral.info(TAG, "✓ Archivo físico guardado: " + archivoInfo.fileId);

                    // 2. Registrar archivo en BD usando el CONTROLADOR
                    boolean registrado = controlador.registrarArchivo(
                        archivoInfo.fileId,
                        archivoInfo.nombreOriginal,
                        archivoInfo.mimeType,
                        archivoInfo.tamanio,
                        archivoInfo.hash
                    );

                    if (registrado) {
                        fileIdFoto = archivoInfo.fileId;
                        LoggerCentral.info(TAG, "✅ Nueva foto registrada completamente: " + fileIdFoto);
                    } else {
                        LoggerCentral.error(TAG, "❌ Error al registrar archivo en BD, eliminando archivo físico...");
                        GestorArchivosLocal.eliminarFotoUsuario(archivoInfo.fileId);
                        JOptionPane.showMessageDialog(this,
                            "Error al registrar la foto en la base de datos. El usuario se actualizará sin cambiar la foto.",
                            "Advertencia",
                            JOptionPane.WARNING_MESSAGE);
                    }
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

    @Override
    public void actualizar(String tipo, Object datos) {
        LoggerCentral.debug(TAG, "📢 Evento recibido: " + tipo + " | Datos: " + datos);

        switch (tipo) {
            case "USUARIO_AUTENTICADO":
            case "USUARIO_ONLINE":
            case "USUARIO_OFFLINE":
            case "USUARIO_DESCONECTADO":
            case "CLIENTE_OFFLINE":
            case "CLIENTE_CONECTADO":
            case "CLIENTE_DESCONECTADO":
                // Cuando un usuario cambia de estado o se conecta/desconecta, refrescar la tabla
                SwingUtilities.invokeLater(() -> {
                    LoggerCentral.info(TAG, "🔄 Refrescando tabla de usuarios por evento: " + tipo);
                    refrescarTabla();
                });
                break;

            case "USUARIO_CREADO":
            case "USUARIO_ACTUALIZADO":
            case "USUARIO_ELIMINADO":
                // Actualizar tabla cuando se modifica desde la UI
                SwingUtilities.invokeLater(() -> {
                    LoggerCentral.info(TAG, "🔄 Refrescando tabla por modificación: " + tipo);
                    refrescarTabla();
                });
                break;

            // ✅ NUEVO: Escuchar evento específico de sincronización de usuarios
            case "SINCRONIZADO_USUARIO":
                SwingUtilities.invokeLater(() -> {
                    LoggerCentral.info(TAG, "🔄 ✅ Usuarios sincronizados. Refrescando tabla...");
                    refrescarTabla();
                });
                break;

            // ✅ NUEVO: Escuchar eventos de sincronización P2P
            case "SINCRONIZACION_TERMINADA":
            case "SINCRONIZACION_P2P_TERMINADA":  // ✅ AGREGADO: También escuchar el evento P2P específico
                // Cuando termina la sincronización P2P, refrescar para mostrar usuarios sincronizados
                SwingUtilities.invokeLater(() -> {
                    LoggerCentral.info(TAG, "🔄 Refrescando tabla por sincronización P2P terminada");
                    refrescarTabla();
                });
                break;

            default:
                LoggerCentral.debug(TAG, "Evento no manejado en PanelUsuarios: " + tipo);
        }
    }

    // Getters para acceder a los componentes si es necesario
    public TablaUsuarios getTablaUsuarios() {
        return tablaUsuarios;
    }

    public BarraHerramientasUsuarios getBarraHerramientas() {
        return barraHerramientas;
    }
}
