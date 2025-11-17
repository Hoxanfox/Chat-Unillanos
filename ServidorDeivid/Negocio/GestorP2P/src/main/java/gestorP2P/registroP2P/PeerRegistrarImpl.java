package gestorP2P.registroP2P;

import dominio.p2p.Peer;
import dto.p2p.DTOPeer;
import repositorio.p2p.PeerRepositorio;
import observador.IObservador;
import logger.LoggerCentral;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implementación de la responsabilidad única de mapear y persistir peers.
 * Ahora actúa también como Sujeto observador: notifica cuando registra peers.
 */
public class PeerRegistrarImpl implements IPeerRegistrar{

    private final PeerRepositorio repo;
    private final List<IObservador> observadores = new ArrayList<>();

    public PeerRegistrarImpl() {
        this.repo = new PeerRepositorio();
        LoggerCentral.debug("PeerRegistrarImpl: inicializado con PeerRepositorio");
    }

    @Override
    public boolean registrarPeer(Peer peer, String socketInfo) {
        if (peer == null) {
            LoggerCentral.warn("PeerRegistrarImpl.registrarPeer: intento de registrar peer nulo");
            return false;
        }
        LoggerCentral.debug("PeerRegistrarImpl.registrarPeer: intentando guardar/actualizar peer id=" + (peer.getId()!=null?peer.getId().toString():"<null>") + " ip=" + peer.getIp() + " socketInfo=" + socketInfo);
        boolean ok = repo.guardarOActualizarPeer(peer, socketInfo);
        if (ok) {
            LoggerCentral.info("PeerRegistrarImpl.registrarPeer: peer guardado/actualizado correctamente id=" + (peer.getId()!=null?peer.getId().toString():"<null>"));
            // notificar registro de peer individual, incluir socketInfo en el payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("peer", peer);
            payload.put("socketInfo", socketInfo);
            notificarObservadores("PEER_REGISTRADO", payload);

            // Además notificar en formato DTO para que las capas superiores dependan del módulo DTO
            try {
                DTOPeer dto = new DTOPeer();
                dto.setId(peer.getId() != null ? peer.getId().toString() : null);
                dto.setIp(peer.getIp());
                dto.setSocketInfo(socketInfo);
                dto.setEstado(peer.getEstado() != null ? peer.getEstado().name() : null);
                dto.setFechaCreacion(peer.getFechaCreacion() != null ? peer.getFechaCreacion().toString() : null);
                LoggerCentral.debug("PeerRegistrarImpl.registrarPeer: notificando DTOPeer para PEER_REGISTRADO_DESDE_DTO id=" + dto.getId());
                notificarObservadores("PEER_REGISTRADO_DESDE_DTO", dto);
            } catch (Exception e) {
                LoggerCentral.warn("PeerRegistrarImpl.registrarPeer: no se pudo crear DTOPeer para notificar: " + e.getMessage());
            }
        } else {
            LoggerCentral.warn("PeerRegistrarImpl.registrarPeer: no se pudo guardar peer id=" + (peer.getId()!=null?peer.getId().toString():"<null>"));
        }
        return ok;
    }

    @Override
    public Peer mapearDesdeDTO(DTOPeer dto) {
        if (dto == null) {
            LoggerCentral.debug("PeerRegistrarImpl.mapearDesdeDTO: dto nulo");
            return null;
        }
        LoggerCentral.debug("PeerRegistrarImpl.mapearDesdeDTO: mapeando DTOPeer id=" + dto.getId() + " ip=" + dto.getIp() + " estado=" + dto.getEstado());
        UUID id = null;
        if (dto.getId() != null && !dto.getId().isEmpty()) {
            try { id = UUID.fromString(dto.getId()); } catch (Exception e) { LoggerCentral.warn("PeerRegistrarImpl.mapearDesdeDTO: id no válido='" + dto.getId() + "'"); }
        }
        String ip = dto.getIp();
        Peer.Estado estado = Peer.Estado.ONLINE;
        if (dto.getEstado() != null && !dto.getEstado().isEmpty()) {
            try { estado = Peer.Estado.valueOf(dto.getEstado()); } catch (Exception e) { LoggerCentral.warn("PeerRegistrarImpl.mapearDesdeDTO: estado no válido='" + dto.getEstado() + "', usando ONLINE"); }
        }
        Instant fecha = Instant.now();
        if (dto.getFechaCreacion() != null && !dto.getFechaCreacion().isEmpty()) {
            try { fecha = Instant.parse(dto.getFechaCreacion()); } catch (Exception e) { LoggerCentral.warn("PeerRegistrarImpl.mapearDesdeDTO: fechaCreacion no válida='" + dto.getFechaCreacion() + "', usando now"); }
        }
        Peer p = new Peer(id, ip, null, estado, fecha);
        LoggerCentral.debug("PeerRegistrarImpl.mapearDesdeDTO: mapeo resultado id=" + (p.getId()!=null?p.getId().toString():"<null>") + " ip=" + p.getIp() + " estado=" + p.getEstado());
        return p;
    }

    @Override
    public boolean registrarDesdeDTO(DTOPeer dto) {
        if (dto == null) {
            LoggerCentral.warn("PeerRegistrarImpl.registrarDesdeDTO: dto nulo");
            return false;
        }
        LoggerCentral.debug("PeerRegistrarImpl.registrarDesdeDTO: intentando registrar desde DTOPeer id=" + dto.getId() + " ip=" + dto.getIp());
        Peer p = mapearDesdeDTO(dto);
        if (p == null) {
            LoggerCentral.warn("PeerRegistrarImpl.registrarDesdeDTO: mapearDesdeDTO devolvió null");
            return false;
        }
        String socketInfo = dto.getSocketInfo();
        boolean ok = registrarPeer(p, socketInfo);
        if (ok) {
            // notificar con DTO original como información adicional
            LoggerCentral.info("PeerRegistrarImpl.registrarDesdeDTO: registro desde DTO exitoso id=" + (p.getId()!=null?p.getId().toString():"<null>"));
            notificarObservadores("PEER_REGISTRADO_DESDE_DTO", dto);
        } else {
            LoggerCentral.warn("PeerRegistrarImpl.registrarDesdeDTO: fallo al registrar desde DTO id=" + (p.getId()!=null?p.getId().toString():"<null>"));
        }
        return ok;
    }

    @Override
    public List<Peer> registrarListaDesdeDTO(List<DTOPeer> dtos) {
        List<Peer> resultado = new ArrayList<>();
        if (dtos == null) {
            LoggerCentral.debug("PeerRegistrarImpl.registrarListaDesdeDTO: dtos nulo");
            return resultado;
        }
        LoggerCentral.debug("PeerRegistrarImpl.registrarListaDesdeDTO: procesando lista de tamaño=" + dtos.size());
        for (DTOPeer dto : dtos) {
            try {
                Peer p = mapearDesdeDTO(dto);
                if (p != null) {
                    registrarPeer(p, dto.getSocketInfo());
                    resultado.add(p);
                }
            } catch (Exception e) {
                LoggerCentral.warn("PeerRegistrarImpl.registrarListaDesdeDTO: excepción procesando dto id=" + (dto!=null?dto.getId():"<null>") + " -> " + e.getMessage());
                // continuar con los demas
            }
        }
        if (!resultado.isEmpty()) {
            LoggerCentral.info("PeerRegistrarImpl.registrarListaDesdeDTO: se registraron " + resultado.size() + " peers");
            // Notificar la lista original de DTOs para preservar socketInfo
            notificarObservadores("PEER_LISTA_REGISTRADA", dtos);
        } else {
            LoggerCentral.debug("PeerRegistrarImpl.registrarListaDesdeDTO: no se registraron peers en la lista");
        }
        return resultado;
    }

    // Implementación de ISujeto
    @Override
    public void registrarObservador(IObservador observador) {
        if (observador == null) {
            LoggerCentral.warn("PeerRegistrarImpl.registrarObservador: intento de registrar observador nulo");
            return;
        }
        synchronized (observadores) {
            if (!observadores.contains(observador)) {
                observadores.add(observador);
                LoggerCentral.debug("PeerRegistrarImpl.registrarObservador: observador registrado: " + observador.getClass().getSimpleName());
            } else {
                LoggerCentral.debug("PeerRegistrarImpl.registrarObservador: observador ya registrado: " + observador.getClass().getSimpleName());
            }
        }
    }

    @Override
    public void removerObservador(IObservador observador) {
        if (observador == null) {
            LoggerCentral.warn("PeerRegistrarImpl.removerObservador: intento de remover observador nulo");
            return;
        }
        synchronized (observadores) {
            observadores.remove(observador);
            LoggerCentral.debug("PeerRegistrarImpl.removerObservador: observador removido: " + observador.getClass().getSimpleName());
        }
    }

    @Override
    public void notificarObservadores(String tipoDeDato, Object datos) {
        List<IObservador> copia;
        synchronized (observadores) {
            copia = new ArrayList<>(observadores);
        }
        LoggerCentral.debug("PeerRegistrarImpl.notificarObservadores: notificando tipo='" + tipoDeDato + "' a " + copia.size() + " observadores");
        for (IObservador obs : copia) {
            try { obs.actualizar(tipoDeDato, datos); LoggerCentral.debug("PeerRegistrarImpl.notificarObservadores: notificado " + obs.getClass().getSimpleName()); } catch (Exception e) { LoggerCentral.warn("PeerRegistrarImpl.notificarObservadores: fallo notificando a " + obs.getClass().getSimpleName() + " -> " + e.getMessage()); }
        }
    }
}
