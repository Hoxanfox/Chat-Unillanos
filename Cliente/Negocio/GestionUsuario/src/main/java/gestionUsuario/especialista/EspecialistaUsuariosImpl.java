package gestionUsuario.especialista;

import dominio.Usuario;
import dto.vistaLobby.DTOUsuario;
import repositorio.usuario.IRepositorioUsuario;
import repositorio.usuario.RepositorioUsuarioImpl;

import java.util.List;
import java.util.UUID;

/**
 * Implementación del especialista de usuarios.
 * Contiene la lógica de negocio y coordina con el repositorio.
 */
public class EspecialistaUsuariosImpl implements IEspecialistaUsuarios {

    private final IRepositorioUsuario repositorioUsuario;

    public EspecialistaUsuariosImpl() {
        this.repositorioUsuario = new RepositorioUsuarioImpl();
        System.out.println("✅ [EspecialistaUsuarios]: Inicializado con RepositorioUsuario.");
    }
    // En EspecialistaUsuariosImpl.java
    public DTOUsuario obtenerUsuarioPorIdComoDTO(UUID idUsuario) {
        System.out.println("🔍 [EspecialistaUsuarios]: Buscando usuario por ID como DTO: " + idUsuario);

        if (idUsuario == null) {
            throw new IllegalArgumentException("El ID del usuario no puede ser nulo");
        }

        Usuario usuario = repositorioUsuario.obtenerPorId(idUsuario);

        if (usuario == null) {
            System.out.println("⚠️ [EspecialistaUsuarios]: Usuario no encontrado.");
            return null;
        }

        // Conversión de Dominio -> DTO
        DTOUsuario dto = new DTOUsuario(
                usuario.getIdUsuario().toString(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getPhotoIdServidor() // o el campo que corresponda en Usuario
        );

        System.out.println("✅ [EspecialistaUsuarios]: Usuario convertido a DTO: " + usuario.getNombre());
        return dto;
    }

    @Override
    public void guardarUsuario(Usuario usuario) {
        System.out.println("💾 [EspecialistaUsuarios]: Guardando usuario: " + usuario.getNombre());
        
        // Validaciones de negocio
        if (usuario == null) {
            throw new IllegalArgumentException("El usuario no puede ser nulo");
        }
        
        if (usuario.getEmail() == null || usuario.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("El email es obligatorio");
        }
        
        if (usuario.getNombre() == null || usuario.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        
        // Verificar si ya existe un usuario con ese email
        if (repositorioUsuario.existePorEmail(usuario.getEmail())) {
            throw new IllegalStateException("Ya existe un usuario con ese email: " + usuario.getEmail());
        }
        
        repositorioUsuario.guardar(usuario);
        System.out.println("✅ [EspecialistaUsuarios]: Usuario guardado correctamente.");
    }

    @Override
    public Usuario obtenerUsuarioPorId(UUID idUsuario) {
        System.out.println("🔍 [EspecialistaUsuarios]: Buscando usuario por ID: " + idUsuario);
        
        if (idUsuario == null) {
            throw new IllegalArgumentException("El ID del usuario no puede ser nulo");
        }
        
        Usuario usuario = repositorioUsuario.obtenerPorId(idUsuario);
        
        if (usuario != null) {
            System.out.println("✅ [EspecialistaUsuarios]: Usuario encontrado: " + usuario.getNombre());
        } else {
            System.out.println("⚠️ [EspecialistaUsuarios]: Usuario no encontrado.");
        }
        
        return usuario;
    }

    @Override
    public Usuario obtenerUsuarioPorEmail(String email) {
        System.out.println("🔍 [EspecialistaUsuarios]: Buscando usuario por email: " + email);
        
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("El email no puede ser nulo o vacío");
        }
        
        Usuario usuario = repositorioUsuario.obtenerPorEmail(email);
        
        if (usuario != null) {
            System.out.println("✅ [EspecialistaUsuarios]: Usuario encontrado: " + usuario.getNombre());
        } else {
            System.out.println("⚠️ [EspecialistaUsuarios]: Usuario no encontrado.");
        }
        
        return usuario;
    }

    @Override
    public void actualizarUsuario(Usuario usuario) {
        System.out.println("🔄 [EspecialistaUsuarios]: Actualizando usuario: " + usuario.getNombre());
        
        // Validaciones de negocio
        if (usuario == null) {
            throw new IllegalArgumentException("El usuario no puede ser nulo");
        }
        
        if (usuario.getIdUsuario() == null) {
            throw new IllegalArgumentException("El ID del usuario es obligatorio para actualizar");
        }
        
        // Verificar que el usuario existe
        Usuario usuarioExistente = repositorioUsuario.obtenerPorId(usuario.getIdUsuario());
        if (usuarioExistente == null) {
            throw new IllegalStateException("No se puede actualizar. Usuario no encontrado: " + usuario.getIdUsuario());
        }
        
        repositorioUsuario.actualizar(usuario);
        System.out.println("✅ [EspecialistaUsuarios]: Usuario actualizado correctamente.");
    }

    @Override
    public void eliminarUsuario(UUID idUsuario) {
        System.out.println("🗑️ [EspecialistaUsuarios]: Eliminando usuario: " + idUsuario);
        
        if (idUsuario == null) {
            throw new IllegalArgumentException("El ID del usuario no puede ser nulo");
        }
        
        // Verificar que el usuario existe antes de eliminar
        Usuario usuarioExistente = repositorioUsuario.obtenerPorId(idUsuario);
        if (usuarioExistente == null) {
            throw new IllegalStateException("No se puede eliminar. Usuario no encontrado: " + idUsuario);
        }
        
        repositorioUsuario.eliminar(idUsuario);
        System.out.println("✅ [EspecialistaUsuarios]: Usuario eliminado correctamente.");
    }

    @Override
    public List<Usuario> obtenerTodosUsuarios() {
        System.out.println("📋 [EspecialistaUsuarios]: Obteniendo todos los usuarios.");
        
        List<Usuario> usuarios = repositorioUsuario.obtenerTodos();
        System.out.println("✅ [EspecialistaUsuarios]: Se obtuvieron " + usuarios.size() + " usuarios.");
        
        return usuarios;
    }

    @Override
    public boolean existeUsuarioPorEmail(String email) {
        System.out.println("🔍 [EspecialistaUsuarios]: Verificando existencia de email: " + email);
        
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("El email no puede ser nulo o vacío");
        }
        
        boolean existe = repositorioUsuario.existePorEmail(email);
        System.out.println("✅ [EspecialistaUsuarios]: Email " + (existe ? "existe" : "no existe"));
        
        return existe;
    }

    @Override
    public void actualizarEstadoUsuario(UUID idUsuario, String nuevoEstado) {
        System.out.println("🔄 [EspecialistaUsuarios]: Actualizando estado de usuario: " + idUsuario + " a '" + nuevoEstado + "'");

        // Validaciones de negocio
        if (idUsuario == null) {
            throw new IllegalArgumentException("El ID del usuario no puede ser nulo");
        }

        if (nuevoEstado == null || nuevoEstado.trim().isEmpty()) {
            throw new IllegalArgumentException("El nuevo estado no puede ser nulo o vacío");
        }

        // Validar que el estado sea válido
        if (!nuevoEstado.equals("activo") && !nuevoEstado.equals("inactivo") && !nuevoEstado.equals("baneado")) {
            throw new IllegalArgumentException("Estado inválido. Debe ser: 'activo', 'inactivo' o 'baneado'");
        }

        // Verificar que el usuario existe
        Usuario usuarioExistente = repositorioUsuario.obtenerPorId(idUsuario);
        if (usuarioExistente == null) {
            throw new IllegalStateException("No se puede actualizar estado. Usuario no encontrado: " + idUsuario);
        }

        repositorioUsuario.actualizarEstado(idUsuario, nuevoEstado);
        System.out.println("✅ [EspecialistaUsuarios]: Estado actualizado correctamente.");
    }
}
