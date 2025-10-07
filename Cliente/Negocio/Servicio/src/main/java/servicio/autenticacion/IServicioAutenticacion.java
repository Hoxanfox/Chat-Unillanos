package servicio.autenticacion;

import dto.vistaLogin.DTOAutenticacion;

/**
 * Contrato para el servicio de autenticación.
 * Actúa como intermediario entre el controlador y la fachada.
 */
public interface IServicioAutenticacion {
    /**
     * Delega la solicitud de autenticación a la fachada.
     * @param datos Los datos de autenticación del usuario.
     * @return true si la autenticación es exitosa, false en caso contrario.
     */
    boolean autenticar(DTOAutenticacion datos);
}
