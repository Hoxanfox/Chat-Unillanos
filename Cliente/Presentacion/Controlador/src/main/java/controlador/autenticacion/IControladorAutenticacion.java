package controlador.autenticacion;

import dto.vistaLogin.DTOAutenticacion;
import java.util.concurrent.CompletableFuture;

/**
 * Contrato para el controlador de autenticación.
 */
public interface IControladorAutenticacion {
    /**
     * Inicia el proceso de autenticación de forma asíncrona.
     * @param datos Los datos de autenticación del usuario.
     * @return Un Future que se completará con 'true' si la autenticación es exitosa, o 'false' si falla.
     */
    CompletableFuture<Boolean> autenticar(DTOAutenticacion datos);
}

