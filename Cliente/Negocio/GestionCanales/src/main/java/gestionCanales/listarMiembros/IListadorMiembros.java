package gestionCanales.listarMiembros;

import dto.canales.DTOMiembroCanal;
import observador.ISujeto;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Contrato para el componente encargado de solicitar y gestionar
 * la lista de miembros de un canal específico.
 * AHORA extiende ISujeto para poder notificar cambios a los observadores.
 */
public interface IListadorMiembros extends ISujeto {

    /**
     * Inicia el proceso para solicitar la lista de miembros de un canal al servidor.
     * La operación es asíncrona y el resultado se obtiene a través del CompletableFuture.
     *
     * @param canalId El ID del canal del cual se quieren listar los miembros.
     * @return Un CompletableFuture que se completará con la lista de DTOs de los miembros del canal.
     */
    CompletableFuture<List<DTOMiembroCanal>> solicitarMiembros(String canalId);
}
