package com.unillanos.server.service.impl;

import com.unillanos.server.dto.DTOResponse;
import com.unillanos.server.entity.ContactoEntity;
import com.unillanos.server.entity.UsuarioEntity;
import com.unillanos.server.exception.NotFoundException;
import com.unillanos.server.exception.ValidationException;
import com.unillanos.server.repository.interfaces.IContactoRepository;
import com.unillanos.server.repository.interfaces.IUsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Servicio para gestión de contactos/amistades.
 */
@Service
public class ContactService {
    
    private static final Logger logger = LoggerFactory.getLogger(ContactService.class);
    
    private final IContactoRepository contactoRepository;
    private final IUsuarioRepository usuarioRepository;
    private final NotificationService notificationService;

    public ContactService(IContactoRepository contactoRepository, IUsuarioRepository usuarioRepository, NotificationService notificationService) {
        this.contactoRepository = contactoRepository;
        this.usuarioRepository = usuarioRepository;
        this.notificationService = notificationService;
    }

    /**
     * Envía una solicitud de amistad.
     */
    public DTOResponse enviarSolicitudAmistad(String remitenteId, String destinatarioId) {
        try {
            // Validaciones
            if (remitenteId.equals(destinatarioId)) {
                throw new ValidationException("No puedes enviarte una solicitud a ti mismo", "destinatarioId");
            }

            // Verificar que ambos usuarios existen
            UsuarioEntity remitente = usuarioRepository.findById(remitenteId)
                .orElseThrow(() -> new NotFoundException("Usuario remitente no encontrado", "USER_NOT_FOUND"));
            
            UsuarioEntity destinatario = usuarioRepository.findById(destinatarioId)
                .orElseThrow(() -> new NotFoundException("Usuario destinatario no encontrado", "USER_NOT_FOUND"));

            // Verificar que no son ya contactos
            if (contactoRepository.sonContactos(remitenteId, destinatarioId)) {
                throw new ValidationException("Ya son contactos", "RELATIONSHIP_EXISTS");
            }

            // Verificar que no hay solicitud pendiente
            if (contactoRepository.tieneSolicitudPendiente(remitenteId, destinatarioId)) {
                throw new ValidationException("Ya existe una solicitud pendiente", "PENDING_REQUEST");
            }

            // Crear solicitud
            ContactoEntity solicitud = new ContactoEntity();
            solicitud.setId(UUID.randomUUID().toString());
            solicitud.setUsuarioId(remitenteId);
            solicitud.setContactoId(destinatarioId);
            solicitud.setEstado("PENDIENTE");
            solicitud.setFechaSolicitud(LocalDateTime.now());
            solicitud.setSolicitadoPor("usuario");

            contactoRepository.save(solicitud);

            // Crear notificación
            notificationService.crearNotificacionSolicitudAmistad(destinatarioId, remitenteId, remitente.getNombre());

            logger.info("Solicitud de amistad enviada: {} -> {}", remitente.getNombre(), destinatario.getNombre());

            return DTOResponse.success("enviarSolicitudAmistad", 
                "Solicitud de amistad enviada exitosamente", null);

        } catch (ValidationException | NotFoundException e) {
            logger.warn("Error al enviar solicitud de amistad: {}", e.getMessage());
            return DTOResponse.error("enviarSolicitudAmistad", e.getMessage());
        } catch (Exception e) {
            logger.error("Error inesperado al enviar solicitud de amistad", e);
            return DTOResponse.error("enviarSolicitudAmistad", "Error interno del servidor");
        }
    }

    /**
     * Responde a una solicitud de amistad.
     */
    public DTOResponse responderSolicitudAmistad(String solicitudId, String usuarioId, boolean aceptar) {
        try {
            // Buscar la solicitud
            ContactoEntity solicitud = contactoRepository.findById(solicitudId)
                .orElseThrow(() -> new NotFoundException("Solicitud no encontrada", "REQUEST_NOT_FOUND"));

            // Verificar que el usuario es el destinatario
            if (!solicitud.getContactoId().equals(usuarioId)) {
                throw new ValidationException("No tienes permisos para responder esta solicitud", "UNAUTHORIZED");
            }

            // Verificar que está pendiente
            if (!solicitud.isPendiente()) {
                throw new ValidationException("Esta solicitud ya fue respondida", "ALREADY_RESPONDED");
            }

            // Actualizar estado
            String nuevoEstado = aceptar ? "ACEPTADO" : "RECHAZADO";
            contactoRepository.actualizarEstado(solicitudId, nuevoEstado);

            // Si se acepta, crear la relación inversa
            if (aceptar) {
                ContactoEntity relacionInversa = new ContactoEntity();
                relacionInversa.setId(UUID.randomUUID().toString());
                relacionInversa.setUsuarioId(solicitud.getContactoId());
                relacionInversa.setContactoId(solicitud.getUsuarioId());
                relacionInversa.setEstado("ACEPTADO");
                relacionInversa.setFechaSolicitud(LocalDateTime.now());
                relacionInversa.setFechaRespuesta(LocalDateTime.now());
                relacionInversa.setSolicitadoPor("contacto");

                contactoRepository.save(relacionInversa);
            }

            String mensaje = aceptar ? "Solicitud de amistad aceptada" : "Solicitud de amistad rechazada";
            logger.info("Solicitud de amistad respondida: {} - {}", solicitudId, mensaje);

            return DTOResponse.success("responderSolicitudAmistad", mensaje, null);

        } catch (ValidationException | NotFoundException e) {
            logger.warn("Error al responder solicitud de amistad: {}", e.getMessage());
            return DTOResponse.error("responderSolicitudAmistad", e.getMessage());
        } catch (Exception e) {
            logger.error("Error inesperado al responder solicitud de amistad", e);
            return DTOResponse.error("responderSolicitudAmistad", "Error interno del servidor");
        }
    }

    /**
     * Obtiene la lista de contactos de un usuario.
     */
    public DTOResponse obtenerContactos(String usuarioId) {
        try {
            // Verificar que el usuario existe
            if (!usuarioRepository.existsById(usuarioId)) {
                throw new NotFoundException("Usuario no encontrado", "USER_NOT_FOUND");
            }

            // Obtener contactos activos
            List<ContactoEntity> contactos = contactoRepository.findContactosActivos(usuarioId);

            logger.info("Contactos obtenidos para usuario {}: {} contactos", usuarioId, contactos.size());

            return DTOResponse.success("obtenerContactos", 
                String.format("Contactos obtenidos: %d contactos", contactos.size()), 
                contactos);

        } catch (NotFoundException e) {
            logger.warn("Error al obtener contactos: {}", e.getMessage());
            return DTOResponse.error("obtenerContactos", e.getMessage());
        } catch (Exception e) {
            logger.error("Error inesperado al obtener contactos", e);
            return DTOResponse.error("obtenerContactos", "Error interno del servidor");
        }
    }

    /**
     * Obtiene solicitudes pendientes de un usuario.
     */
    public DTOResponse obtenerSolicitudesPendientes(String usuarioId) {
        try {
            // Verificar que el usuario existe
            if (!usuarioRepository.existsById(usuarioId)) {
                throw new NotFoundException("Usuario no encontrado", "USER_NOT_FOUND");
            }

            // Obtener solicitudes recibidas
            List<ContactoEntity> solicitudes = contactoRepository.findSolicitudesRecibidas(usuarioId);

            logger.info("Solicitudes pendientes obtenidas para usuario {}: {} solicitudes", usuarioId, solicitudes.size());

            return DTOResponse.success("obtenerSolicitudesPendientes", 
                String.format("Solicitudes pendientes: %d solicitudes", solicitudes.size()), 
                solicitudes);

        } catch (NotFoundException e) {
            logger.warn("Error al obtener solicitudes pendientes: {}", e.getMessage());
            return DTOResponse.error("obtenerSolicitudesPendientes", e.getMessage());
        } catch (Exception e) {
            logger.error("Error inesperado al obtener solicitudes pendientes", e);
            return DTOResponse.error("obtenerSolicitudesPendientes", "Error interno del servidor");
        }
    }
}
