package dto.gestionArchivos;

/**
 * DTO para solicitar un chunk específico de descarga.
 */
public class DTORequestChunk {
    private String downloadId;
    private int chunkNumber;

    public DTORequestChunk(String downloadId, int chunkNumber) {
        this.downloadId = downloadId;
        this.chunkNumber = chunkNumber;
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
}

