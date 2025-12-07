package dto.gestionArchivos;

/**
 * DTO para solicitar el inicio de una descarga de archivo.
 */
public class DTOStartDownload {
    private String fileId;

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

