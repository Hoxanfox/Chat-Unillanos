package dto.gestionArchivos;

/**
 * DTO para el payload de la petición 'endFileUpload'.
 */
public final class DTOEndUpload {
    private final String uploadId;
    private final String fileHash_sha256;

    public DTOEndUpload(String uploadId, String fileHash_sha256) {
        this.uploadId = uploadId;
        this.fileHash_sha256 = fileHash_sha256;
    }

    // Getters
    public String getUploadId() { return uploadId; }
    public String getFileHash_sha256() { return fileHash_sha256; }
}