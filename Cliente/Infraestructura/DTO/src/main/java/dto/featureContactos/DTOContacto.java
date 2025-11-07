package dto.featureContactos;

import com.google.gson.annotations.SerializedName;

/**
 * DTO que representa la información de un contacto para ser mostrada en la vista.
 * Anotado con @SerializedName para mapear correctamente los campos del servidor.
 */
public class DTOContacto {
    private final String id;

    @SerializedName(value = "peerId", alternate = {"idPeer"})
    private final String peerId;  // ID del peer WebRTC del contacto

    private final String nombre;
    private final String email;
    private final String estado;

    @SerializedName(value = "photoId", alternate = {"photoAddress", "imagenId", "photoFileId"})
    private final String photoId; // ID de la foto en el servidor

    private final String fechaRegistro; // Fecha de registro del contacto

    @SerializedName("imagenBase64")
    private final String imagenBase64; // Imagen en base64 (opcional, puede venir del servidor)

    @SerializedName("conectado")
    private final String conectado; // Estado de conexión (puede ser diferente a 'estado')

    private String localPhotoPath; // Ruta local de la foto (mutable para actualización)

    // Constructor sin peerId para compatibilidad
    public DTOContacto(String id, String nombre, String estado) {
        this.id = id;
        this.peerId = null;
        this.nombre = nombre;
        this.email = null;
        this.estado = estado;
        this.photoId = null;
        this.fechaRegistro = null;
        this.imagenBase64 = null;
        this.conectado = null;
    }

    // Constructor completo con todos los campos
    public DTOContacto(String id, String peerId, String nombre, String email, String estado) {
        this.id = id;
        this.peerId = peerId;
        this.nombre = nombre;
        this.email = email;
        this.estado = estado;
        this.photoId = null;
        this.fechaRegistro = null;
        this.imagenBase64 = null;
        this.conectado = null;
    }

    // Constructor completo con photoId y fechaRegistro (para deserialización desde servidor)
    public DTOContacto(String id, String peerId, String nombre, String email, String estado,
                      String photoId, String fechaRegistro) {
        this.id = id;
        this.peerId = peerId;
        this.nombre = nombre;
        this.email = email;
        this.estado = estado;
        this.photoId = photoId;
        this.fechaRegistro = fechaRegistro;
        this.imagenBase64 = null;
        this.conectado = null;
    }

    public String getId() {
        return id;
    }

    public String getPeerId() {
        return peerId;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEmail() {
        return email;
    }

    public String getEstado() {
        // Si 'estado' es null, usar el campo 'conectado' que viene del servidor
        return estado != null ? estado : conectado;
    }

    public String getPhotoId() {
        return photoId;
    }

    public String getFechaRegistro() {
        return fechaRegistro;
    }

    public String getImagenBase64() {
        return imagenBase64;
    }

    public String getConectado() {
        return conectado;
    }

    public String getLocalPhotoPath() {
        return localPhotoPath;
    }

    public void setLocalPhotoPath(String localPhotoPath) {
        this.localPhotoPath = localPhotoPath;
    }

    @Override
    public String toString() {
        return "DTOContacto{" +
                "id='" + id + '\'' +
                ", peerId='" + peerId + '\'' +
                ", nombre='" + nombre + '\'' +
                ", email='" + email + '\'' +
                ", estado='" + estado + '\'' +
                ", conectado='" + conectado + '\'' +
                ", photoId='" + photoId + '\'' +
                ", fechaRegistro='" + fechaRegistro + '\'' +
                '}';
    }
}
