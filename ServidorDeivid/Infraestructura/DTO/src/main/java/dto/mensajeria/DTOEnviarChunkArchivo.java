package dto.mensajeria;

/**
 * DTO para enviar chunks de archivos en mensajes directos.
 * Usado en la ruta: enviarchunkmensajearchivo
 *
 * Este DTO se usa para enviar el archivo en partes (chunks) al servidor.
 */
public class DTOEnviarChunkArchivo {
    private String mensajeId; // ID del mensaje asociado al archivo
    private String uploadId; // ID de la sesión de carga
    private int chunkNumber; // Número del chunk actual (0-based)
    private String chunkDataBase64; // Datos del chunk en Base64

    public DTOEnviarChunkArchivo() {}

    public DTOEnviarChunkArchivo(String mensajeId, String uploadId,
                                 int chunkNumber, String chunkDataBase64) {
        this.mensajeId = mensajeId;
        this.uploadId = uploadId;
        this.chunkNumber = chunkNumber;
        this.chunkDataBase64 = chunkDataBase64;
    }

    // Getters y Setters
    public String getMensajeId() {
        return mensajeId;
    }

    public void setMensajeId(String mensajeId) {
        this.mensajeId = mensajeId;
    }

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public int getChunkNumber() {
        return chunkNumber;
    }

    public void setChunkNumber(int chunkNumber) {
        this.chunkNumber = chunkNumber;
    }

    public String getChunkDataBase64() {
        return chunkDataBase64;
    }

    public void setChunkDataBase64(String chunkDataBase64) {
        this.chunkDataBase64 = chunkDataBase64;
    }
}

