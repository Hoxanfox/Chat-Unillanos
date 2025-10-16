package gestionCanales.nuevoCanal;

import dominio.Canal;
import observador.ISujeto;
import java.util.concurrent.CompletableFuture;

/**
 * Contrato para la lógica de creación de un nuevo canal.
 * Devuelve un objeto del Dominio.
 * Implementa ISujeto para notificar eventos de creación.
 */
public interface ICreadorCanal extends ISujeto {

    /**
     * Inicia el proceso de creación de un nuevo canal.
     * Envía la petición al servidor y, si es exitoso, guarda el canal en el repositorio local.
     *
     * @param nombre      El nombre del nuevo canal.
     * @param descripcion Una descripción opcional para el canal.
     * @return Un CompletableFuture que se completará con el objeto Canal del dominio
     * una vez que se haya creado en el servidor y guardado localmente,
     * o con una excepción si ocurre un error.
     */
    CompletableFuture<Canal> crearCanal(String nombre, String descripcion);
}
