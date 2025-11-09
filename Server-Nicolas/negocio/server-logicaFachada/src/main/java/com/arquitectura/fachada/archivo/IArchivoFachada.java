package com.arquitectura.fachada.archivo;

import com.arquitectura.DTO.archivos.DTODownloadInfo;
import com.arquitectura.DTO.archivos.DTOEndUpload;
import com.arquitectura.DTO.archivos.DTOStartUpload;
import com.arquitectura.DTO.archivos.DTOUploadChunk;
import com.arquitectura.utils.chunkManager.FileUploadResponse;

import java.io.IOException;
import java.util.UUID;

/**
 * Fachada especializada para operaciones relacionadas con archivos.
 */
public interface IArchivoFachada {

    /**
     * Inicia el proceso de carga de un archivo.
     * @param dto Datos iniciales de la carga
     * @return ID de la sesión de carga
     * @throws Exception si hay error al iniciar la carga
     */
    String startUpload(DTOStartUpload dto) throws Exception;

    /**
     * Procesa un fragmento (chunk) de un archivo.
     * @param dto Datos del fragmento
     * @throws Exception si hay error al procesar el fragmento
     */
    void processChunk(DTOUploadChunk dto) throws Exception;

    /**
     * Finaliza el proceso de carga de un archivo.
     * @param dto Datos de finalización
     * @param autorId ID del autor que sube el archivo
     * @param subDirectory Subdirectorio donde guardar el archivo
     * @return Respuesta con información del archivo subido
     * @throws Exception si hay error al finalizar la carga
     */
    FileUploadResponse endUpload(DTOEndUpload dto, UUID autorId, String subDirectory) throws Exception;

    /**
     * Inicia el proceso de descarga de un archivo.
     * @param fileId ID del archivo a descargar
     * @return Información de descarga
     * @throws Exception si el archivo no existe
     */
    DTODownloadInfo startDownload(String fileId) throws Exception;

    /**
     * Obtiene un fragmento específico de un archivo en descarga.
     * @param downloadId ID de la sesión de descarga
     * @param chunkNumber Número del fragmento a obtener
     * @return Bytes del fragmento
     * @throws Exception si hay error al obtener el fragmento
     */
    byte[] getChunk(String downloadId, int chunkNumber) throws Exception;

    /**
     * Obtiene un archivo como string base64.
     * @param relativePath Ruta relativa del archivo
     * @return Contenido del archivo en base64
     * @throws IOException si hay error al leer el archivo
     */
    String getFileAsBase64(String relativePath) throws IOException, Exception;
}
