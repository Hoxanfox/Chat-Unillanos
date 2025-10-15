package fachada.gestionArchivos;

import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * Contrato para la Fachada que gestiona las operaciones de archivos.
 * Es el único punto de entrada a esta lógica de negocio desde el Servicio.
 */
public interface IFachadaArchivos {

    /**
     * Delega la tarea de subir un archivo al componente de gestión correspondiente.
     * @param archivo El archivo local que se desea subir.
     * @return Una promesa que se resolverá con el identificador del archivo en el servidor.
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
