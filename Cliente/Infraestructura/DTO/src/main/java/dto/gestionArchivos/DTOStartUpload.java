package dto.gestionArchivos;

/**
 * DTO para el payload de la petición 'startFileUpload'.
 */
public final class DTOStartUpload {
    private final String fileName;
    private final String fileMimeType;
    private final int totalChunks;

    public DTOStartUpload(String fileName, String fileMimeType, int totalChunks) {
        this.fileName = fileName;
        this.fileMimeType = fileMimeType;
        this.totalChunks = totalChunks;
    }

    // Getters
    public String getFileName() { return fileName; }
    public String getFileMimeType() { return fileMimeType; }
    public int getTotalChunks() { return totalChunks; }
}
