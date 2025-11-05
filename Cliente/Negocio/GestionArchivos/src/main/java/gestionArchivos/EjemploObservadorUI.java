package gestionArchivos;

import observador.IObservador;
import dto.gestionArchivos.DTODownloadInfo;

import java.io.File;

/**
 * Ejemplo de cómo la UI puede implementar IObservador para recibir notificaciones
 * sobre el progreso de descarga de archivos y otras actualizaciones.
 */
public class EjemploObservadorUI implements IObservador {

    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        switch (tipoDeDato) {
            case "DESCARGA_INICIADA":
                String fileId = (String) datos;
                System.out.println("[UI] Descarga iniciada para archivo ID: " + fileId);
                // Aquí podrías mostrar un diálogo de progreso en la UI
                break;

            case "DESCARGA_INFO":
                DTODownloadInfo info = (DTODownloadInfo) datos;
                System.out.println("[UI] Información de descarga recibida:");
                System.out.println("    - Nombre: " + info.getFileName());
                System.out.println("    - Tamaño: " + info.getFileSize() + " bytes");
                System.out.println("    - Chunks: " + info.getTotalChunks());
                // Aquí podrías actualizar la UI con la información del archivo
                break;

            case "DESCARGA_PROGRESO":
                Integer progreso = (Integer) datos;
                System.out.println("[UI] Progreso de descarga: " + progreso + "%");
                // Aquí podrías actualizar una barra de progreso en la UI
                break;

            case "DESCARGA_COMPLETADA":
                File archivoDescargado = (File) datos;
                System.out.println("[UI] Descarga completada: " + archivoDescargado.getAbsolutePath());
                // Aquí podrías mostrar un mensaje de éxito y cerrar el diálogo de progreso
                // También podrías abrir el archivo o mostrar una notificación
                break;

            case "DESCARGA_ERROR":
                String mensajeError = (String) datos;
                System.err.println("[UI] Error en descarga: " + mensajeError);
                // Aquí podrías mostrar un diálogo de error en la UI
                break;

            case "SUBIDA_PROGRESO":
                Integer progresoSubida = (Integer) datos;
                System.out.println("[UI] Progreso de subida: " + progresoSubida + "%");
                // Similar al progreso de descarga
                break;

            case "BASE_DATOS_ACTUALIZADA":
                String tipoActualizacion = (String) datos;
                System.out.println("[UI] Base de datos actualizada: " + tipoActualizacion);
                // Aquí podrías refrescar la vista correspondiente (canales, contactos, etc.)
                break;

            default:
                System.out.println("[UI] Actualización desconocida: " + tipoDeDato);
                break;
        }
    }

    /**
     * Ejemplo de uso:
     * 
     * // En tu controlador o ventana de UI:
     * IGestionArchivos gestionArchivos = new GestionArchivosImpl();
     * EjemploObservadorUI observadorUI = new EjemploObservadorUI();
     * 
     * // Registrar el observador
     * gestionArchivos.registrarObservador(observadorUI);
     * 
     * // Descargar un archivo
     * File directorioDestino = new File("./descargas");
     * gestionArchivos.descargarArchivo("file-id-123", directorioDestino)
     *     .thenAccept(archivo -> {
     *         System.out.println("Archivo descargado: " + archivo.getName());
     *     })
     *     .exceptionally(ex -> {
     *         System.err.println("Error al descargar: " + ex.getMessage());
     *         return null;
     *     });
     */
}

