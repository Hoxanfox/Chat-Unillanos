package gestorP2P.registroP2P;

import dominio.p2p.Peer;
import dto.p2p.DTOPeer;
import repositorio.p2p.PeerRepositorio;
import observador.IObservador;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
    }

    @Override
    public boolean registrarPeer(Peer peer, String socketInfo) {
        if (peer == null) return false;
        boolean ok = repo.guardarOActualizarPeer(peer, socketInfo);
        if (ok) {
            // notificar registro de peer individual
            notificarObservadores("PEER_REGISTRADO", peer);
        }
        return ok;
    }

    @Override
    public Peer mapearDesdeDTO(DTOPeer dto) {
        if (dto == null) return null;
        UUID id = null;
        if (dto.getId() != null && !dto.getId().isEmpty()) {
            try { id = UUID.fromString(dto.getId()); } catch (Exception ignored) {}
        }
        String ip = dto.getIp();
        Peer.Estado estado = Peer.Estado.ONLINE;
        if (dto.getEstado() != null && !dto.getEstado().isEmpty()) {
            try { estado = Peer.Estado.valueOf(dto.getEstado()); } catch (Exception ignored) {}
        }
        Instant fecha = Instant.now();
        if (dto.getFechaCreacion() != null && !dto.getFechaCreacion().isEmpty()) {
            try { fecha = Instant.parse(dto.getFechaCreacion()); } catch (Exception ignored) {}
        }
        return new Peer(id, ip, null, estado, fecha);
    }

    @Override
    public boolean registrarDesdeDTO(DTOPeer dto) {
        if (dto == null) return false;
        Peer p = mapearDesdeDTO(dto);
        if (p == null) return false;
        String socketInfo = dto.getSocketInfo();
        boolean ok = registrarPeer(p, socketInfo);
        if (ok) {
            // notificar con DTO original como información adicional
            notificarObservadores("PEER_REGISTRADO_DESDE_DTO", dto);
        }
        return ok;
    }

    @Override
    public List<Peer> registrarListaDesdeDTO(List<DTOPeer> dtos) {
        List<Peer> resultado = new ArrayList<>();
        if (dtos == null) return resultado;
        for (DTOPeer dto : dtos) {
            try {
                Peer p = mapearDesdeDTO(dto);
                if (p != null) {
                    registrarPeer(p, dto.getSocketInfo());
                    resultado.add(p);
                }
            } catch (Exception ignored) {
                // continuar con los demas
            }
        }
        if (!resultado.isEmpty()) {
            notificarObservadores("PEER_LISTA_REGISTRADA", resultado);
        }
        return resultado;
    }

    // Implementación de ISujeto
    @Override
    public void registrarObservador(IObservador observador) {
        if (observador == null) return;
        synchronized (observadores) {
            if (!observadores.contains(observador)) observadores.add(observador);
        }
    }

    @Override
    public void removerObservador(IObservador observador) {
        if (observador == null) return;
        synchronized (observadores) {
            observadores.remove(observador);
        }
    }

    @Override
    public void notificarObservadores(String tipoDeDato, Object datos) {
        List<IObservador> copia;
        synchronized (observadores) {
            copia = new ArrayList<>(observadores);
        }
        for (IObservador obs : copia) {
            try { obs.actualizar(tipoDeDato, datos); } catch (Exception ignored) { }
        }
    }
}
