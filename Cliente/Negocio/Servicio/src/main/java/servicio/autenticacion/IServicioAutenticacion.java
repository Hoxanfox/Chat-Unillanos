package servicio.autenticacion;

import dto.vistaLogin.DTOAutenticacion;
import java.util.concurrent.CompletableFuture;

/**
 * Contrato para el servicio de autenticación.
 */
public interface IServicioAutenticacion {
    /**
     * Procesa la autenticación de forma asíncrona.
     * @param datos Los datos de autenticación.
     * @return Un Future que se completará con el resultado de la autenticación.
     */
    CompletableFuture<Boolean> autenticar(DTOAutenticacion datos);
}

