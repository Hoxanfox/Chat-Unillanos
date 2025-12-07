package dto.archivos;

public class DTODownloadInfo {
    private String downloadId;
    private String fileName;
    private String mimeType;
    private long fileSize;
    private int totalChunks;

    public DTODownloadInfo() {}

    public DTODownloadInfo(String downloadId, String fileName, String mimeType, long fileSize, int totalChunks) {
        this.downloadId = downloadId;
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.totalChunks = totalChunks;
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

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
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
}

