package dto.gestionArchivos;

/**
 * DTO para recibir un chunk de datos del servidor durante la descarga.
 */
public class DTODownloadChunk {
    private String downloadId;
    private int chunkNumber;
    private String chunkData; // Base64 encoded
    private boolean isLast;

    public DTODownloadChunk() {}

    public DTODownloadChunk(String downloadId, int chunkNumber, String chunkData, boolean isLast) {
        this.downloadId = downloadId;
        this.chunkNumber = chunkNumber;
        this.chunkData = chunkData;
        this.isLast = isLast;
    }

    public String getDownloadId() {
        return downloadId;
    }

    public void setDownloadId(String downloadId) {
        this.downloadId = downloadId;
    }

    public int getChunkNumber() {
        return chunkNumber;
    }

    public void setChunkNumber(int chunkNumber) {
        this.chunkNumber = chunkNumber;
    }

    public String getChunkData() {
        return chunkData;
    }

    public void setChunkData(String chunkData) {
        this.chunkData = chunkData;
    }

    public boolean isLast() {
        return isLast;
    }

    public void setLast(boolean last) {
        isLast = last;
    }
}

