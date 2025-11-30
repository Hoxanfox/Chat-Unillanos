package dto.gestionArchivos.request;

/** Wrapper reorganizado: request para iniciar descarga */
public class DTOStartDownload {
    private final dto.gestionArchivos.DTOStartDownload original;

    public DTOStartDownload(dto.gestionArchivos.DTOStartDownload original) { this.original = original; }
    public dto.gestionArchivos.DTOStartDownload getOriginal() { return original; }
}
