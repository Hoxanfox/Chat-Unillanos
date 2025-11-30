package dto.archivos;

public class DTOStartDownload {
    private String fileId;

    public DTOStartDownload() {}

    public DTOStartDownload(String fileId) {
        this.fileId = fileId;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }
}

