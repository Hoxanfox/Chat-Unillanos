package controlador.autenticacion;

import dto.vistaLogin.DTOAutenticacion;

/**
 * Contrato para el controlador de autenticación.
 * Define las operaciones que la vista puede solicitar.
 */
public interface IControladorAutenticacion {
    /**
     * Procesa la solicitud de autenticación.
     * @param datos DTO con el usuario y la contraseña.
     * @return true si la autenticación es exitosa, false en caso contrario.
     */
    boolean autenticar(DTOAutenticacion datos);
}
