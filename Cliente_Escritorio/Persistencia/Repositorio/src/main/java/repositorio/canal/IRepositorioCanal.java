package repositorio.canal;

import dominio.Canal;
import dto.canales.DTOMiembroCanal;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Contrato para el repositorio que gestiona la persistencia de los canales.
 * Todas las operaciones son asíncronas y devuelven un CompletableFuture.
 */
public interface IRepositorioCanal {

    /**
     * Guarda un nuevo canal en la base de datos local.
     *
     * @param canal El objeto de dominio Canal a persistir.
     * @return Un CompletableFuture que se completará con `true` si la operación fue exitosa, `false` en caso contrario.
     */
    CompletableFuture<Boolean> guardar(Canal canal);

    /**
     * Busca un canal por su ID.
     *
     * @param id El UUID del canal a buscar.
     * @return Un CompletableFuture que se completará con el Canal si se encuentra, o `null` si no.
     */
    CompletableFuture<Canal> buscarPorId(String id);

    /**
     * Obtiene todos los canales almacenados localmente.
     *
     * @return Un CompletableFuture que se completará con una lista de todos los canales.
     */
    CompletableFuture<List<Canal>> obtenerTodos();

    /**
     * Actualiza la información de un canal existente.
     *
     * @param canal El objeto Canal con los datos actualizados.
     * @return Un CompletableFuture que se completará con `true` si la actualización fue exitosa, `false` en caso contrario.
     */
    CompletableFuture<Boolean> actualizar(Canal canal);

    /**
     * Elimina un canal de la base de datos local por su ID.
     *
     * @param id El UUID del canal a eliminar.
     * @return Un CompletableFuture que se completará con `true` si la eliminación fue exitosa, `false` en caso contrario.
     */
    CompletableFuture<Boolean> eliminar(String id);

    /**
     * Agrega un miembro a un canal específico.
     *
     * @param canalId El UUID del canal.
     * @param usuarioId El UUID del usuario a agregar.
     * @return Un CompletableFuture que se completará con `true` si el miembro fue agregado exitosamente,
     *         `false` si ya existía o si la operación falló.
     */
    CompletableFuture<Boolean> agregarMiembroACanal(String canalId, String usuarioId);

    /**
     * Sincroniza una lista de canales del servidor con la base de datos local.
     * Este método actualiza o inserta los canales según sea necesario.
     *
     * @param canalesDelServidor Lista de canales obtenidos del servidor.
     * @return Un CompletableFuture que se completa cuando la sincronización finaliza.
     */
    CompletableFuture<Void> sincronizarCanales(List<Canal> canalesDelServidor);

    /**
     * Sincroniza la lista de miembros de un canal específico.
     * Actualiza la tabla canal_usuario con la información de los miembros del servidor.
     *
     * @param canalId El UUID del canal.
     * @param miembros Lista de DTOs con la información de los miembros del servidor.
     * @return Un CompletableFuture que se completa cuando la sincronización finaliza.
     */
    CompletableFuture<Void> sincronizarMiembros(String canalId, List<DTOMiembroCanal> miembros);
}

