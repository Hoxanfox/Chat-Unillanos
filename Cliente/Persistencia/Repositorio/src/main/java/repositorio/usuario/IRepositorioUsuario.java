package repositorio.usuario;

import dto.repositorio.DTOUsuarioRepositorio;

/**
 * Contrato para el Repositorio que gestiona la persistencia
 * de los datos de usuario en el cliente.
 */
public interface IRepositorioUsuario {

    /**
     * Guarda los datos de un nuevo usuario en la persistencia local.
     * @param datosUsuario El DTO con la información del usuario a guardar.
     */
    void guardarUsuario(DTOUsuarioRepositorio datosUsuario);
}

