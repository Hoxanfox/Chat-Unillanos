package gestionCanales.invitarMiembro;

import java.util.concurrent.CompletableFuture;

/**
 * Contrato para la lógica de negocio encargada de invitar a un miembro a un canal.
 */
public interface IInvitadorMiembro {

    /**
     * Envía una solicitud al servidor para agregar un contacto a un canal específico.
     * Esta operación debe ser realizada por un administrador del canal.
     *
     * @param canalId    El ID del canal al que se invitará el miembro.
     * @param contactoId El ID del contacto (usuario) que será invitado.
     * @return Un CompletableFuture que se completa sin valor si la invitación fue exitosa,
     * o con una excepción si ocurrió un error.
     */
    CompletableFuture<Void> invitarMiembro(String canalId, String contactoId);
}
