package repositorio.usuario;

import dominio.Usuario;
import dto.repositorio.DTOUsuarioRepositorio;

/**
 * Implementación del Repositorio de Usuarios.
 */
public class RepositorioUsuarioImpl implements IRepositorioUsuario {

    @Override
    public void guardarUsuario(DTOUsuarioRepositorio datosUsuario) {
        // El Repositorio recibe el DTO y lo convierte al objeto de Dominio.
        Usuario usuarioParaGuardar = new Usuario(
                datosUsuario.getUserId(),
                datosUsuario.getName(),
                datosUsuario.getEmail(),
                datosUsuario.getPassword(),
                datosUsuario.getFotoBytes(),
                datosUsuario.getPhotoId(), // Se añade el photoId
                datosUsuario.getIp(),
                datosUsuario.getFechaRegistro()
        );

        // ... Lógica de persistencia con el objeto de dominio completo ...
        System.out.println("--- INICIO REPOSITORIO ---");
        System.out.println("Guardando usuario en la base de datos local...");
        System.out.println("ID: " + usuarioParaGuardar.getIdUsuario());
        System.out.println("Photo ID (Servidor): " + usuarioParaGuardar.getPhotoId());
        System.out.println("Tamaño de la foto local (bytes): " + (usuarioParaGuardar.getFoto() != null ? usuarioParaGuardar.getFoto().length : 0));
        System.out.println("Usuario guardado exitosamente.");
        System.out.println("--- FIN REPOSITORIO ---");
    }
}

