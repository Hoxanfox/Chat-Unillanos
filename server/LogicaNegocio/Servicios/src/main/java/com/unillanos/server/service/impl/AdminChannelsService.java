package com.unillanos.server.service.impl;

import com.unillanos.server.dto.DTOCanal;
import com.unillanos.server.dto.DTOMiembroCanal;
import com.unillanos.server.repository.interfaces.ICanalMiembroRepository;
import com.unillanos.server.repository.interfaces.ICanalRepository;
import com.unillanos.server.repository.interfaces.IUsuarioRepository;
import com.unillanos.server.entity.CanalEntity;
import com.unillanos.server.entity.CanalMiembroEntity;
import com.unillanos.server.entity.RolCanal;
import com.unillanos.server.entity.UsuarioEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Servicio de apoyo para administración de canales desde la GUI.
 */
@Service
public class AdminChannelsService {

    private final ICanalRepository canalRepository;
    private final ICanalMiembroRepository canalMiembroRepository;
    private final IUsuarioRepository usuarioRepository;

    public AdminChannelsService(ICanalRepository canalRepository,
                                ICanalMiembroRepository canalMiembroRepository,
                                IUsuarioRepository usuarioRepository) {
        this.canalRepository = canalRepository;
        this.canalMiembroRepository = canalMiembroRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public List<DTOCanal> listChannels(int limit, int offset) {
        List<CanalEntity> canales = canalRepository.findAll(limit, offset);
        List<DTOCanal> dtos = new ArrayList<>();
        for (CanalEntity c : canales) {
            int count = canalMiembroRepository.countMiembros(c.getId());
            dtos.add(c.toDTO(count));
        }
        return dtos;
    }

    public List<DTOMiembroCanal> listMembers(String canalId) {
        List<CanalMiembroEntity> miembros = canalMiembroRepository.findMiembrosByCanal(canalId);
        List<DTOMiembroCanal> dtos = new ArrayList<>();
        for (CanalMiembroEntity m : miembros) {
            Optional<UsuarioEntity> u = usuarioRepository.findById(m.getUsuarioId());
            DTOMiembroCanal dto = new DTOMiembroCanal();
            dto.setUsuarioId(m.getUsuarioId());
            dto.setNombreUsuario(u.map(UsuarioEntity::getNombre).orElse("(desconocido)"));
            dto.setRol(m.getRol() != null ? m.getRol().name() : "MEMBER");
            dto.setFechaUnion(m.getFechaUnion() != null ? m.getFechaUnion().toString() : null);
            dtos.add(dto);
        }
        return dtos;
    }

    public DTOCanal createChannel(String nombre, String descripcion, String creadorId) {
        CanalEntity canal = new CanalEntity();
        canal.setId(UUID.randomUUID().toString());
        canal.setNombre(nombre);
        canal.setDescripcion(descripcion);
        canal.setCreadorId(creadorId);
        canal.setFechaCreacion(LocalDateTime.now());
        canal.setActivo(true);
        CanalEntity saved = canalRepository.save(canal);
        // agregar creador como ADMIN
        canalMiembroRepository.agregarMiembro(saved.getId(), creadorId, RolCanal.ADMIN);
        int count = canalMiembroRepository.countMiembros(saved.getId());
        return saved.toDTO(count);
    }

    public void setActive(String canalId, boolean activo) {
        canalRepository.updateActivo(canalId, activo);
    }

    public void inviteMember(String canalId, String usuarioId, boolean admin) {
        RolCanal rol = admin ? RolCanal.ADMIN : RolCanal.MEMBER;
        canalMiembroRepository.agregarMiembro(canalId, usuarioId, rol);
    }

    public void removeMember(String canalId, String usuarioId) {
        canalMiembroRepository.removerMiembro(canalId, usuarioId);
    }

    public void changeMemberRole(String canalId, String usuarioId, String nuevoRol) {
        RolCanal rol = RolCanal.fromString(nuevoRol);
        canalMiembroRepository.actualizarRol(canalId, usuarioId, rol);
    }
}


