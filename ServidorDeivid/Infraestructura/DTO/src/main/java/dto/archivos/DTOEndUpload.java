package dto.archivos;

public class DTOEndUpload {
    private String uploadId;
    private String fileHash;

    public DTOEndUpload() {}

    public DTOEndUpload(String uploadId, String fileHash) {
        this.uploadId = uploadId;
        this.fileHash = fileHash;
    }

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }
}

