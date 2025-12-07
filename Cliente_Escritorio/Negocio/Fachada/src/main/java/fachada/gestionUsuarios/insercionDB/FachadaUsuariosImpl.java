package fachada.gestionUsuarios.insercionDB;

import dominio.Usuario;
import dto.vistaLobby.DTOUsuario;
import gestionUsuario.especialista.EspecialistaUsuariosImpl;
import gestionUsuario.especialista.IEspecialistaUsuarios;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Implementación de la fachada de usuarios.
 * Coordina las operaciones llamando al especialista de usuarios.
 */
public class FachadaUsuariosImpl implements IFachadaUsuarios {

    private final IEspecialistaUsuarios especialistaUsuarios;

    public FachadaUsuariosImpl() {
        this.especialistaUsuarios = new EspecialistaUsuariosImpl();
        System.out.println("✅ [FachadaUsuarios]: Inicializada con EspecialistaUsuarios.");
    }

    @Override
    public CompletableFuture<DTOUsuario> obtenerUsuarioPorId(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("🔍 [FachadaUsuarios]: Buscando usuario por ID: " + userId);
            
            try {
                UUID id = UUID.fromString(userId);
                Usuario usuario = especialistaUsuarios.obtenerUsuarioPorId(id);

                if (usuario == null) {
                    System.out.println("⚠️ [FachadaUsuarios]: Usuario no encontrado.");
                    return null;
                }

                // Convertir Usuario (dominio) a DTOUsuario (para la vista)
                DTOUsuario dtoUsuario = new DTOUsuario(
                    usuario.getIdUsuario().toString(),
                    usuario.getNombre(),
                    usuario.getEmail(),
                    usuario.getPhotoIdServidor()
                );

                System.out.println("✅ [FachadaUsuarios]: Usuario encontrado: " + dtoUsuario.getNombre());
                return dtoUsuario;

            } catch (IllegalArgumentException e) {
                System.err.println("❌ [FachadaUsuarios]: ID de usuario inválido: " + userId);
                return null;
            } catch (Exception e) {
                System.err.println("❌ [FachadaUsuarios]: Error al obtener usuario: " + e.getMessage());
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<DTOUsuario> obtenerUsuarioPorEmail(String email) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("🔍 [FachadaUsuarios]: Buscando usuario por email: " + email);
            
            try {
                Usuario usuario = especialistaUsuarios.obtenerUsuarioPorEmail(email);

                if (usuario == null) {
                    System.out.println("⚠️ [FachadaUsuarios]: Usuario no encontrado.");
                    return null;
                }

                // Convertir Usuario (dominio) a DTOUsuario (para la vista)
                return new DTOUsuario(
                    usuario.getIdUsuario().toString(),
                    usuario.getNombre(),
                    usuario.getEmail(),
                    usuario.getPhotoIdServidor()
                );

            } catch (Exception e) {
                System.err.println("❌ [FachadaUsuarios]: Error al obtener usuario: " + e.getMessage());
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<Void> guardarUsuario(DTOUsuario dtoUsuario) {
        return CompletableFuture.runAsync(() -> {
            System.out.println("💾 [FachadaUsuarios]: Guardando usuario: " + dtoUsuario.getNombre());
            
            try {
                // Convertir DTOUsuario a Usuario (dominio)
                Usuario usuario = new Usuario();
                usuario.setIdUsuario(UUID.fromString(dtoUsuario.getId()));
                usuario.setNombre(dtoUsuario.getNombre());
                usuario.setEmail(dtoUsuario.getEmail());
                usuario.setEstado("activo");
                usuario.setPhotoIdServidor(dtoUsuario.getAvatarUrl());
                usuario.setFechaRegistro(LocalDateTime.now());

                especialistaUsuarios.guardarUsuario(usuario);
                System.out.println("✅ [FachadaUsuarios]: Usuario guardado correctamente.");

            } catch (Exception e) {
                System.err.println("❌ [FachadaUsuarios]: Error al guardar usuario: " + e.getMessage());
                throw new RuntimeException("Error al guardar usuario", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> actualizarUsuario(DTOUsuario dtoUsuario) {
        return CompletableFuture.runAsync(() -> {
            System.out.println("🔄 [FachadaUsuarios]: Actualizando usuario: " + dtoUsuario.getNombre());
            
            try {
                // Convertir DTOUsuario a Usuario (dominio)
                Usuario usuario = new Usuario();
                usuario.setIdUsuario(UUID.fromString(dtoUsuario.getId()));
                usuario.setNombre(dtoUsuario.getNombre());
                usuario.setEmail(dtoUsuario.getEmail());
                usuario.setEstado("activo");
                usuario.setPhotoIdServidor(dtoUsuario.getAvatarUrl());

                especialistaUsuarios.actualizarUsuario(usuario);
                System.out.println("✅ [FachadaUsuarios]: Usuario actualizado correctamente.");

            } catch (Exception e) {
                System.err.println("❌ [FachadaUsuarios]: Error al actualizar usuario: " + e.getMessage());
                throw new RuntimeException("Error al actualizar usuario", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> eliminarUsuario(String userId) {
        return CompletableFuture.runAsync(() -> {
            System.out.println("🗑️ [FachadaUsuarios]: Eliminando usuario: " + userId);

            try {
                UUID id = UUID.fromString(userId);
                especialistaUsuarios.eliminarUsuario(id);
                System.out.println("✅ [FachadaUsuarios]: Usuario eliminado correctamente.");

            } catch (IllegalArgumentException e) {
                System.err.println("❌ [FachadaUsuarios]: ID de usuario inválido: " + userId);
                throw new RuntimeException("ID de usuario inválido", e);
            } catch (Exception e) {
                System.err.println("❌ [FachadaUsuarios]: Error al eliminar usuario: " + e.getMessage());
                throw new RuntimeException("Error al eliminar usuario", e);
            }
        });
    }
}

