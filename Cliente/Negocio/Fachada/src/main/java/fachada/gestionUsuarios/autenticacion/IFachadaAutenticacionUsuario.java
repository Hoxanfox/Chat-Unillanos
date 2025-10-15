package fachada.gestionUsuarios.autenticacion;

import dto.vistaLogin.DTOAutenticacion;
import dto.vistaRegistro.DTORegistro;

import java.util.concurrent.CompletableFuture;

/**
 * Contrato para la Fachada que gestiona la autenticación y registro de usuarios.
 */
public interface IFachadaAutenticacionUsuario {
    CompletableFuture<Boolean> autenticarUsuario(DTOAutenticacion dto);

    /**
     * Procesa la lógica de registro de un usuario.
     * @param dto Contiene los datos del usuario para el servidor.
     * @param fotoBytes Los bytes de la foto para la persistencia local.
     * @return Una promesa que se resolverá con el resultado del registro.
     */
    CompletableFuture<Boolean> registrarUsuario(DTORegistro dto, byte[] fotoBytes);
}

