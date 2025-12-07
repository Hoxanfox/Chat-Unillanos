package dto.gestionArchivos;

/**
 * DTO para el payload de la petición 'uploadFileChunk'.
 */
public final class DTOUploadChunk {
    private final String uploadId;
    private final int chunkNumber;
    private final String chunkData_base64;

    public DTOUploadChunk(String uploadId, int chunkNumber, String chunkData_base64) {
        this.uploadId = uploadId;
        this.chunkNumber = chunkNumber;
        this.chunkData_base64 = chunkData_base64;
    }

    // Getters
    public String getUploadId() { return uploadId; }
    public int getChunkNumber() { return chunkNumber; }
    public String getChunkData_base64() { return chunkData_base64; }
}