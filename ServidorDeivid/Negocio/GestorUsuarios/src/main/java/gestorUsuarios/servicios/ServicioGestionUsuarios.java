package gestorUsuarios.servicios;

import dto.usuario.DTOActualizarUsuario;
import dto.usuario.DTOCrearUsuario;
import dto.vista.DTOUsuarioVista;
import gestorUsuarios.GestorUsuarios;
import logger.LoggerCentral;

import java.util.List;

/**
 * Servicio de Gestión de Usuarios
 * Capa intermedia entre el controlador y el gestor
 * Maneja transacciones y orquestación de operaciones complejas
 */
public class ServicioGestionUsuarios {

    private static final String TAG = "ServicioGestionUsuarios";
    private final GestorUsuarios gestor;

    public ServicioGestionUsuarios(GestorUsuarios gestor) {
        this.gestor = gestor;
        LoggerCentral.info(TAG, "ServicioGestionUsuarios inicializado");
    }

    /**
     * Crea un nuevo usuario en el sistema
     * @param dto Datos del usuario a crear
     * @return DTO con la información del usuario creado
     * @throws IllegalArgumentException si los datos son inválidos
     */
    public DTOUsuarioVista crearUsuario(DTOCrearUsuario dto) {
        try {
            LoggerCentral.info(TAG, "Procesando creación de usuario: " + dto.getNombre());
            DTOUsuarioVista resultado = gestor.crearUsuario(dto);
            LoggerCentral.info(TAG, "Usuario creado exitosamente con ID: " + resultado.getId());
            return resultado;
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al crear usuario: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Actualiza un usuario existente
     * @param dto Datos del usuario a actualizar
     * @return DTO con la información actualizada
     * @throws IllegalArgumentException si el usuario no existe o los datos son inválidos
     */
    public DTOUsuarioVista actualizarUsuario(DTOActualizarUsuario dto) {
        try {
            LoggerCentral.info(TAG, "Procesando actualización de usuario: " + dto.getId());
            DTOUsuarioVista resultado = gestor.actualizarUsuario(dto);
            LoggerCentral.info(TAG, "Usuario actualizado exitosamente");
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
     * Cambia el estado de un usuario (ONLINE/OFFLINE)
     * @param id UUID del usuario
     * @param estado Nuevo estado ("ONLINE" o "OFFLINE")
     * @return true si se actualizó correctamente
     */
    public boolean cambiarEstado(String id, String estado) {
        try {
            LoggerCentral.info(TAG, "Cambiando estado de usuario " + id + " a " + estado);
            dominio.clienteServidor.Usuario.Estado nuevoEstado =
                dominio.clienteServidor.Usuario.Estado.valueOf(estado.toUpperCase());
            return gestor.cambiarEstado(id, nuevoEstado);
        } catch (IllegalArgumentException e) {
            LoggerCentral.error(TAG, "Estado inválido: " + estado);
            throw new IllegalArgumentException("Estado inválido. Use 'ONLINE' u 'OFFLINE'");
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al cambiar estado: " + e.getMessage());
            throw e;
        }
    }
}


