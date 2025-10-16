package gestionCanales.aceptarInvitacion;

import java.util.concurrent.CompletableFuture;

/**
 * Define el contrato para la lógica de negocio que se encarga de
 * aceptar una invitación a un canal.
 */
public interface IAceptadorInvitacion {

    /**
     * Envía una petición al servidor para unirse a un canal después de haber recibido una invitación.
     *
     * @param canalId El ID del canal al que el usuario desea unirse.
     * @return Un CompletableFuture que se completa sin valor si la operación fue exitosa,
     * o con una excepción si ocurrió un error.
     */
    CompletableFuture<Void> aceptarInvitacion(String canalId);
}
