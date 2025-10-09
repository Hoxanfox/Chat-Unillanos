package fachada.gestionUsuarios;

import dto.vistaLogin.DTOAutenticacion;
import java.util.concurrent.CompletableFuture;

/**
 * Contrato para la Fachada que gestiona la autenticación de usuarios.
 * Es el único punto de entrada a esta lógica de negocio desde las capas superiores.
 */
public interface IFachadaAutenticacionUsuario {

    /**
     * Procesa la lógica de autenticación de un usuario de forma asíncrona.
     * @param dto Contiene el email y la contraseña del usuario.
     * @return Un Future que se completará con el resultado de la autenticación.
     */
    CompletableFuture<Boolean> autenticarUsuario(DTOAutenticacion dto);
}

