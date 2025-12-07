package repositorio.archivo;

import dominio.Archivo;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Contrato para el repositorio que gestiona la persistencia de archivos.
 */
public interface IRepositorioArchivo {

    /**
     * Guarda un archivo con su contenido en Base64.
     *
     * @param archivo El archivo a guardar
     * @return CompletableFuture que se completa con true si fue exitoso
     */
    CompletableFuture<Boolean> guardar(Archivo archivo);

    /**
     * Busca un archivo por el ID que asignó el servidor.
     *
     * @param fileIdServidor El ID del archivo en el servidor
     * @return CompletableFuture que se completa con el archivo o null si no existe
     */
    CompletableFuture<Archivo> buscarPorFileIdServidor(String fileIdServidor);

    /**
     * Busca archivos asociados a una entidad específica.
     *
     * @param asociadoA Tipo de asociación ('perfil', 'mensaje', 'canal', etc.)
     * @param idAsociado ID de la entidad asociada
     * @return CompletableFuture con lista de archivos
     */
    CompletableFuture<List<Archivo>> buscarPorAsociacion(String asociadoA, String idAsociado);

    /**
     * Actualiza el estado de un archivo.
     *
     * @param fileIdServidor ID del archivo en el servidor
     * @param nuevoEstado Nuevo estado ('descargando', 'completo', 'error')
     * @return CompletableFuture que se completa con true si fue exitoso
     */
    CompletableFuture<Boolean> actualizarEstado(String fileIdServidor, String nuevoEstado);

    /**
     * Actualiza el contenido Base64 de un archivo.
     *
     * @param fileIdServidor ID del archivo en el servidor
     * @param contenidoBase64 Contenido completo del archivo en Base64
     * @return CompletableFuture que se completa con true si fue exitoso
     */
    CompletableFuture<Boolean> actualizarContenido(String fileIdServidor, String contenidoBase64);

    /**
     * Elimina un archivo de la base de datos local.
     *
     * @param fileIdServidor ID del archivo en el servidor
     * @return CompletableFuture que se completa con true si fue eliminado
     */
    CompletableFuture<Boolean> eliminar(String fileIdServidor);

    /**
     * Verifica si un archivo ya existe localmente.
     *
     * @param fileIdServidor ID del archivo en el servidor
     * @return CompletableFuture que se completa con true si existe
     */
    CompletableFuture<Boolean> existe(String fileIdServidor);
}

