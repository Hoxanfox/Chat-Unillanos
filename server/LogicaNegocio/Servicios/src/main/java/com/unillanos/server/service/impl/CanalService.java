package com.unillanos.server.service.impl;

import com.google.gson.Gson;
import com.unillanos.server.dto.*;
import com.unillanos.server.exception.DuplicateResourceException;
import com.unillanos.server.exception.NotFoundException;
import com.unillanos.server.exception.ValidationException;
import com.unillanos.server.repository.interfaces.ICanalMiembroRepository;
import com.unillanos.server.repository.interfaces.ICanalRepository;
import com.unillanos.server.repository.interfaces.IUsuarioRepository;
import com.unillanos.server.repository.models.*;
import com.unillanos.server.validation.CrearCanalValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio de gestión de canales.
 * Maneja creación, unión, salida, listado y gestión de miembros de canales.
 */
@Service
public class CanalService {
    
    private static final Logger logger = LoggerFactory.getLogger(CanalService.class);
    
    private final ICanalRepository canalRepository;
    private final ICanalMiembroRepository canalMiembroRepository;
    private final IUsuarioRepository usuarioRepository;
    private final LoggerService loggerService;
    private final ConnectionManager connectionManager;
    private final Gson gson;

    public CanalService(ICanalRepository canalRepository,
                        ICanalMiembroRepository canalMiembroRepository,
                        IUsuarioRepository usuarioRepository,
                        LoggerService loggerService,
                        ConnectionManager connectionManager) {
        this.canalRepository = canalRepository;
        this.canalMiembroRepository = canalMiembroRepository;
        this.usuarioRepository = usuarioRepository;
        this.loggerService = loggerService;
        this.connectionManager = connectionManager;
        this.gson = new Gson();
    }

    /**
     * Crea un nuevo canal.
     *
     * @param dto Datos del canal a crear
     * @return DTOResponse con el canal creado
     */
    public DTOResponse crearCanal(DTOCrearCanal dto) {
        try {
            // 1. Validar datos con CrearCanalValidator
            CrearCanalValidator.validate(dto);
            
            // 2. Verificar que el creador existe
            UsuarioEntity creador = usuarioRepository.findById(dto.getCreadorId())
                .orElseThrow(() -> new NotFoundException("Usuario creador", dto.getCreadorId()));
            
            // 3. Verificar que el nombre del canal no exista
            if (canalRepository.existsByNombre(dto.getNombre().trim())) {
                throw new DuplicateResourceException("Canal", "nombre", dto.getNombre().trim());
            }
            
            // 4. Crear CanalEntity con ID UUID y activo = true
            CanalEntity canal = new CanalEntity();
            canal.setId(UUID.randomUUID().toString());
            canal.setNombre(dto.getNombre().trim());
            canal.setDescripcion(dto.getDescripcion() != null ? dto.getDescripcion().trim() : null);
            canal.setCreadorId(dto.getCreadorId());
            canal.setFechaCreacion(LocalDateTime.now());
            canal.setActivo(true);
            
            // 5. Guardar canal en BD
            CanalEntity canalGuardado = canalRepository.save(canal);
            
            // 6. Agregar al creador como ADMIN en canal_miembros
            canalMiembroRepository.agregarMiembro(canalGuardado.getId(), dto.getCreadorId(), RolCanal.ADMIN);
            
            // 7. Registrar en logs
            loggerService.logInfo("crearCanal", 
                String.format("Canal creado: %s por usuario %s", canalGuardado.getNombre(), creador.getNombre()));
            
            // 8. Retornar DTOResponse.success con DTOCanal
            DTOCanal dtoCanal = canalGuardado.toDTO(1); // 1 miembro (el creador)
            logger.info("Canal creado exitosamente: {} ({})", dtoCanal.getNombre(), dtoCanal.getId());
            
            return DTOResponse.success("crearCanal", "Canal creado exitosamente", dtoCanal);
            
        } catch (ValidationException | DuplicateResourceException | NotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error inesperado en creación de canal", e);
            throw new RuntimeException("Error al crear canal", e);
        }
    }

    /**
     * Permite a un usuario unirse a un canal.
     *
     * @param dto Datos de unión al canal
     * @return DTOResponse confirmando la unión
     */
    public DTOResponse unirseCanal(DTOUnirseCanal dto) {
        try {
            // 1. Validar que usuarioId y canalId no estén vacíos
            if (dto.getUsuarioId() == null || dto.getUsuarioId().trim().isEmpty()) {
                throw new ValidationException("El ID del usuario es requerido", "usuarioId");
            }
            if (dto.getCanalId() == null || dto.getCanalId().trim().isEmpty()) {
                throw new ValidationException("El ID del canal es requerido", "canalId");
            }
            
            // 2. Verificar que el usuario existe
            UsuarioEntity usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new NotFoundException("Usuario", dto.getUsuarioId()));
            
            // 3. Verificar que el canal existe y está activo
            CanalEntity canal = canalRepository.findById(dto.getCanalId())
                .orElseThrow(() -> new NotFoundException("Canal", dto.getCanalId()));
            
            if (!canal.isActivo()) {
                throw new ValidationException("El canal no está activo", "canalId");
            }
            
            // 4. Verificar que el usuario NO sea ya miembro
            if (canalMiembroRepository.esMiembro(dto.getUsuarioId(), dto.getCanalId())) {
                throw new ValidationException("Ya eres miembro de este canal", "usuarioId");
            }
            
            // 5. Agregar usuario al canal con rol MEMBER
            canalMiembroRepository.agregarMiembro(dto.getCanalId(), dto.getUsuarioId(), RolCanal.MEMBER);
            
            // 6. Registrar en logs
            loggerService.logInfo("unirseCanal", 
                String.format("Usuario %s se unió al canal %s", usuario.getNombre(), canal.getNombre()));
            
            // 7. Notificar a todos los miembros del canal sobre el nuevo miembro
            notificarMiembrosCanal(dto.getCanalId(), "nuevoMiembro", 
                String.format("%s se ha unido al canal", usuario.getNombre()), 
                Map.of("usuarioId", usuario.getId(), "nombreUsuario", usuario.getNombre()));
            
            // 8. Retornar DTOResponse.success
            logger.info("Usuario {} se unió al canal {}", usuario.getNombre(), canal.getNombre());
            return DTOResponse.success("unirseCanal", "Te has unido al canal exitosamente", null);
            
        } catch (ValidationException | NotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error inesperado al unirse a canal", e);
            throw new RuntimeException("Error al unirse al canal", e);
        }
    }

    /**
     * Permite a un usuario salir de un canal.
     *
     * @param dto Datos de salida del canal
     * @return DTOResponse confirmando la salida
     */
    public DTOResponse salirCanal(DTOSalirCanal dto) {
        try {
            // 1. Validar que usuarioId y canalId no estén vacíos
            if (dto.getUsuarioId() == null || dto.getUsuarioId().trim().isEmpty()) {
                throw new ValidationException("El ID del usuario es requerido", "usuarioId");
            }
            if (dto.getCanalId() == null || dto.getCanalId().trim().isEmpty()) {
                throw new ValidationException("El ID del canal es requerido", "canalId");
            }
            
            // 2. Verificar que el usuario es miembro del canal
            if (!canalMiembroRepository.esMiembro(dto.getUsuarioId(), dto.getCanalId())) {
                throw new ValidationException("No eres miembro de este canal", "usuarioId");
            }
            
            UsuarioEntity usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new NotFoundException("Usuario", dto.getUsuarioId()));
            
            CanalEntity canal = canalRepository.findById(dto.getCanalId())
                .orElseThrow(() -> new NotFoundException("Canal", dto.getCanalId()));
            
            // 3. Si el usuario es el creador Y el único admin, no permitir salida
            if (canal.getCreadorId().equals(dto.getUsuarioId())) {
                // Contar cuántos admins hay
                List<CanalMiembroEntity> miembros = canalMiembroRepository.findMiembrosByCanal(dto.getCanalId());
                long adminCount = miembros.stream().filter(m -> m.getRol() == RolCanal.ADMIN).count();
                
                if (adminCount == 1) {
                    throw new ValidationException(
                        "No puedes salir del canal porque eres el único administrador. Asigna otro administrador primero.", 
                        "usuarioId"
                    );
                }
            }
            
            // 4. Remover usuario del canal
            canalMiembroRepository.removerMiembro(dto.getCanalId(), dto.getUsuarioId());
            
            // 5. Registrar en logs
            loggerService.logInfo("salirCanal", 
                String.format("Usuario %s salió del canal %s", usuario.getNombre(), canal.getNombre()));
            
            // 6. Notificar a miembros del canal
            notificarMiembrosCanal(dto.getCanalId(), "miembroSalio", 
                String.format("%s ha salido del canal", usuario.getNombre()), 
                Map.of("usuarioId", usuario.getId(), "nombreUsuario", usuario.getNombre()));
            
            // 7. Retornar DTOResponse.success
            logger.info("Usuario {} salió del canal {}", usuario.getNombre(), canal.getNombre());
            return DTOResponse.success("salirCanal", "Has salido del canal exitosamente", null);
            
        } catch (ValidationException | NotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error inesperado al salir del canal", e);
            throw new RuntimeException("Error al salir del canal", e);
        }
    }

    /**
     * Lista todos los canales o los canales de un usuario específico.
     *
     * @param dto Parámetros de listado
     * @return DTOResponse con lista de canales
     */
    public DTOResponse listarCanales(DTOListarCanales dto) {
        try {
            List<CanalEntity> canales;
            
            // 1 y 2. Si usuarioId está presente, verificar que existe y obtener sus canales
            if (dto.getUsuarioId() != null && !dto.getUsuarioId().trim().isEmpty()) {
                usuarioRepository.findById(dto.getUsuarioId())
                    .orElseThrow(() -> new NotFoundException("Usuario", dto.getUsuarioId()));
                
                canales = canalRepository.findByUsuario(dto.getUsuarioId(), dto.getLimit(), dto.getOffset());
            } else {
                // 3. Si usuarioId NO está presente, obtener todos los canales activos
                canales = canalRepository.findAll(dto.getLimit(), dto.getOffset());
            }
            
            // 4 y 5. Para cada canal, obtener cantidad de miembros y convertir a DTO
            List<DTOCanal> dtoCanales = canales.stream()
                .map(canal -> {
                    int cantidadMiembros = canalRepository.countMiembros(canal.getId());
                    return canal.toDTO(cantidadMiembros);
                })
                .collect(Collectors.toList());
            
            // 6. Retornar DTOResponse.success con lista de DTOCanal
            logger.debug("Listados {} canales", dtoCanales.size());
            return DTOResponse.success("listarCanales", "Canales listados exitosamente", dtoCanales);
            
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error inesperado al listar canales", e);
            throw new RuntimeException("Error al listar canales", e);
        }
    }

    /**
     * Lista los miembros de un canal específico.
     *
     * @param canalId ID del canal
     * @param solicitanteId ID del usuario que solicita la información
     * @return DTOResponse con lista de miembros
     */
    public DTOResponse listarMiembros(String canalId, String solicitanteId) {
        try {
            // 1. Validar que canalId y solicitanteId no estén vacíos
            if (canalId == null || canalId.trim().isEmpty()) {
                throw new ValidationException("El ID del canal es requerido", "canalId");
            }
            if (solicitanteId == null || solicitanteId.trim().isEmpty()) {
                throw new ValidationException("El ID del solicitante es requerido", "solicitanteId");
            }
            
            // 2. Verificar que el canal existe
            canalRepository.findById(canalId)
                .orElseThrow(() -> new NotFoundException("Canal", canalId));
            
            // 3. Verificar que el solicitante es miembro del canal
            if (!canalMiembroRepository.esMiembro(solicitanteId, canalId)) {
                throw new ValidationException("Debes ser miembro del canal para ver los miembros", "solicitanteId");
            }
            
            // 4. Obtener lista de miembros del canal
            List<CanalMiembroEntity> miembros = canalMiembroRepository.findMiembrosByCanal(canalId);
            
            // 5 y 6. Para cada miembro, obtener información del usuario y crear DTOMiembroCanal
            List<DTOMiembroCanal> dtoMiembros = miembros.stream()
                .map(miembro -> {
                    Optional<UsuarioEntity> usuarioOpt = usuarioRepository.findById(miembro.getUsuarioId());
                    if (usuarioOpt.isPresent()) {
                        UsuarioEntity usuario = usuarioOpt.get();
                        return new DTOMiembroCanal(
                            usuario.getId(),
                            usuario.getNombre(),
                            miembro.getRol().name(),
                            miembro.getFechaUnion() != null ? miembro.getFechaUnion().toString() : null
                        );
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            
            // 7. Retornar DTOResponse.success con lista de DTOMiembroCanal
            logger.debug("Listados {} miembros del canal {}", dtoMiembros.size(), canalId);
            return DTOResponse.success("listarMiembros", "Miembros listados exitosamente", dtoMiembros);
            
        } catch (ValidationException | NotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error inesperado al listar miembros", e);
            throw new RuntimeException("Error al listar miembros", e);
        }
    }

    /**
     * Gestiona miembros de un canal (agregar, remover, cambiar rol).
     * Solo puede ser ejecutado por un administrador del canal.
     *
     * @param dto Datos de gestión de miembro
     * @return DTOResponse confirmando la acción
     */
    public DTOResponse gestionarMiembro(DTOGestionarMiembro dto) {
        try {
            // 1. Validar que todos los campos requeridos estén presentes
            if (dto.getAdminId() == null || dto.getAdminId().trim().isEmpty()) {
                throw new ValidationException("El ID del administrador es requerido", "adminId");
            }
            if (dto.getCanalId() == null || dto.getCanalId().trim().isEmpty()) {
                throw new ValidationException("El ID del canal es requerido", "canalId");
            }
            if (dto.getUsuarioId() == null || dto.getUsuarioId().trim().isEmpty()) {
                throw new ValidationException("El ID del usuario es requerido", "usuarioId");
            }
            if (dto.getAccion() == null || dto.getAccion().trim().isEmpty()) {
                throw new ValidationException("La acción es requerida", "accion");
            }
            
            // 2. Verificar que el admin existe y es administrador del canal
            UsuarioEntity admin = usuarioRepository.findById(dto.getAdminId())
                .orElseThrow(() -> new NotFoundException("Usuario administrador", dto.getAdminId()));
            
            if (!canalMiembroRepository.esAdministrador(dto.getAdminId(), dto.getCanalId())) {
                throw new ValidationException("No tienes permisos de administrador en este canal", "adminId");
            }
            
            // 3. Verificar que el canal existe
            CanalEntity canal = canalRepository.findById(dto.getCanalId())
                .orElseThrow(() -> new NotFoundException("Canal", dto.getCanalId()));
            
            // 4. Verificar que el usuario objetivo existe
            UsuarioEntity usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new NotFoundException("Usuario", dto.getUsuarioId()));
            
            // 5. Según la acción
            String mensaje = "";
            String accionLog = "";
            
            switch (dto.getAccion().toUpperCase()) {
                case "AGREGAR":
                    // Agregar usuario como MEMBER si no es miembro
                    if (canalMiembroRepository.esMiembro(dto.getUsuarioId(), dto.getCanalId())) {
                        throw new ValidationException("El usuario ya es miembro del canal", "usuarioId");
                    }
                    canalMiembroRepository.agregarMiembro(dto.getCanalId(), dto.getUsuarioId(), RolCanal.MEMBER);
                    mensaje = String.format("%s ha sido agregado al canal", usuario.getNombre());
                    accionLog = "Miembro agregado";
                    break;
                    
                case "REMOVER":
                    // Remover usuario del canal (no permitir si es creador)
                    if (canal.getCreadorId().equals(dto.getUsuarioId())) {
                        throw new ValidationException("No puedes remover al creador del canal", "usuarioId");
                    }
                    if (!canalMiembroRepository.esMiembro(dto.getUsuarioId(), dto.getCanalId())) {
                        throw new ValidationException("El usuario no es miembro del canal", "usuarioId");
                    }
                    canalMiembroRepository.removerMiembro(dto.getCanalId(), dto.getUsuarioId());
                    mensaje = String.format("%s ha sido removido del canal", usuario.getNombre());
                    accionLog = "Miembro removido";
                    break;
                    
                case "CAMBIAR_ROL":
                    // Actualizar rol del usuario
                    if (dto.getNuevoRol() == null || dto.getNuevoRol().trim().isEmpty()) {
                        throw new ValidationException("El nuevo rol es requerido para cambiar rol", "nuevoRol");
                    }
                    if (!canalMiembroRepository.esMiembro(dto.getUsuarioId(), dto.getCanalId())) {
                        throw new ValidationException("El usuario no es miembro del canal", "usuarioId");
                    }
                    RolCanal nuevoRol = RolCanal.fromString(dto.getNuevoRol());
                    canalMiembroRepository.actualizarRol(dto.getCanalId(), dto.getUsuarioId(), nuevoRol);
                    mensaje = String.format("Rol de %s actualizado a %s", usuario.getNombre(), nuevoRol.name());
                    accionLog = "Rol cambiado";
                    break;
                    
                default:
                    throw new ValidationException("Acción inválida. Debe ser AGREGAR, REMOVER o CAMBIAR_ROL", "accion");
            }
            
            // 6. Registrar en logs
            loggerService.logInfo("gestionarMiembro", 
                String.format("%s: %s en canal %s por admin %s", 
                    accionLog, usuario.getNombre(), canal.getNombre(), admin.getNombre()));
            
            // 7. Notificar al usuario afectado
            if (connectionManager.isUserOnline(dto.getUsuarioId())) {
                DTOResponse notificacion = DTOResponse.success("notificacionCanal", mensaje, 
                    Map.of("canalId", canal.getId(), "canalNombre", canal.getNombre(), "accion", dto.getAccion()));
                connectionManager.notifyUser(dto.getUsuarioId(), notificacion);
            }
            
            // 8. Notificar a todos los miembros del canal
            notificarMiembrosCanal(dto.getCanalId(), "cambioMiembro", mensaje, 
                Map.of("usuarioId", usuario.getId(), "nombreUsuario", usuario.getNombre(), "accion", dto.getAccion()));
            
            // 9. Retornar DTOResponse.success
            logger.info("Gestión de miembro exitosa: {} - {}", dto.getAccion(), mensaje);
            return DTOResponse.success("gestionarMiembro", mensaje, null);
            
        } catch (ValidationException | NotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error inesperado en gestión de miembro", e);
            throw new RuntimeException("Error al gestionar miembro", e);
        }
    }

    // --- MÉTODOS AUXILIARES ---

    /**
     * Notifica a todos los miembros de un canal.
     *
     * @param canalId ID del canal
     * @param accion Acción que se está notificando
     * @param mensaje Mensaje descriptivo
     * @param data Datos adicionales
     */
    private void notificarMiembrosCanal(String canalId, String accion, String mensaje, Map<String, String> data) {
        try {
            List<CanalMiembroEntity> miembros = canalMiembroRepository.findMiembrosByCanal(canalId);
            Set<String> idsUsuarios = miembros.stream()
                .map(CanalMiembroEntity::getUsuarioId)
                .collect(Collectors.toSet());
            
            DTOResponse notificacion = DTOResponse.success(accion, mensaje, data);
            connectionManager.notifyChannel(canalId, notificacion, idsUsuarios);
        } catch (Exception e) {
            logger.warn("Error al notificar miembros del canal {}: {}", canalId, e.getMessage());
        }
    }
}

