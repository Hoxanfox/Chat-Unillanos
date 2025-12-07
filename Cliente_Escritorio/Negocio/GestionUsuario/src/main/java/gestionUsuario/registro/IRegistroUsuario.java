package gestionUsuario.registro;

import dto.vistaRegistro.DTORegistro;
import observador.ISujeto;
import java.util.concurrent.CompletableFuture;

/**
 * Contrato para el componente de negocio que maneja la lógica de
 * registro de nuevos usuarios.
 * Implementa ISujeto para notificar a la UI sobre eventos de registro.
 */
public interface IRegistroUsuario extends ISujeto {

    /**
     * Procesa el registro de un nuevo usuario, incluyendo el guardado local.
     * @param dto Contiene los datos para el nuevo registro (con photoId).
     * @param fotoBytes Los bytes de la foto para la persistencia local.
     * @return Una promesa que se resolverá con 'true' si fue exitoso, 'false' en caso contrario.
     */
    CompletableFuture<Boolean> registrar(DTORegistro dto, byte[] fotoBytes);
}
