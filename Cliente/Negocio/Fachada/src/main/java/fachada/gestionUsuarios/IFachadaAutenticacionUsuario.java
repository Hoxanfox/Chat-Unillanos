package fachada.gestionUsuarios;

import dto.vistaLogin.DTOAutenticacion;

/**
 * Contrato para la Fachada que gestiona la autenticación de usuarios.
 * Es el único punto de entrada a esta lógica de negocio desde las capas superiores.
 */
public interface IFachadaAutenticacionUsuario {

    /**
     * Procesa la lógica de autenticación de un usuario.
     * @param dto Contiene el email y la contraseña del usuario.
     * @return true si la autenticación es exitosa, false en caso contrario.
     */
    boolean autenticarUsuario(DTOAutenticacion dto);

    // Aquí se añadirían más métodos relacionados con la gestión de usuarios si fuera necesario.
}
