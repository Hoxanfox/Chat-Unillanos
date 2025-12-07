package dto.vistaRegistro;

import java.io.File;

/**
 * DTO para capturar los datos directamente del formulario de registro en la capa de Presentación.
 * Este DTO es utilizado por la Vista y el Controlador.
 */
public final class DTOFormularioRegistro {
    private final String name;
    private final String email;
    private final String password;
    private final File photoFile; // Capturamos el archivo de la foto, no el Base64
    private final String ip;

    public DTOFormularioRegistro(String name, String email, String password, File photoFile, String ip) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.photoFile = photoFile;
        this.ip = ip;
    }

    // Getters para todos los campos...

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public File getPhotoFile() {
        return photoFile;
    }

    public String getIp() {
        return ip;
    }
}
