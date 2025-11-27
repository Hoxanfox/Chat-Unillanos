package controlador.usuarios;

import dto.usuario.DTOActualizarUsuario;
import dto.usuario.DTOCrearUsuario;
import dto.vista.DTOUsuarioVista;
import logger.LoggerCentral;
import servicio.usuario.ServicioGestionUsuarios;

import javax.swing.*;
import java.util.List;

/**
 * Controlador para la gestión de usuarios desde la interfaz gráfica
 * Coordina las acciones entre la vista (PanelUsuarios) y el servicio de negocio
 * Pertenece a la capa de Presentación
 */
public class ControladorUsuarios {

    private static final String TAG = "ControladorUsuarios";
    private final ServicioGestionUsuarios servicio;

    public ControladorUsuarios(ServicioGestionUsuarios servicio) {
        this.servicio = servicio;
        LoggerCentral.info(TAG, "ControladorUsuarios inicializado");
    }

    /**
     * Crea un nuevo usuario en el sistema
     * @param dto Datos del usuario a crear
     * @return DTO del usuario creado o null si hubo error
     */
    public DTOUsuarioVista crearUsuario(DTOCrearUsuario dto) {
        try {
            LoggerCentral.info(TAG, "Solicitud de creación de usuario: " + dto.getNombre());

            // Validaciones previas en el controlador
            if (dto.getNombre() == null || dto.getNombre().trim().isEmpty()) {
                mostrarError("El nombre del usuario es obligatorio");
                return null;
            }

            if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
                mostrarError("El email del usuario es obligatorio");
                return null;
            }

            if (dto.getContrasena() == null || dto.getContrasena().trim().isEmpty()) {
                mostrarError("La contraseña es obligatoria");
                return null;
            }

            // Llamar al servicio
            DTOUsuarioVista resultado = servicio.crearUsuario(dto);

            LoggerCentral.info(TAG, "Usuario creado exitosamente con ID: " + resultado.getId());
            mostrarExito("Usuario creado exitosamente");

            return resultado;

        } catch (IllegalArgumentException e) {
            LoggerCentral.warn(TAG, "Error de validación al crear usuario: " + e.getMessage());
            mostrarError(e.getMessage());
            return null;
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error inesperado al crear usuario: " + e.getMessage());
            mostrarError("Error al crear usuario: " + e.getMessage());
            return null;
        }
    }

    /**
     * Actualiza un usuario existente
     * @param dto Datos del usuario a actualizar
     * @return DTO del usuario actualizado o null si hubo error
     */
    public DTOUsuarioVista actualizarUsuario(DTOActualizarUsuario dto) {
        try {
            LoggerCentral.info(TAG, "Solicitud de actualización de usuario: " + dto.getId());

            if (dto.getId() == null || dto.getId().trim().isEmpty()) {
                mostrarError("El ID del usuario es obligatorio");
                return null;
            }

            // Llamar al servicio
            DTOUsuarioVista resultado = servicio.actualizarUsuario(dto);

            LoggerCentral.info(TAG, "Usuario actualizado exitosamente");
            mostrarExito("Usuario actualizado exitosamente");

            return resultado;

        } catch (IllegalArgumentException e) {
            LoggerCentral.warn(TAG, "Error de validación al actualizar usuario: " + e.getMessage());
            mostrarError(e.getMessage());
            return null;
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error inesperado al actualizar usuario: " + e.getMessage());
            mostrarError("Error al actualizar usuario: " + e.getMessage());
            return null;
        }
    }

    /**
     * Obtiene la lista completa de usuarios
     * @return Lista de DTOs con información de usuarios
     */
    public List<DTOUsuarioVista> listarUsuarios() {
        try {
            LoggerCentral.debug(TAG, "Obteniendo lista de usuarios");
            List<DTOUsuarioVista> usuarios = servicio.listarUsuarios();
            LoggerCentral.debug(TAG, "Total usuarios: " + usuarios.size());
            return usuarios;
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al listar usuarios: " + e.getMessage());
            mostrarError("Error al obtener la lista de usuarios");
            return List.of(); // Retornar lista vacía en caso de error
        }
    }

    /**
     * Busca un usuario por su ID
     * @param id UUID del usuario
     * @return DTO del usuario o null si no existe
     */
    public DTOUsuarioVista buscarPorId(String id) {
        try {
            LoggerCentral.debug(TAG, "Buscando usuario por ID: " + id);
            DTOUsuarioVista usuario = servicio.buscarPorId(id);

            if (usuario == null) {
                LoggerCentral.warn(TAG, "Usuario no encontrado con ID: " + id);
                mostrarAdvertencia("Usuario no encontrado");
            }

            return usuario;
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al buscar usuario: " + e.getMessage());
            mostrarError("Error al buscar usuario");
            return null;
        }
    }

    /**
     * Cambia el estado de un usuario
     * @param id UUID del usuario
     * @param estado Nuevo estado ("ONLINE" o "OFFLINE")
     * @return true si se actualizó correctamente
     */
    public boolean cambiarEstado(String id, String estado) {
        try {
            LoggerCentral.info(TAG, "Cambiando estado de usuario " + id + " a " + estado);
            boolean resultado = servicio.cambiarEstado(id, estado);

            if (resultado) {
                LoggerCentral.info(TAG, "Estado actualizado exitosamente");
            } else {
                LoggerCentral.warn(TAG, "No se pudo actualizar el estado");
                mostrarAdvertencia("No se pudo actualizar el estado del usuario");
            }

            return resultado;
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al cambiar estado: " + e.getMessage());
            mostrarError("Error al cambiar estado del usuario");
            return false;
        }
    }

    /**
     * Elimina un usuario del sistema (eliminación lógica - solo cambia estado)
     * ⚠️ NO SE ELIMINA DE LA BASE DE DATOS, solo se marca como NO DISPONIBLE
     * @param id UUID del usuario a eliminar
     * @return true si se actualizó correctamente
     */
    public boolean eliminarUsuario(String id) {
        try {
            LoggerCentral.warn(TAG, "Intento de desactivación de usuario: " + id);

            // Confirmación de eliminación
            int confirmacion = JOptionPane.showConfirmDialog(
                null,
                "¿Está seguro de desactivar este usuario?\n" +
                "El usuario NO será eliminado de la base de datos,\n" +
                "solo se marcará como NO DISPONIBLE (OFFLINE).",
                "Confirmar desactivación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );

            if (confirmacion != JOptionPane.YES_OPTION) {
                LoggerCentral.info(TAG, "Desactivación cancelada por el usuario");
                return false;
            }

            // Cambiar estado a OFFLINE (eliminación lógica, NO física)
            boolean resultado = servicio.cambiarEstado(id, "OFFLINE");

            if (resultado) {
                LoggerCentral.info(TAG, "✓ Usuario marcado como OFFLINE (desactivado)");
                LoggerCentral.info(TAG, "ℹ️ El usuario permanece en la base de datos");
                mostrarExito("Usuario desactivado exitosamente.\nEl usuario permanece en la base de datos.");
            }

            return resultado;

        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al desactivar usuario: " + e.getMessage());
            mostrarError("Error al desactivar usuario");
            return false;
        }
    }

    /**
     * ✅ NUEVO: Registra un archivo en la base de datos
     * Este método respeta la arquitectura: Controlador -> Servicio -> Gestor -> Repositorio
     * @param fileId ID del archivo (ruta relativa desde Bucket/)
     * @param nombreOriginal Nombre original del archivo
     * @param mimeType Tipo MIME del archivo
     * @param tamanio Tamaño en bytes
     * @param hash Hash SHA-256 del archivo
     * @return true si se registró correctamente
     */
    public boolean registrarArchivo(String fileId, String nombreOriginal, String mimeType, long tamanio, String hash) {
        try {
            LoggerCentral.info(TAG, "📝 Registrando archivo en BD: " + fileId);

            // Llamar al servicio para registrar el archivo
            boolean resultado = servicio.registrarArchivo(fileId, nombreOriginal, mimeType, tamanio, hash);

            if (resultado) {
                LoggerCentral.info(TAG, "✅ Archivo registrado en BD exitosamente: " + fileId);
            } else {
                LoggerCentral.error(TAG, "❌ Error al registrar archivo en BD: " + fileId);
                mostrarError("No se pudo registrar el archivo en la base de datos");
            }

            return resultado;

        } catch (Exception e) {
            LoggerCentral.error(TAG, "❌ Error inesperado al registrar archivo: " + e.getMessage());
            e.printStackTrace();
            mostrarError("Error al registrar archivo en la base de datos");
            return false;
        }
    }

    // --- Métodos auxiliares para mostrar mensajes ---

    private void mostrarError(String mensaje) {
        SwingUtilities.invokeLater(() ->
            JOptionPane.showMessageDialog(
                null,
                mensaje,
                "Error",
                JOptionPane.ERROR_MESSAGE
            )
        );
    }

    private void mostrarExito(String mensaje) {
        SwingUtilities.invokeLater(() ->
            JOptionPane.showMessageDialog(
                null,
                mensaje,
                "Éxito",
                JOptionPane.INFORMATION_MESSAGE
            )
        );
    }

    private void mostrarAdvertencia(String mensaje) {
        SwingUtilities.invokeLater(() ->
            JOptionPane.showMessageDialog(
                null,
                mensaje,
                "Advertencia",
                JOptionPane.WARNING_MESSAGE
            )
        );
    }
}
