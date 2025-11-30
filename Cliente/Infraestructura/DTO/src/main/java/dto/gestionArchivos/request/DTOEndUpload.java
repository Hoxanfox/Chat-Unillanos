package dto.gestionArchivos.request;

/** Wrapper reorganizado: request para finalizar upload */
public class DTOEndUpload {
    private final dto.gestionArchivos.DTOEndUpload original;

    public DTOEndUpload(dto.gestionArchivos.DTOEndUpload original) { this.original = original; }
    public dto.gestionArchivos.DTOEndUpload getOriginal() { return original; }
}
