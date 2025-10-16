package gestionCanales.listarMiembros;

import dto.canales.DTOMiembroCanal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Contrato para el componente encargado de solicitar y gestionar
 * la lista de miembros de un canal específico.
 */
public interface IListadorMiembros {

    /**
     * Inicia el proceso para solicitar la lista de miembros de un canal al servidor.
     * La operación es asíncrona y el resultado se obtiene a través del CompletableFuture.
     *
     * @param canalId El ID del canal del cual se quieren listar los miembros.
     * @return Un CompletableFuture que se completará con la lista de DTOs de los miembros del canal.
     */
    CompletableFuture<List<DTOMiembroCanal>> solicitarMiembros(String canalId);
}
