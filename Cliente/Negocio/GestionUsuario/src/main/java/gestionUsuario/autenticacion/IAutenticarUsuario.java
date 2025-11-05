package gestionUsuario.autenticacion;

import dto.vistaLogin.DTOAutenticacion;
import observador.ISujeto;
import java.util.concurrent.CompletableFuture;

/**
 * Contrato para el componente que maneja la lógica de negocio de autenticación.
 * La operación es asíncrona para no bloquear el hilo principal.
 * Implementa ISujeto para notificar a la UI sobre eventos de autenticación.
 */
public interface IAutenticarUsuario extends ISujeto {
    /**
     * Valida las credenciales de un usuario de forma asíncrona.
     * @param dto Los datos de autenticación (email y contraseña).
     * @return Un Future que se completará con el resultado de la validación.
     */
    CompletableFuture<Boolean> autenticar(DTOAutenticacion dto);
}
