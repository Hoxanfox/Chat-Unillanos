package dto.mensajeria;

/**
 * DTO para solicitar la descarga de un archivo de un mensaje.
 * Usado en la ruta: solicitardescargaarchivomensaje
 *
 * Este DTO se usa cuando un cliente quiere descargar un archivo adjunto en un mensaje.
 */
public class DTOSolicitarDescargaArchivo {
    private String mensajeId; // ID del mensaje que contiene el archivo
    private String fileId; // ID del archivo a descargar
    private String userId; // ID del usuario que solicita la descarga

    public DTOSolicitarDescargaArchivo() {}

    public DTOSolicitarDescargaArchivo(String mensajeId, String fileId, String userId) {
        this.mensajeId = mensajeId;
        this.fileId = fileId;
        this.userId = userId;
    }

    // Getters y Setters
    public String getMensajeId() {
        return mensajeId;
    }

    public void setMensajeId(String mensajeId) {
        this.mensajeId = mensajeId;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}

