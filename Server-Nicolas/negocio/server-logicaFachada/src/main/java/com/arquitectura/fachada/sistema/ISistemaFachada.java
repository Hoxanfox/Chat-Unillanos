package com.arquitectura.fachada.sistema;

import java.io.IOException;

/**
 * Fachada especializada para operaciones del sistema y utilidades.
 */
public interface ISistemaFachada {

    /**
     * Obtiene el contenido de los logs del sistema.
     * @return Contenido del archivo de logs
     * @throws IOException si hay error al leer los logs
     */
    String getLogContents() throws IOException;
}
