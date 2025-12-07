package dto.mensajeria;

/**
 * DTO para recibir chunks de archivos durante la descarga.
 * Respuesta del servidor cuando se solicita un chunk de archivo.
 *
 * Este DTO se usa para enviar chunks del archivo desde el servidor al cliente.
 */
public class DTOChunkArchivoDescarga {
    private String mensajeId;
    private String fileId;
    private int chunkNumber;
    private int totalChunks;
    private String chunkDataBase64;
    private boolean isLastChunk;

    public DTOChunkArchivoDescarga() {}

    public DTOChunkArchivoDescarga(String mensajeId, String fileId, int chunkNumber,
                                   int totalChunks, String chunkDataBase64, boolean isLastChunk) {
        this.mensajeId = mensajeId;
        this.fileId = fileId;
        this.chunkNumber = chunkNumber;
        this.totalChunks = totalChunks;
        this.chunkDataBase64 = chunkDataBase64;
        this.isLastChunk = isLastChunk;
    }

    // Getters y Setters
    public String getMensajeId() {
        return mensajeId;
    }

    public void setMensajeId(String mensajeId) {
        this.mensajeId = mensajeId;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public int getChunkNumber() {
        return chunkNumber;
    }

    public void setChunkNumber(int chunkNumber) {
        this.chunkNumber = chunkNumber;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(int totalChunks) {
        this.totalChunks = totalChunks;
    }

    public String getChunkDataBase64() {
        return chunkDataBase64;
    }

    public void setChunkDataBase64(String chunkDataBase64) {
        this.chunkDataBase64 = chunkDataBase64;
    }

    public boolean isLastChunk() {
        return isLastChunk;
    }

    public void setLastChunk(boolean lastChunk) {
        isLastChunk = lastChunk;
    }
}


