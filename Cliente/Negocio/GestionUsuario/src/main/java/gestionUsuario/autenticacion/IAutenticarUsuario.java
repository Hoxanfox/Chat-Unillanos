package gestionUsuario.autenticacion;

import dto.vistaLogin.DTOAutenticacion;

/**
 * Contrato para el componente que maneja la lógica de negocio específica
 * para la autenticación de usuarios. Es utilizado por la Fachada.
 */
public interface IAutenticarUsuario {

    /**
     * Valida las credenciales de un usuario.
     * En una implementación real, aquí se interactuaría con el servidor o la base de datos.
     * @param dto Los datos de autenticación (email y contraseña).
     * @return true si las credenciales son válidas, false en caso contrario.
     */
    boolean autenticar(DTOAutenticacion dto);
}
