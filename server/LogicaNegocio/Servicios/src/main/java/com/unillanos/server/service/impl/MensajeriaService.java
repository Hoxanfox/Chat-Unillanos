package com.unillanos.server.service.impl;

import com.google.gson.Gson;
import com.unillanos.server.dto.*;
import com.unillanos.server.exception.AuthenticationException;
import com.unillanos.server.exception.NotFoundException;
import com.unillanos.server.exception.ValidationException;
import com.unillanos.server.repository.interfaces.ICanalMiembroRepository;
import com.unillanos.server.repository.interfaces.ICanalRepository;
import com.unillanos.server.repository.interfaces.IMensajeRepository;
import com.unillanos.server.repository.interfaces.IUsuarioRepository;
import com.unillanos.server.repository.interfaces.IArchivoRepository;
import com.unillanos.server.entity.*;
import com.unillanos.server.validation.EnviarMensajeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Servicio de mensajería en tiempo real.
 * Gestiona el envío de mensajes directos y de canal, así como el historial.
 */
@Service
public class MensajeriaService {
    
    private static final Logger logger = LoggerFactory.getLogger(MensajeriaService.class);
    
    private final IMensajeRepository mensajeRepository;
    private final IUsuarioRepository usuarioRepository;
    private final ICanalMiembroRepository canalMiembroRepository;
    private final ICanalRepository canalRepository;
    private final LoggerService loggerService;
    private final IArchivoRepository archivoRepository;
    private final ConnectionManager connectionManager;
    private final Gson gson;

    public MensajeriaService(IMensajeRepository mensajeRepository,
                            IUsuarioRepository usuarioRepository,
                            ICanalMiembroRepository canalMiembroRepository,
                            ICanalRepository canalRepository,
                            LoggerService loggerService,
                            ConnectionManager connectionManager,
                            IArchivoRepository archivoRepository) {
        this.mensajeRepository = mensajeRepository;
        this.usuarioRepository = usuarioRepository;
        this.canalMiembroRepository = canalMiembroRepository;
        this.canalRepository = canalRepository;
        this.loggerService = loggerService;
        this.connectionManager = connectionManager;
        this.archivoRepository = archivoRepository;
        this.gson = new Gson();
    }

    /**
     * Envía un mensaje directo de un usuario a otro.
     * 
     * @param dto Datos del mensaje a enviar
     * @return DTOResponse con el mensaje enviado
     */
    public DTOResponse enviarMensajeDirecto(DTOEnviarMensaje dto) {
        try {
            logger.info("Enviando mensaje directo de {} a {}", dto.getRemitenteId(), dto.getDestinatarioId());
            
            // 1. Validar datos con EnviarMensajeValidator
            EnviarMensajeValidator.validate(dto);
            
            // 2. Verificar que el remitente existe
            Optional<UsuarioEntity> remitenteOpt = usuarioRepository.findById(dto.getRemitenteId());
            if (remitenteOpt.isEmpty()) {
                throw new NotFoundException("Remitente no encontrado", "REMITENTE_NOT_FOUND");
            }
            UsuarioEntity remitente = remitenteOpt.get();
            
            // 3. Verificar que el destinatario existe
            Optional<UsuarioEntity> destinatarioOpt = usuarioRepository.findById(dto.getDestinatarioId());
            if (destinatarioOpt.isEmpty()) {
                throw new NotFoundException("Destinatario no encontrado", "DESTINATARIO_NOT_FOUND");
            }
            UsuarioEntity destinatario = destinatarioOpt.get();
            
            // 4. Validar archivo adjunto si existe
            String fileName = null;
            if (dto.getFileId() != null && !dto.getFileId().trim().isEmpty()) {
                var archivoOpt = archivoRepository.findById(dto.getFileId());
                if (archivoOpt.isEmpty()) {
                    throw new NotFoundException("Archivo no encontrado", "FILE_NOT_FOUND");
                }
                var archivo = archivoOpt.get();
                if (!archivo.getUsuarioId().equals(dto.getRemitenteId())) {
                    throw new AuthenticationException("No tienes permisos para adjuntar este archivo", "FILE_NOT_OWNED");
                }
                fileName = archivo.getNombreOriginal();
            }

            // 5. Crear MensajeEntity con tipo DIRECT
            MensajeEntity mensaje = new MensajeEntity();
            mensaje.setRemitenteId(dto.getRemitenteId());
            mensaje.setDestinatarioId(dto.getDestinatarioId());
            mensaje.setCanalId(null);  // No es mensaje de canal
            mensaje.setTipo(TipoMensaje.DIRECT);
            mensaje.setContenido(dto.getContenido());
            mensaje.setFileId(dto.getFileId());
            mensaje.setFechaEnvio(LocalDateTime.now());
            mensaje.setEstado(EstadoMensaje.ENVIADO); // Estado inicial
            
            // 6. Guardar mensaje en BD (recuperar ID generado)
            MensajeEntity mensajeGuardado = mensajeRepository.save(mensaje);
            
            // 7. Registrar en logs
            loggerService.logInfo("enviarMensajeDirecto", 
                String.format("Usuario %s envió mensaje a %s. ID: %d", 
                    remitente.getNombre(), destinatario.getNombre(), mensajeGuardado.getId()));
            
            // 8. Si destinatario está online, notificar en tiempo real
            if (connectionManager.isUserOnline(dto.getDestinatarioId())) {
                // Marcar como entregado
                mensajeRepository.marcarComoEntregado(mensajeGuardado.getId());
                
                DTOMensaje notificacionMensaje = mensajeGuardado.toDTO(
                    remitente.getNombre(),
                    destinatario.getNombre(),
                    null,
                    fileName
                );
                DTOResponse notificacion = DTOResponse.success(
                    "nuevoMensajeDirecto",
                    "Nuevo mensaje directo recibido",
                    notificacionMensaje
                );
                connectionManager.notifyUser(dto.getDestinatarioId(), notificacion);
                logger.debug("Notificación enviada al destinatario {} y mensaje marcado como entregado", destinatario.getNombre());
            }
            
            // 9. Retornar DTOResponse.success con DTOMensaje
            DTOMensaje mensajeDTO = mensajeGuardado.toDTO(
                remitente.getNombre(),
                destinatario.getNombre(),
                null,
                fileName
            );
            
            return DTOResponse.success("enviarMensajeDirecto", "Mensaje enviado exitosamente", mensajeDTO);
            
        } catch (ValidationException | NotFoundException e) {
            logger.error("Error al enviar mensaje directo: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error inesperado al enviar mensaje directo", e);
            throw new RuntimeException("Error al enviar mensaje directo", e);
        }
    }

    /**
     * Envía un mensaje a un canal.
     * 
     * @param dto Datos del mensaje a enviar
     * @return DTOResponse con el mensaje enviado
     */
    public DTOResponse enviarMensajeCanal(DTOEnviarMensaje dto) {
        try {
            logger.info("Enviando mensaje al canal {} por usuario {}", dto.getCanalId(), dto.getRemitenteId());
            
            // 1. Validar datos con EnviarMensajeValidator
            EnviarMensajeValidator.validate(dto);
            
            // 2. Verificar que el remitente existe
            Optional<UsuarioEntity> remitenteOpt = usuarioRepository.findById(dto.getRemitenteId());
            if (remitenteOpt.isEmpty()) {
                throw new NotFoundException("Remitente no encontrado", "REMITENTE_NOT_FOUND");
            }
            UsuarioEntity remitente = remitenteOpt.get();
            
            // 3. Verificar que el canal existe y está activo
            Optional<CanalEntity> canalOpt = canalRepository.findById(dto.getCanalId());
            if (canalOpt.isEmpty()) {
                throw new NotFoundException("Canal no encontrado", "CANAL_NOT_FOUND");
            }
            CanalEntity canal = canalOpt.get();
            
            if (!canal.isActivo()) {
                throw new ValidationException("El canal no está activo", "CANAL_INACTIVE");
            }
            
            // 4. Verificar que el remitente es miembro del canal
            if (!canalMiembroRepository.esMiembro(dto.getRemitenteId(), dto.getCanalId())) {
                throw new AuthenticationException(
                    "No eres miembro de este canal",
                    "NOT_CHANNEL_MEMBER"
                );
            }
            
            // 5. Crear MensajeEntity con tipo CHANNEL
            MensajeEntity mensaje = new MensajeEntity();
            mensaje.setRemitenteId(dto.getRemitenteId());
            mensaje.setDestinatarioId(null);  // No es mensaje directo
            mensaje.setCanalId(dto.getCanalId());
            mensaje.setTipo(TipoMensaje.CHANNEL);
            mensaje.setContenido(dto.getContenido());
            mensaje.setFileId(dto.getFileId());
            mensaje.setFechaEnvio(LocalDateTime.now());
            
            // 6. Guardar mensaje en BD (recuperar ID generado)
            MensajeEntity mensajeGuardado = mensajeRepository.save(mensaje);
            
            // 4. Validar archivo adjunto si existe
            String fileName = null;
            if (dto.getFileId() != null && !dto.getFileId().trim().isEmpty()) {
                var archivoOpt = archivoRepository.findById(dto.getFileId());
                if (archivoOpt.isEmpty()) {
                    throw new NotFoundException("Archivo no encontrado", "FILE_NOT_FOUND");
                }
                var archivo = archivoOpt.get();
                if (!archivo.getUsuarioId().equals(dto.getRemitenteId())) {
                    throw new AuthenticationException("No tienes permisos para adjuntar este archivo", "FILE_NOT_OWNED");
                }
                fileName = archivo.getNombreOriginal();
            }

            // 7. Registrar en logs
            loggerService.logInfo("enviarMensajeCanal", 
                String.format("Usuario %s envió mensaje al canal %s. ID: %d", 
                    remitente.getNombre(), canal.getNombre(), mensajeGuardado.getId()));
            
            // 8. Notificar a todos los miembros del canal (excepto remitente)
            List<CanalMiembroEntity> miembros = canalMiembroRepository.findMiembrosByCanal(dto.getCanalId());
            Set<String> miembrosIds = miembros.stream()
                .map(CanalMiembroEntity::getUsuarioId)
                .filter(id -> !id.equals(dto.getRemitenteId()))  // Excluir al remitente
                .collect(Collectors.toSet());
            
            if (!miembrosIds.isEmpty()) {
                DTOMensaje notificacionMensaje = mensajeGuardado.toDTO(
                    remitente.getNombre(),
                    null,  // No hay destinatario específico
                    canal.getNombre(),
                    fileName
                );
                DTOResponse notificacion = DTOResponse.success(
                    "nuevoMensajeCanal",
                    "Nuevo mensaje en el canal " + canal.getNombre(),
                    notificacionMensaje
                );
                connectionManager.notifyChannel(dto.getCanalId(), notificacion, miembrosIds);
                logger.debug("Notificación enviada a {} miembros del canal {}", miembrosIds.size(), canal.getNombre());
            }
            
            // 9. Retornar DTOResponse.success con DTOMensaje
            DTOMensaje mensajeDTO = mensajeGuardado.toDTO(
                remitente.getNombre(),
                null,
                canal.getNombre(),
                fileName
            );
            
            return DTOResponse.success("enviarMensajeCanal", "Mensaje enviado al canal exitosamente", mensajeDTO);
            
        } catch (ValidationException | NotFoundException | AuthenticationException e) {
            logger.error("Error al enviar mensaje al canal: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error inesperado al enviar mensaje al canal", e);
            throw new RuntimeException("Error al enviar mensaje al canal", e);
        }
    }

    /**
     * Obtiene el historial de mensajes (directos o de canal).
     * 
     * @param dto Parámetros de consulta de historial
     * @return DTOResponse con lista de mensajes
     */
    public DTOResponse obtenerHistorial(DTOHistorial dto) {
        try {
            logger.info("Obteniendo historial para usuario {}", dto.getUsuarioId());
            
            // 1. Validar que usuarioId esté presente
            if (dto.getUsuarioId() == null || dto.getUsuarioId().trim().isEmpty()) {
                throw new ValidationException("El ID del usuario es requerido", "usuarioId");
            }
            
            // 2. Validar que haya EXACTAMENTE uno de destinatarioId o canalId
            boolean tieneDestinatario = dto.getDestinatarioId() != null && !dto.getDestinatarioId().trim().isEmpty();
            boolean tieneCanal = dto.getCanalId() != null && !dto.getCanalId().trim().isEmpty();
            
            if (!tieneDestinatario && !tieneCanal) {
                throw new ValidationException(
                    "Debe especificar un destinatario o un canal para el historial",
                    "destinatario"
                );
            }
            
            if (tieneDestinatario && tieneCanal) {
                throw new ValidationException(
                    "No puede obtener historial de destinatario y canal simultáneamente",
                    "destinatario"
                );
            }
            
            // Validar límites de paginación
            int limit = dto.getLimit() > 0 && dto.getLimit() <= 100 ? dto.getLimit() : 50;
            int offset = dto.getOffset() >= 0 ? dto.getOffset() : 0;
            
            List<MensajeEntity> mensajes;
            List<DTOMensaje> mensajesDTO = new ArrayList<>();
            
            // 3. Si es historial directo:
            if (tieneDestinatario) {
                // Verificar que ambos usuarios existen
                Optional<UsuarioEntity> usuarioOpt = usuarioRepository.findById(dto.getUsuarioId());
                if (usuarioOpt.isEmpty()) {
                    throw new NotFoundException("Usuario no encontrado", "USER_NOT_FOUND");
                }
                
                Optional<UsuarioEntity> destinatarioOpt = usuarioRepository.findById(dto.getDestinatarioId());
                if (destinatarioOpt.isEmpty()) {
                    throw new NotFoundException("Destinatario no encontrado", "DESTINATARIO_NOT_FOUND");
                }
                
                // Obtener mensajes entre los dos usuarios
                mensajes = mensajeRepository.findMensajesDirectos(
                    dto.getUsuarioId(),
                    dto.getDestinatarioId(),
                    limit,
                    offset
                );
                
                // Convertir a DTOs
                UsuarioEntity usuario = usuarioOpt.get();
                UsuarioEntity destinatario = destinatarioOpt.get();
                
                for (MensajeEntity mensaje : mensajes) {
                    String remitenteNombre = mensaje.getRemitenteId().equals(usuario.getId()) ? 
                        usuario.getNombre() : destinatario.getNombre();
                    String destinatarioNombre = mensaje.getDestinatarioId().equals(usuario.getId()) ? 
                        usuario.getNombre() : destinatario.getNombre();
                    
                    mensajesDTO.add(mensaje.toDTO(remitenteNombre, destinatarioNombre, null, null));
                }
                
                logger.info("Historial directo obtenido: {} mensajes entre {} y {}", 
                    mensajes.size(), usuario.getNombre(), destinatario.getNombre());
            }
            // 4. Si es historial de canal:
            else {
                // Verificar que el canal existe
                Optional<CanalEntity> canalOpt = canalRepository.findById(dto.getCanalId());
                if (canalOpt.isEmpty()) {
                    throw new NotFoundException("Canal no encontrado", "CANAL_NOT_FOUND");
                }
                CanalEntity canal = canalOpt.get();
                
                // Verificar que el usuario es miembro del canal
                if (!canalMiembroRepository.esMiembro(dto.getUsuarioId(), dto.getCanalId())) {
                    throw new AuthenticationException(
                        "No eres miembro de este canal",
                        "NOT_CHANNEL_MEMBER"
                    );
                }
                
                // Obtener mensajes del canal
                mensajes = mensajeRepository.findMensajesCanal(
                    dto.getCanalId(),
                    limit,
                    offset
                );
                
                // Convertir a DTOs
                for (MensajeEntity mensaje : mensajes) {
                    // Obtener nombre del remitente
                    Optional<UsuarioEntity> remitenteOpt = usuarioRepository.findById(mensaje.getRemitenteId());
                    String remitenteNombre = remitenteOpt.isPresent() ? 
                        remitenteOpt.get().getNombre() : "Usuario desconocido";
                    
                    mensajesDTO.add(mensaje.toDTO(remitenteNombre, null, canal.getNombre(), null));
                }
                
                logger.info("Historial de canal obtenido: {} mensajes del canal {}", 
                    mensajes.size(), canal.getNombre());
            }
            
            // 7. Retornar DTOResponse.success con lista de DTOMensaje
            return DTOResponse.success("obtenerHistorial", 
                String.format("Historial obtenido: %d mensajes", mensajesDTO.size()), 
                mensajesDTO);
            
        } catch (ValidationException | NotFoundException | AuthenticationException e) {
            logger.error("Error al obtener historial: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error inesperado al obtener historial", e);
            throw new RuntimeException("Error al obtener historial", e);
        }
    }

    /**
     * Marca un mensaje como leído por el destinatario.
     *
     * @param dto DTO con el ID del mensaje y el usuario que lo lee
     * @return DTOResponse con el resultado de la operación
     */
    public DTOResponse marcarComoLeido(DTOMarcarMensajeLeido dto) {
        try {
            // Validaciones básicas
            if (dto == null) {
                throw new ValidationException("Los datos para marcar como leído son requeridos", "dto");
            }
            if (dto.getMensajeId() == null || dto.getMensajeId().trim().isEmpty()) {
                throw new ValidationException("El ID del mensaje es requerido", "mensajeId");
            }
            if (dto.getUsuarioId() == null || dto.getUsuarioId().trim().isEmpty()) {
                throw new ValidationException("El ID del usuario es requerido", "usuarioId");
            }

            Long mensajeId;
            try {
                mensajeId = Long.parseLong(dto.getMensajeId());
            } catch (NumberFormatException e) {
                throw new ValidationException("El ID del mensaje debe ser un número válido", "mensajeId");
            }

            // Verificar que el mensaje existe
            Optional<MensajeEntity> mensajeOpt = mensajeRepository.findById(mensajeId);
            if (mensajeOpt.isEmpty()) {
                throw new NotFoundException("Mensaje no encontrado", "MESSAGE_NOT_FOUND");
            }

            MensajeEntity mensaje = mensajeOpt.get();

            // Verificar que el usuario es el destinatario
            if (!mensaje.getDestinatarioId().equals(dto.getUsuarioId())) {
                throw new AuthenticationException("No tienes autorización para marcar este mensaje como leído", "UNAUTHORIZED_MESSAGE_READ");
            }

            // Verificar que el mensaje no esté ya leído
            if (mensaje.getEstado() == EstadoMensaje.LEIDO) {
                logger.info("Mensaje {} ya estaba marcado como leído", mensajeId);
                return DTOResponse.success("marcar_leido", "Mensaje ya estaba marcado como leído", null);
            }

            // Marcar como leído en la base de datos
            mensajeRepository.marcarComoLeido(mensajeId, dto.getUsuarioId());

            // Notificar al remitente del cambio de estado si está conectado
            if (connectionManager.isUserOnline(mensaje.getRemitenteId())) {
                DTOEstadoMensaje estadoNotificacion = new DTOEstadoMensaje(
                    dto.getMensajeId(),
                    EstadoMensaje.LEIDO.toString(),
                    mensaje.getFechaEnvio().toString(),
                    mensaje.getFechaEntrega() != null ? mensaje.getFechaEntrega().toString() : null,
                    LocalDateTime.now().toString()
                );

                DTOResponse notificacion = DTOResponse.success("mensaje_leido", 
                    "Tu mensaje ha sido leído", estadoNotificacion);

                connectionManager.notifyUser(mensaje.getRemitenteId(), notificacion);
            }

            logger.info("Mensaje {} marcado como leído por usuario {}", mensajeId, dto.getUsuarioId());
            return DTOResponse.success("marcar_leido", "Mensaje marcado como leído exitosamente", null);

        } catch (ValidationException | NotFoundException | AuthenticationException e) {
            logger.warn("Error al marcar mensaje como leído: {}", e.getMessage());
            return DTOResponse.error("marcar_leido", e.getMessage());
        } catch (Exception e) {
            logger.error("Error inesperado al marcar mensaje como leído", e);
            return DTOResponse.error("marcar_leido", "Error inesperado al marcar el mensaje como leído");
        }
    }
}

