package dto.gestionArchivos;

/**
 * DTO para la información de inicio de descarga recibida del servidor.
 */
public class DTODownloadInfo {
    private String downloadId;
    private String fileName;
    private long fileSize;
    private int totalChunks;
    private String mimeType;

    public DTODownloadInfo() {}

    public DTODownloadInfo(String downloadId, String fileName, long fileSize, int totalChunks, String mimeType) {
        this.downloadId = downloadId;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.totalChunks = totalChunks;
        this.mimeType = mimeType;
    }

    public String getDownloadId() {
        return downloadId;
    }

    public void setDownloadId(String downloadId) {
        this.downloadId = downloadId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(int totalChunks) {
        this.totalChunks = totalChunks;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
}

