package com.unillanos.server.service.impl;

import com.unillanos.server.dto.DTOUsuario;
import com.unillanos.server.repository.interfaces.IUsuarioRepository;
import com.unillanos.server.entity.EstadoUsuario;
import com.unillanos.server.entity.UsuarioEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Servicio de administración de usuarios para la GUI.
 */
@Service
public class AdminUsersService {

    private final IUsuarioRepository usuarioRepository;
    private final ConnectionManager connectionManager;
    private final LoggerService loggerService;

    public AdminUsersService(IUsuarioRepository usuarioRepository,
                             ConnectionManager connectionManager,
                             LoggerService loggerService) {
        this.usuarioRepository = usuarioRepository;
        this.connectionManager = connectionManager;
        this.loggerService = loggerService;
    }

    /**
     * Lista usuarios con paginación y filtro simple por nombre/email (case-insensitive).
     */
    public List<DTOUsuario> listUsers(String query, int limit, int offset, String estadoFilter) {
        List<UsuarioEntity> entities;
        if (estadoFilter != null && !estadoFilter.isBlank()) {
            entities = usuarioRepository.findByEstado(EstadoUsuario.fromString(estadoFilter), limit, offset);
        } else {
            entities = usuarioRepository.findAll(limit, offset);
        }
        List<DTOUsuario> mapped = new ArrayList<>();
        for (UsuarioEntity e : entities) {
            mapped.add(e.toDTO());
        }
        if (query == null || query.trim().isEmpty()) {
            return mapped;
        }
        String q = query.toLowerCase(Locale.ROOT);
        return mapped.stream()
                .filter(u -> (u.getNombre() != null && u.getNombre().toLowerCase(Locale.ROOT).contains(q))
                        || (u.getEmail() != null && u.getEmail().toLowerCase(Locale.ROOT).contains(q)))
                .collect(Collectors.toList());
    }

    /**
     * Fuerza logout de un usuario: actualiza estado OFFLINE y elimina su conexión.
     */
    public void forceLogout(String userId) {
        if (userId == null || userId.isBlank()) return;
        usuarioRepository.updateEstado(userId, EstadoUsuario.OFFLINE);
        connectionManager.removeConnection(userId);
        loggerService.logSystem("adminForceLogout", "Usuario desconectado por admin: " + userId);
    }

    /**
     * Cambia estado del usuario.
     */
    public void changeEstado(String userId, String nuevoEstado) {
        if (userId == null || userId.isBlank() || nuevoEstado == null || nuevoEstado.isBlank()) return;
        EstadoUsuario estado = EstadoUsuario.fromString(nuevoEstado);
        usuarioRepository.updateEstado(userId, estado);
        loggerService.logSystem("adminChangeEstado", "Estado cambiado a " + estado + " para: " + userId);
        if (estado == EstadoUsuario.OFFLINE) {
            connectionManager.removeConnection(userId);
        }
    }
}


