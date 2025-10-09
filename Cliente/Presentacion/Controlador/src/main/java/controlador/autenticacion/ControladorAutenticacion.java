package controlador.autenticacion;

import dto.vistaLogin.DTOAutenticacion;
import dto.vistaRegistro.DTOFormularioRegistro;
import dto.vistaRegistro.DTORegistro;
import servicio.archivos.IServicioArchivos;
import servicio.archivos.ServicioArchivosImpl;
import servicio.autenticacion.IServicioAutenticacion;
import servicio.autenticacion.ServicioAutenticacion;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;

/**
 * Implementación del controlador que orquesta la autenticación y el registro.
 */
public class ControladorAutenticacion implements IControladorAutenticacion {

    private final IServicioAutenticacion servicioAutenticacion;
    private final IServicioArchivos servicioArchivos;

    public ControladorAutenticacion() {
        this.servicioAutenticacion = new ServicioAutenticacion();
        this.servicioArchivos = new ServicioArchivosImpl();
    }

    @Override
    public CompletableFuture<Boolean> autenticar(DTOAutenticacion datos) {
        return servicioAutenticacion.autenticar(datos);
    }

    @Override
    public CompletableFuture<Boolean> registrar(DTOFormularioRegistro datosFormulario) {
        File archivoFoto = datosFormulario.getPhotoFile();
        if (archivoFoto == null) {
            // Manejar el caso en que no se selecciona foto
            return CompletableFuture.failedFuture(new IllegalArgumentException("No se ha seleccionado una foto de perfil."));
        }

        try {
            // 1. Leer los bytes del archivo para la persistencia local.
            byte[] fotoBytes = Files.readAllBytes(archivoFoto.toPath());

            // 2. Iniciar la subida del archivo al servidor.
            return servicioArchivos.subirArchivo(archivoFoto)
                    .thenCompose(photoId -> {
                        // 3. Crear el DTO de registro con el photoId obtenido.
                        DTORegistro datosRegistro = new DTORegistro(
                                datosFormulario.getName(),
                                datosFormulario.getEmail(),
                                datosFormulario.getPassword(),
                                photoId,
                                datosFormulario.getIp()
                        );

                        // 4. CORRECCIÓN: Iniciar el registro del usuario, pasando AHORA los bytes
                        // para el guardado local en el cliente.
                        return servicioAutenticacion.registrar(datosRegistro, fotoBytes);
                    });
        } catch (IOException e) {
            System.err.println("Error al leer el archivo de la foto: " + e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }
}

