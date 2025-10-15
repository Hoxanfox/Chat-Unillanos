package gestionArchivos;

import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * Contrato para el componente de negocio que gestiona toda la lógica
 * relacionada con la manipulación y transferencia de archivos.
 */
public interface IGestionArchivos {

    /**
     * Orquesta el proceso completo de subir un archivo al servidor,
     * manejando el protocolo de división por chunks.
     *
     * @param archivo El archivo local que se desea subir.
     * @return Una promesa (CompletableFuture) que se resolverá con un identificador
     * único del archivo en el servidor si la subida es exitosa, o
     * se completará excepcionalmente si ocurre un error.
     */
    CompletableFuture<String> subirArchivo(File archivo);

    /**
     * Sube un archivo al servidor durante el proceso de registro (sin autenticación).
     *
     * @param archivo El archivo a subir
     * @return CompletableFuture que se completa con el ID del archivo subido
     */
    CompletableFuture<String> subirArchivoParaRegistro(File archivo);
}