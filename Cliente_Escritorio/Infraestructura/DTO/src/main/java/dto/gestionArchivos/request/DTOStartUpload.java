package dto.gestionArchivos.request;

/** Wrapper reorganizado: request para iniciar upload */
public class DTOStartUpload {
    private final dto.gestionArchivos.DTOStartUpload original;

    public DTOStartUpload(dto.gestionArchivos.DTOStartUpload original) { this.original = original; }
    public dto.gestionArchivos.DTOStartUpload getOriginal() { return original; }
}
