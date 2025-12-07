package dto.gestionArchivos.response;

/** Wrapper reorganizado: response info de descarga */
public class DTODownloadInfo {
    private final dto.gestionArchivos.DTODownloadInfo original;

    public DTODownloadInfo(dto.gestionArchivos.DTODownloadInfo original) { this.original = original; }
    public dto.gestionArchivos.DTODownloadInfo getOriginal() { return original; }
}
