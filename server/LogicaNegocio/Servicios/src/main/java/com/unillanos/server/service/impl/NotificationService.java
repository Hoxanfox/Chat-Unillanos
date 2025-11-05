package com.unillanos.server.service.impl;

import com.unillanos.server.dto.DTOResponse;
import com.unillanos.server.dto.response.DTONotificacionResponse;
import com.unillanos.server.dto.response.DTOResponseNotificaciones;
import com.unillanos.server.entity.NotificacionEntity;
import com.unillanos.server.entity.UsuarioEntity;
import com.unillanos.server.exception.NotFoundException;
import com.unillanos.server.exception.ValidationException;
import com.unillanos.server.repository.interfaces.INotificacionRepository;
import com.unillanos.server.repository.interfaces.IUsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de notificaciones.
 */
@Service
public class NotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    
    private final INotificacionRepository notificacionRepository;
    private final IUsuarioRepository usuarioRepository;

    public NotificationService(INotificacionRepository notificacionRepository, IUsuarioRepository usuarioRepository) {
        this.notificacionRepository = notificacionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Obtiene todas las notificaciones de un usuario.
     */
    public DTOResponse obtenerNotificaciones(String usuarioId) {
        try {
            // Validar que el usuario existe
            if (!usuarioRepository.existsById(usuarioId)) {
                throw new NotFoundException("Usuario no encontrado", "USER_NOT_FOUND");
            }

            // Obtener notificaciones
            List<NotificacionEntity> notificaciones = notificacionRepository.findByUsuarioId(usuarioId);
            int totalNoLeidas = notificacionRepository.countNoLeidasByUsuarioId(usuarioId);

            // Convertir a DTOs de respuesta
            List<DTONotificacionResponse> notificacionesDTO = notificaciones.stream()
                .map(notificacion -> {
                    String nombreRemitente = obtenerNombreUsuario(notificacion.getRemitenteId());
                    String nombreCanal = obtenerNombreCanal(notificacion.getCanalId());
                    return notificacion.toDTO(nombreRemitente, nombreCanal);
                })
                .collect(Collectors.toList());

            // Crear respuesta estructurada
            DTOResponseNotificaciones response = new DTOResponseNotificaciones(
                notificacionesDTO,
                totalNoLeidas,
                notificaciones.size()
            );

            logger.info("Notificaciones obtenidas para usuario {}: {} total, {} no leídas", 
                usuarioId, notificaciones.size(), totalNoLeidas);

            return DTOResponse.success("obtenerNotificaciones", 
                String.format("Notificaciones obtenidas: %d total, %d no leídas", 
                    notificaciones.size(), totalNoLeidas), 
                response);

        } catch (NotFoundException e) {
            logger.warn("Error al obtener notificaciones: {}", e.getMessage());
            return DTOResponse.error("obtenerNotificaciones", e.getMessage());
        } catch (Exception e) {
            logger.error("Error inesperado al obtener notificaciones", e);
            return DTOResponse.error("obtenerNotificaciones", "Error interno del servidor");
        }
    }

    /**
     * Marca una notificación como leída.
     */
    public DTOResponse marcarNotificacionLeida(String notificacionId) {
        try {
            // Validar que la notificación existe
            if (notificacionRepository.findById(notificacionId).isEmpty()) {
                throw new NotFoundException("Notificación no encontrada", "NOTIFICATION_NOT_FOUND");
            }

            // Marcar como leída
            notificacionRepository.marcarComoLeida(notificacionId);

            logger.info("Notificación marcada como leída: {}", notificacionId);

            return DTOResponse.success("marcarNotificacionLeida", 
                "Notificación marcada como leída", null);

        } catch (NotFoundException e) {
            logger.warn("Error al marcar notificación como leída: {}", e.getMessage());
            return DTOResponse.error("marcarNotificacionLeida", e.getMessage());
        } catch (Exception e) {
            logger.error("Error inesperado al marcar notificación como leída", e);
            return DTOResponse.error("marcarNotificacionLeida", "Error interno del servidor");
        }
    }

    /**
     * Crea una nueva notificación.
     */
    public void crearNotificacion(String usuarioId, String tipo, String titulo, String mensaje,
                                 String remitenteId, String canalId, String accion) {
        try {
            NotificacionEntity notificacion = new NotificacionEntity();
            notificacion.setId(UUID.randomUUID().toString());
            notificacion.setUsuarioId(usuarioId);
            notificacion.setTipo(tipo);
            notificacion.setTitulo(titulo);
            notificacion.setMensaje(mensaje);
            notificacion.setRemitenteId(remitenteId);
            notificacion.setCanalId(canalId);
            notificacion.setLeida(false);
            notificacion.setTimestamp(LocalDateTime.now());
            notificacion.setAccion(accion);
            notificacion.setMetadata(null);

            notificacionRepository.save(notificacion);

            logger.debug("Notificación creada: {} para usuario {}", notificacion.getId(), usuarioId);

        } catch (Exception e) {
            logger.error("Error al crear notificación", e);
            // No lanzar excepción para no interrumpir el flujo principal
        }
    }

    /**
     * Crea una notificación de solicitud de amistad.
     */
    public void crearNotificacionSolicitudAmistad(String destinatarioId, String remitenteId, String nombreRemitente) {
        String titulo = "Nueva solicitud de amistad";
        String mensaje = String.format("%s te ha enviado una solicitud de amistad", nombreRemitente);
        
        crearNotificacion(destinatarioId, "SOLICITUD_AMISTAD", titulo, mensaje, 
                         remitenteId, null, "responder");
    }

    /**
     * Crea una notificación de invitación a canal.
     */
    public void crearNotificacionInvitacionCanal(String destinatarioId, String remitenteId, 
                                                String nombreRemitente, String canalId, String nombreCanal) {
        String titulo = "Invitación a canal";
        String mensaje = String.format("%s te ha invitado al canal %s", nombreRemitente, nombreCanal);
        
        crearNotificacion(destinatarioId, "INVITACION_CANAL", titulo, mensaje, 
                         remitenteId, canalId, "responder");
    }

    private String obtenerNombreUsuario(String usuarioId) {
        if (usuarioId == null) return null;
        return usuarioRepository.findById(usuarioId)
            .map(UsuarioEntity::getNombre)
            .orElse("Usuario desconocido");
    }

    private String obtenerNombreCanal(String canalId) {
        // TODO: Implementar cuando esté disponible CanalRepository
        if (canalId == null) return null;
        return "Canal"; // Placeholder por ahora
    }
}
