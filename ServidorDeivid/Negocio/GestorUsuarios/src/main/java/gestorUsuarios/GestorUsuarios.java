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

    // ✅ NUEVO: ID del peer local para asignarlo automáticamente a nuevos usuarios
    private UUID peerLocalId;

    public GestorUsuarios() {
        this.repositorio = new UsuarioRepositorio();
        this.observadores = new ArrayList<>();
        LoggerCentral.info(TAG, "GestorUsuarios inicializado");
    }

    /**
     * ✅ NUEVO: Configura el ID del peer local que se asignará automáticamente
     * a los usuarios creados.
     */
    public void setPeerLocalId(UUID peerLocalId) {
        this.peerLocalId = peerLocalId;
        LoggerCentral.info(TAG, "Peer local configurado: " + peerLocalId);
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

        // ✅ ESTABLECER PEER PADRE AUTOMÁTICAMENTE
        // Prioridad: 1) DTO, 2) Peer Local configurado, 3) null
        if (dto.getPeerPadreId() != null && !dto.getPeerPadreId().isEmpty()) {
            try {
                usuario.setPeerPadre(UUID.fromString(dto.getPeerPadreId()));
                LoggerCentral.debug(TAG, "Peer padre asignado desde DTO: " + dto.getPeerPadreId());
            } catch (IllegalArgumentException e) {
                LoggerCentral.warn(TAG, "PeerPadreId inválido en DTO: " + dto.getPeerPadreId());
            }
        } else if (peerLocalId != null) {
            // Asignar automáticamente el peer local como padre
            usuario.setPeerPadre(peerLocalId);
            LoggerCentral.info(TAG, "✅ Peer padre asignado automáticamente (peer local): " + peerLocalId);
        } else {
            LoggerCentral.warn(TAG, "⚠️ No se pudo asignar peer padre: ni en DTO ni peer local configurado");
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

        // ✅ NUEVO: Variable para detectar si hubo cambios
        boolean huboCambios = false;

        // Actualizar campos si se proporcionan
        if (dto.getNombre() != null && !dto.getNombre().trim().isEmpty()) {
            if (!usuario.getNombre().equals(dto.getNombre())) {
                usuario.setNombre(dto.getNombre());
                huboCambios = true;
            }
        }
        if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
            if (!validarEmail(dto.getEmail())) {
                throw new IllegalArgumentException("El email no es válido");
            }
            if (!usuario.getEmail().equals(dto.getEmail())) {
                usuario.setEmail(dto.getEmail());
                huboCambios = true;
            }
        }
        if (dto.getFoto() != null) {
            if (!dto.getFoto().equals(usuario.getFoto())) {
                usuario.setFoto(dto.getFoto());
                huboCambios = true;
            }
        }
        if (dto.getContrasena() != null && !dto.getContrasena().trim().isEmpty()) {
            if (!dto.getContrasena().equals(usuario.getContrasena())) {
                usuario.setContrasena(dto.getContrasena()); // TODO: Encriptar
                huboCambios = true;
            }
        }
        if (dto.getEstado() != null) {
            try {
                Usuario.Estado nuevoEstado = Usuario.Estado.valueOf(dto.getEstado().toUpperCase());
                if (usuario.getEstado() != nuevoEstado) {
                    usuario.setEstado(nuevoEstado);
                    huboCambios = true;
                }
            } catch (IllegalArgumentException e) {
                LoggerCentral.warn(TAG, "Estado inválido: " + dto.getEstado());
            }
        }

        // ✅ CRÍTICO: Actualizar timestamp solo si hubo cambios reales
        if (huboCambios) {
            usuario.setFechaCreacion(Instant.now());
            LoggerCentral.info(TAG, "⏰ Timestamp actualizado por modificación: " + usuario.getFechaCreacion());
        } else {
            LoggerCentral.debug(TAG, "No hubo cambios en el usuario, timestamp sin modificar");
        }

        // Persistir cambios
        boolean actualizado = repositorio.guardar(usuario);
        if (!actualizado) {
            throw new RuntimeException("Error al actualizar el usuario en la base de datos");
        }

        LoggerCentral.info(TAG, "Usuario actualizado exitosamente: " + usuario.getId());

        // Notificar a observadores solo si hubo cambios
        if (huboCambios) {
            notificarObservadores(EVENTO_USUARIO_ACTUALIZADO, usuario);
        }

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
     * ✅ ACTUALIZADO: Ahora actualiza timestamp y notifica correctamente
     */
    public boolean cambiarEstado(String id, Usuario.Estado nuevoEstado) {
        LoggerCentral.info(TAG, "Cambiando estado de usuario " + id + " a " + nuevoEstado);

        try {
            UUID uuid = UUID.fromString(id);

            // ✅ MEJORADO: actualizarEstado() ya actualiza el timestamp automáticamente
            boolean actualizado = repositorio.actualizarEstado(uuid, nuevoEstado);

            if (actualizado) {
                // ✅ IMPORTANTE: Recargar usuario con timestamp actualizado
                Usuario usuario = repositorio.buscarPorId(uuid);
                if (usuario != null) {
                    LoggerCentral.info(TAG, "✅ Estado actualizado con timestamp: " + usuario.getFechaCreacion());
                    // Notificar a observadores (activa sincronización P2P)
                    notificarObservadores(EVENTO_USUARIO_ACTUALIZADO, usuario);
                } else {
                    LoggerCentral.warn(TAG, "⚠️ Usuario actualizado pero no se pudo recargar para notificar");
                }
            }

            return actualizado;
        } catch (IllegalArgumentException e) {
            LoggerCentral.error(TAG, "ID de usuario inválido: " + id);
            return false;
        }
    }

    /**
     * ✅ NUEVO: Registra un archivo en la base de datos
     * @param fileId ID del archivo (ruta relativa desde Bucket/)
     * @param nombreOriginal Nombre original del archivo
     * @param mimeType Tipo MIME del archivo
     * @param tamanio Tamaño en bytes
     * @param hash Hash SHA-256 del archivo
     * @return true si se registró correctamente
     */
    public boolean registrarArchivo(String fileId, String nombreOriginal, String mimeType, long tamanio, String hash) {
        try {
            LoggerCentral.info(TAG, "📝 Registrando archivo en repositorio: " + fileId);

            // Crear entidad Archivo
            dominio.clienteServidor.Archivo archivo = new dominio.clienteServidor.Archivo(
                fileId, nombreOriginal, mimeType, tamanio
            );
            archivo.setRutaRelativa(fileId);
            archivo.setHashSHA256(hash);
            archivo.setFechaUltimaActualizacion(java.time.Instant.now());

            // Guardar en repositorio
            repositorio.clienteServidor.ArchivoRepositorio repoArchivo =
                new repositorio.clienteServidor.ArchivoRepositorio();
            boolean guardado = repoArchivo.guardar(archivo);

            if (guardado) {
                LoggerCentral.info(TAG, "✅ Archivo registrado en BD: " + fileId);
                notificarObservadores("ARCHIVO_REGISTRADO", fileId);
            } else {
                LoggerCentral.error(TAG, "❌ Error al guardar archivo en BD: " + fileId);
            }

            return guardado;
        } catch (Exception e) {
            LoggerCentral.error(TAG, "❌ Error al registrar archivo: " + e.getMessage());
            e.printStackTrace();
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
