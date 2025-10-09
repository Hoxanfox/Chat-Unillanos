package servicio.archivos;

import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * Contrato para el servicio que expone la lógica de gestión de archivos
 * a la capa de Presentación (Controladores).
 */
public interface IServicioArchivos {

    /**
     * Inicia el proceso de subida de un archivo.
     * @param archivo El archivo seleccionado por el usuario en la vista.
     * @return Una promesa que se resolverá con el identificador del archivo en el servidor.
     */
    CompletableFuture<String> subirArchivo(File archivo);
}
