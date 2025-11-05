package fachada.gestionUsuarios.autenticacion;

import dto.vistaLogin.DTOAutenticacion;
import dto.vistaRegistro.DTORegistro;
import fachada.FachadaGeneralImpl;
import fachada.gestionArchivos.IFachadaArchivos;
import gestionUsuario.autenticacion.AutenticarUsuario;
import gestionUsuario.autenticacion.IAutenticarUsuario;
import gestionUsuario.registro.IRegistroUsuario;
import gestionUsuario.registro.RegistroUsuarioImpl;
import gestionUsuario.sesion.GestorSesionUsuario;
import gestionUsuario.especialista.EspecialistaUsuariosImpl;
import gestionUsuario.especialista.IEspecialistaUsuarios;
import observador.IObservador;
import dominio.Usuario;

import java.util.concurrent.CompletableFuture;

/**
 * Implementación de la Fachada que gestiona la autenticación y registro de usuarios.
 * Orquesta los diferentes componentes de la capa de gestión.
 */
public class FachadaAutenticacionUsuario implements IFachadaAutenticacionUsuario {

    // La Fachada ahora depende de los dos componentes de gestión.
    private final IAutenticarUsuario gestionAutenticacion;
    private final IRegistroUsuario gestionRegistro;
    private IFachadaArchivos fachadaArchivos; // ⚠️ No inicializar en constructor
    private final IEspecialistaUsuarios especialistaUsuarios;

    public FachadaAutenticacionUsuario() {
        // En una aplicación real, estas dependencias se inyectarían.
        this.gestionAutenticacion = new AutenticarUsuario();
        this.gestionRegistro = new RegistroUsuarioImpl();
        // ✅ NO inicializar fachadaArchivos aquí para evitar dependencia circular
        this.especialistaUsuarios = new EspecialistaUsuariosImpl();

        System.out.println("✅ [FachadaAutenticacionUsuario]: Inicializada con gestión de archivos integrada");
    }

    /**
     * Obtiene la instancia de FachadaArchivos de forma perezosa (lazy initialization).
     * Esto evita la dependencia circular durante la construcción.
     */
    private IFachadaArchivos getFachadaArchivos() {
        if (fachadaArchivos == null) {
            fachadaArchivos = FachadaGeneralImpl.getInstancia().getFachadaArchivos();
        }
        return fachadaArchivos;
    }

    @Override
    public CompletableFuture<Boolean> autenticarUsuario(DTOAutenticacion dto) {
        System.out.println("➡️ [FachadaAutenticacionUsuario]: Iniciando autenticación para: " + dto.getNombreUsuario());

        // Delega la autenticación a su especialista.
        return gestionAutenticacion.autenticar(dto)
            .thenCompose(exitoso -> {
                if (exitoso) {
                    // Obtener usuario autenticado de la sesión
                    Usuario usuario = GestorSesionUsuario.getInstancia().getUsuarioLogueado();

                    if (usuario != null && usuario.getPhotoIdServidor() != null && !usuario.getPhotoIdServidor().isEmpty()) {
                        String fileId = usuario.getPhotoIdServidor();
                        System.out.println("📸 [FachadaAutenticacionUsuario]: Descargando foto de perfil: " + fileId);

                        // Descargar foto de perfil automáticamente (usando getter lazy)
                        return getFachadaArchivos().obtenerArchivoPorFileId(fileId)
                            .thenApply(fotoPerfil -> {
                                System.out.println("✅ [FachadaAutenticacionUsuario]: Foto descargada: " + fotoPerfil.getAbsolutePath());

                                // Actualizar usuario con ruta local
                                usuario.setRutaFotoLocal(fotoPerfil.getAbsolutePath());
                                especialistaUsuarios.actualizarUsuario(usuario);

                                System.out.println("📢 [FachadaAutenticacionUsuario]: Foto de perfil lista para UI");
                                return exitoso;
                            })
                            .exceptionally(ex -> {
                                System.err.println("⚠️ [FachadaAutenticacionUsuario]: Error descargando foto: " + ex.getMessage());
                                System.out.println("💡 [FachadaAutenticacionUsuario]: Continuando sin foto (usar por defecto)");
                                // No falla la autenticación por esto
                                return exitoso;
                            });
                    } else {
                        System.out.println("ℹ️ [FachadaAutenticacionUsuario]: Usuario sin foto de perfil");
                        return CompletableFuture.completedFuture(exitoso);
                    }
                }
                return CompletableFuture.completedFuture(exitoso);
            });
    }

    @Override
    public CompletableFuture<Boolean> registrarUsuario(DTORegistro dto, byte[] fotoBytes) {
        // Delega el registro a su especialista.
        return gestionRegistro.registrar(dto, fotoBytes);
    }

    @Override
    public void registrarObservadorAutenticacion(IObservador observador) {
        System.out.println("🔔 [FachadaAutenticacionUsuario]: Registrando observador en Autenticación");
        gestionAutenticacion.registrarObservador(observador);
    }

    @Override
    public void registrarObservadorRegistro(IObservador observador) {
        System.out.println("🔔 [FachadaAutenticacionUsuario]: Registrando observador en Registro");
        gestionRegistro.registrarObservador(observador);
    }
}
