package dto.archivos;

public class DTOStartUpload {
    private String fileName;
    private String mimeType;
    private int totalChunks;

    public DTOStartUpload() {}

    public DTOStartUpload(String fileName, String mimeType, int totalChunks) {
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.totalChunks = totalChunks;
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

    public int getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(int totalChunks) {
        this.totalChunks = totalChunks;
    }
}

