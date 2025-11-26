package servicio.usuario;

import dto.usuario.DTOActualizarUsuario;
import dto.usuario.DTOCrearUsuario;
import dto.vista.DTOUsuarioVista;
import gestorUsuarios.GestorUsuarios;
import gestorP2P.servicios.ServicioSincronizacionDatos;
import logger.LoggerCentral;

import java.util.List;

/**
 * Servicio de Gestión de Usuarios - Capa de Aplicación
 * Maneja transacciones y orquestación de operaciones complejas relacionadas con usuarios
 * Actúa como intermediario entre el controlador y el gestor de usuarios
 * Integra sincronización P2P después de cada operación de persistencia
 */
public class ServicioGestionUsuarios {

    private static final String TAG = "ServicioGestionUsuarios";
    private final GestorUsuarios gestor;
    private ServicioSincronizacionDatos servicioSincronizacion;

    public ServicioGestionUsuarios(GestorUsuarios gestor) {
        this.gestor = gestor;
        LoggerCentral.info(TAG, "ServicioGestionUsuarios inicializado");
    }

    /**
     * Configura el servicio de sincronización P2P
     * @param servicioSincronizacion Servicio para sincronizar cambios en la red
     */
    public void setServicioSincronizacion(ServicioSincronizacionDatos servicioSincronizacion) {
        this.servicioSincronizacion = servicioSincronizacion;
        LoggerCentral.info(TAG, "✓ Servicio de sincronización P2P configurado");
    }

    /**
     * Crea un nuevo usuario en el sistema y sincroniza con la red P2P
     * @param dto Datos del usuario a crear
     * @return DTO con la información del usuario creado
     * @throws IllegalArgumentException si los datos son inválidos
     */
    public DTOUsuarioVista crearUsuario(DTOCrearUsuario dto) {
        try {
            LoggerCentral.info(TAG, "Procesando creación de usuario: " + dto.getNombre());

            // 1. Persistir usuario a través del gestor
            DTOUsuarioVista resultado = gestor.crearUsuario(dto);
            LoggerCentral.info(TAG, "✓ Usuario creado exitosamente con ID: " + resultado.getId());

            // 2. Sincronizar con la red P2P
            sincronizarConRed("Usuario creado: " + resultado.getNombre());

            return resultado;
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al crear usuario: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Actualiza un usuario existente y sincroniza con la red P2P
     * @param dto Datos del usuario a actualizar
     * @return DTO con la información actualizada
     * @throws IllegalArgumentException si el usuario no existe o los datos son inválidos
     */
    public DTOUsuarioVista actualizarUsuario(DTOActualizarUsuario dto) {
        try {
            LoggerCentral.info(TAG, "Procesando actualización de usuario: " + dto.getId());

            // 1. Actualizar usuario a través del gestor
            DTOUsuarioVista resultado = gestor.actualizarUsuario(dto);
            LoggerCentral.info(TAG, "✓ Usuario actualizado exitosamente");

            // 2. Sincronizar con la red P2P
            sincronizarConRed("Usuario actualizado: " + resultado.getId());

            return resultado;
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al actualizar usuario: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Obtiene la lista completa de usuarios
     * @return Lista de DTOs con información de todos los usuarios
     */
    public List<DTOUsuarioVista> listarUsuarios() {
        try {
            LoggerCentral.debug(TAG, "Obteniendo lista de usuarios");
            List<DTOUsuarioVista> usuarios = gestor.listarUsuarios();
            LoggerCentral.debug(TAG, "Usuarios obtenidos: " + usuarios.size());
            return usuarios;
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al listar usuarios: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Busca un usuario por su ID
     * @param id UUID del usuario
     * @return DTO del usuario encontrado o null si no existe
     */
    public DTOUsuarioVista buscarPorId(String id) {
        try {
            LoggerCentral.debug(TAG, "Buscando usuario por ID: " + id);
            return gestor.buscarPorId(id);
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al buscar usuario por ID: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Busca un usuario por su email
     * @param email Email del usuario
     * @return DTO del usuario encontrado o null si no existe
     */
    public DTOUsuarioVista buscarPorEmail(String email) {
        try {
            LoggerCentral.debug(TAG, "Buscando usuario por email: " + email);
            return gestor.buscarPorEmail(email);
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al buscar usuario por email: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Cambia el estado de un usuario (ONLINE/OFFLINE) y sincroniza con la red
     * @param id UUID del usuario
     * @param estado Nuevo estado ("ONLINE" o "OFFLINE")
     * @return true si se actualizó correctamente
     */
    public boolean cambiarEstado(String id, String estado) {
        try {
            LoggerCentral.info(TAG, "Cambiando estado de usuario " + id + " a " + estado);
            dominio.clienteServidor.Usuario.Estado nuevoEstado =
                dominio.clienteServidor.Usuario.Estado.valueOf(estado.toUpperCase());

            // 1. Cambiar estado a través del gestor
            boolean actualizado = gestor.cambiarEstado(id, nuevoEstado);

            if (actualizado) {
                // 2. Sincronizar con la red P2P
                sincronizarConRed("Estado de usuario cambiado: " + id + " -> " + estado);
            }

            return actualizado;
        } catch (IllegalArgumentException e) {
            LoggerCentral.error(TAG, "Estado inválido: " + estado);
            throw new IllegalArgumentException("Estado inválido. Use 'ONLINE' u 'OFFLINE'");
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al cambiar estado: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Método privado para sincronizar cambios con la red P2P
     * @param descripcion Descripción del cambio realizado
     */
    private void sincronizarConRed(String descripcion) {
        if (servicioSincronizacion != null) {
            LoggerCentral.info(TAG, "🔄 Iniciando sincronización P2P: " + descripcion);
            try {
                servicioSincronizacion.forzarSincronizacion();
                LoggerCentral.info(TAG, "✓ Sincronización P2P activada exitosamente");
            } catch (Exception e) {
                LoggerCentral.error(TAG, "⚠️ Error al sincronizar con red P2P: " + e.getMessage());
                // No lanzamos la excepción para no afectar la operación principal
            }
        } else {
            LoggerCentral.warn(TAG, "⚠️ Servicio de sincronización no configurado. Cambios no sincronizados con red P2P");
        }
    }
}
