package servicio.autenticacion;

import dto.vistaLogin.DTOAutenticacion;
import dto.vistaRegistro.DTORegistro;

import java.util.concurrent.CompletableFuture;

/**
 * Contrato para el servicio de autenticación y registro.
 * Es el punto de entrada desde el Controlador a la capa de Negocio.
 */
public interface IServicioAutenticacion {

    CompletableFuture<Boolean> autenticar(DTOAutenticacion datos);

    /**
     * Inicia el proceso de registro de un nuevo usuario.
     * @param datos Contiene los datos del usuario para el servidor (con photoId).
     * @param fotoBytes Los bytes de la foto para la persistencia local en el cliente.
     * @return Una promesa que se resolverá con el resultado del registro.
     */
    CompletableFuture<Boolean> registrar(DTORegistro datos, byte[] fotoBytes);
}

