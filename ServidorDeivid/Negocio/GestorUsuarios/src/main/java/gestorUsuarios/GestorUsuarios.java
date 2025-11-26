package gestorUsuarios;

import dominio.clienteServidor.Usuario;
import dto.usuario.DTOActualizarUsuario;
import dto.usuario.DTOCrearUsuario;
import dto.vista.DTOUsuarioVista;
import logger.LoggerCentral;
import observador.IObservador;
import repositorio.clienteServidor.UsuarioRepositorio;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Gestor de Usuarios - Capa de Negocio
 * Maneja la lógica de negocio relacionada con usuarios
 * y notifica cambios a los observadores (sincronización P2P)
 */
public class GestorUsuarios {

    private static final String TAG = "GestorUsuarios";
    private static final String EVENTO_USUARIO_CREADO = "USUARIO_CREADO";
    private static final String EVENTO_USUARIO_ACTUALIZADO = "USUARIO_ACTUALIZADO";
    private static final String EVENTO_USUARIO_ELIMINADO = "USUARIO_ELIMINADO";

    private final UsuarioRepositorio repositorio;
    private final List<IObservador> observadores;
    private static final DateTimeFormatter formatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    public GestorUsuarios() {
        this.repositorio = new UsuarioRepositorio();
        this.observadores = new ArrayList<>();
        LoggerCentral.info(TAG, "GestorUsuarios inicializado");
    }

    /**
     * Registra un observador para cambios en usuarios
     */
    public void registrarObservador(IObservador obs) {
        if (!observadores.contains(obs)) {
            observadores.add(obs);
            LoggerCentral.debug(TAG, "Observador registrado: " + obs.getClass().getSimpleName());
        }
    }

    /**
     * Elimina un observador
     */
    public void removerObservador(IObservador obs) {
        observadores.remove(obs);
        LoggerCentral.debug(TAG, "Observador removido: " + obs.getClass().getSimpleName());
    }

    /**
     * Notifica a todos los observadores registrados
     */
    private void notificarObservadores(String evento, Object datos) {
        for (IObservador obs : observadores) {
            try {
                obs.actualizar(evento, datos);
            } catch (Exception e) {
                LoggerCentral.error(TAG, "Error notificando observador: " + e.getMessage());
            }
        }
    }

    /**
     * Crea un nuevo usuario en el sistema
     */
    public DTOUsuarioVista crearUsuario(DTOCrearUsuario dto) {
        LoggerCentral.info(TAG, "Creando usuario: " + dto.getNombre());

        // Validaciones
        if (dto.getNombre() == null || dto.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del usuario es obligatorio");
        }
        if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("El email del usuario es obligatorio");
        }
        if (!validarEmail(dto.getEmail())) {
            throw new IllegalArgumentException("El email no es válido");
        }
        if (dto.getContrasena() == null || dto.getContrasena().trim().isEmpty()) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }

        // Verificar si ya existe un usuario con ese email
        Usuario existente = repositorio.buscarPorEmail(dto.getEmail());
        if (existente != null) {
            throw new IllegalArgumentException("Ya existe un usuario con ese email");
        }

        // Crear entidad de dominio
        Usuario usuario = new Usuario();
        usuario.setNombre(dto.getNombre());
        usuario.setEmail(dto.getEmail());
        usuario.setContrasena(dto.getContrasena()); // TODO: Encriptar
        usuario.setFoto(dto.getFoto());
        usuario.setEstado(Usuario.Estado.OFFLINE);
        usuario.setFechaCreacion(Instant.now());

        // Establecer peer padre si se proporciona
        if (dto.getPeerPadreId() != null && !dto.getPeerPadreId().isEmpty()) {
            try {
                usuario.setPeerPadre(UUID.fromString(dto.getPeerPadreId()));
            } catch (IllegalArgumentException e) {
                LoggerCentral.warn(TAG, "PeerPadreId inválido: " + dto.getPeerPadreId());
            }
        }

        // Persistir
        boolean guardado = repositorio.guardar(usuario);
        if (!guardado) {
            throw new RuntimeException("Error al guardar el usuario en la base de datos");
        }

        LoggerCentral.info(TAG, "Usuario creado exitosamente: " + usuario.getId());

        // Notificar a observadores (esto activará la sincronización P2P)
        notificarObservadores(EVENTO_USUARIO_CREADO, usuario);

        // Retornar DTO para la vista
        return convertirADTOVista(usuario);
    }

    /**
     * Actualiza un usuario existente
     */
    public DTOUsuarioVista actualizarUsuario(DTOActualizarUsuario dto) {
        LoggerCentral.info(TAG, "Actualizando usuario: " + dto.getId());

        if (dto.getId() == null || dto.getId().isEmpty()) {
            throw new IllegalArgumentException("El ID del usuario es obligatorio");
        }

        // Buscar usuario existente
        Usuario usuario = repositorio.buscarPorId(dto.getId());
        if (usuario == null) {
            throw new IllegalArgumentException("No existe un usuario con ID: " + dto.getId());
        }

        // Actualizar campos si se proporcionan
        if (dto.getNombre() != null && !dto.getNombre().trim().isEmpty()) {
            usuario.setNombre(dto.getNombre());
        }
        if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
            if (!validarEmail(dto.getEmail())) {
                throw new IllegalArgumentException("El email no es válido");
            }
            usuario.setEmail(dto.getEmail());
        }
        if (dto.getFoto() != null) {
            usuario.setFoto(dto.getFoto());
        }
        if (dto.getContrasena() != null && !dto.getContrasena().trim().isEmpty()) {
            usuario.setContrasena(dto.getContrasena()); // TODO: Encriptar
        }
        if (dto.getEstado() != null) {
            try {
                usuario.setEstado(Usuario.Estado.valueOf(dto.getEstado().toUpperCase()));
            } catch (IllegalArgumentException e) {
                LoggerCentral.warn(TAG, "Estado inválido: " + dto.getEstado());
            }
        }

        // Persistir cambios
        boolean actualizado = repositorio.guardar(usuario);
        if (!actualizado) {
            throw new RuntimeException("Error al actualizar el usuario en la base de datos");
        }

        LoggerCentral.info(TAG, "Usuario actualizado exitosamente: " + usuario.getId());

        // Notificar a observadores
        notificarObservadores(EVENTO_USUARIO_ACTUALIZADO, usuario);

        return convertirADTOVista(usuario);
    }

    /**
     * Obtiene todos los usuarios del sistema
     */
    public List<DTOUsuarioVista> listarUsuarios() {
        LoggerCentral.debug(TAG, "Listando todos los usuarios");
        List<Usuario> usuarios = repositorio.obtenerTodosParaSync();
        List<DTOUsuarioVista> dtos = new ArrayList<>();

        for (Usuario u : usuarios) {
            dtos.add(convertirADTOVista(u));
        }

        LoggerCentral.debug(TAG, "Total usuarios encontrados: " + dtos.size());
        return dtos;
    }

    /**
     * Busca un usuario por su ID
     */
    public DTOUsuarioVista buscarPorId(String id) {
        LoggerCentral.debug(TAG, "Buscando usuario por ID: " + id);
        Usuario usuario = repositorio.buscarPorId(id);
        return usuario != null ? convertirADTOVista(usuario) : null;
    }

    /**
     * Busca un usuario por su email
     */
    public DTOUsuarioVista buscarPorEmail(String email) {
        LoggerCentral.debug(TAG, "Buscando usuario por email: " + email);
        Usuario usuario = repositorio.buscarPorEmail(email);
        return usuario != null ? convertirADTOVista(usuario) : null;
    }

    /**
     * Cambia el estado de un usuario (ONLINE/OFFLINE)
     */
    public boolean cambiarEstado(String id, Usuario.Estado nuevoEstado) {
        LoggerCentral.info(TAG, "Cambiando estado de usuario " + id + " a " + nuevoEstado);

        try {
            UUID uuid = UUID.fromString(id);
            boolean actualizado = repositorio.actualizarEstado(uuid, nuevoEstado);

            if (actualizado) {
                // Notificar a observadores
                Usuario usuario = repositorio.buscarPorId(uuid);
                if (usuario != null) {
                    notificarObservadores(EVENTO_USUARIO_ACTUALIZADO, usuario);
                }
            }

            return actualizado;
        } catch (IllegalArgumentException e) {
            LoggerCentral.error(TAG, "ID de usuario inválido: " + id);
            return false;
        }
    }

    // --- Métodos auxiliares ---

    private DTOUsuarioVista convertirADTOVista(Usuario usuario) {
        String fechaFormateada = formatter.format(usuario.getFechaCreacion());
        String peerPadreId = usuario.getPeerPadre() != null ?
            usuario.getPeerPadre().toString() : null;

        return new DTOUsuarioVista(
            usuario.getId().toString(),
            usuario.getNombre(),
            usuario.getEmail(),
            usuario.getEstado().name(),
            fechaFormateada,
            peerPadreId
        );
    }

    private boolean validarEmail(String email) {
        // Validación simple de email
        return email.contains("@") && email.contains(".");
    }
}
