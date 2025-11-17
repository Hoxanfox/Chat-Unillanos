package servicio.p2p;

import fachada.p2p.IRedP2PFacade;
import fachada.p2p.RedP2PFacadeImpl;
import observador.IObservador;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

public class P2PServiceImpl implements IP2PService {

    private final IRedP2PFacade facade;
    // Map from external observer -> wrapper registered on facade
    private final Map<IObservador, IObservador> observerWrappers = new ConcurrentHashMap<>();

    public P2PServiceImpl() {
        this.facade = new RedP2PFacadeImpl();
    }

    // Constructor para inyección/testing
    public P2PServiceImpl(IRedP2PFacade facade) {
        this.facade = facade;
    }

    @Override
    public CompletableFuture<Void> iniciarRed() {
        return facade.iniciarRed();
    }

    @Override
    public void registrarObservador(IObservador observador) {
        if (observador == null) return;
        // Crear wrapper que convierte los objetos de dominio a dto.p2p.DTOPeer (Infraestructura DTO)
        IObservador wrapper = new IObservador() {
            @Override
            public void actualizar(String tipoDeDato, Object datos) {
                try {
                    switch (tipoDeDato) {
                        case "P2P_PEER_LIST_RECIBIDA":
                        case "P2P_ACTUALIZACION":
                            if (datos instanceof Map) {
                                Map<?, ?> map = (Map<?, ?>) datos;
                                Object peersObj = map.get("peers");
                                if (peersObj instanceof List) {
                                    List<?> list = (List<?>) peersObj;
                                    List<dto.p2p.DTOPeer> dtoList = new ArrayList<>();
                                    for (Object o : list) {
                                        if (o != null && o.getClass().getName().equals("dominio.p2p.Peer")) {
                                            try {
                                                dominio.p2p.Peer p = (dominio.p2p.Peer) o;
                                                dto.p2p.DTOPeer sd = new dto.p2p.DTOPeer();
                                                sd.setId(p.getId() != null ? p.getId().toString() : null);
                                                sd.setIp(p.getIp());
                                                sd.setEstado(p.getEstado() == dominio.p2p.Peer.Estado.ONLINE ? "ONLINE" : "OFFLINE");
                                                dtoList.add(sd);
                                            } catch (Exception ignored) {}
                                        }
                                    }
                                    Map<String, Object> newMap = new HashMap<>();
                                    newMap.put("peers", dtoList);
                                    observador.actualizar(tipoDeDato, newMap);
                                    return;
                                }
                            } else if (datos instanceof List) {
                                List<?> list = (List<?>) datos;
                                List<dto.p2p.DTOPeer> dtoList = new ArrayList<>();
                                for (Object o : list) {
                                    if (o != null && o.getClass().getName().equals("dominio.p2p.Peer")) {
                                        try {
                                            dominio.p2p.Peer p = (dominio.p2p.Peer) o;
                                            dto.p2p.DTOPeer sd = new dto.p2p.DTOPeer();
                                            sd.setId(p.getId() != null ? p.getId().toString() : null);
                                            sd.setIp(p.getIp());
                                            sd.setEstado(p.getEstado() == dominio.p2p.Peer.Estado.ONLINE ? "ONLINE" : "OFFLINE");
                                            dtoList.add(sd);
                                        } catch (Exception ignored) {}
                                    }
                                }
                                observador.actualizar(tipoDeDato, dtoList);
                                return;
                            }
                            break;
                        case "P2P_JOIN_EXITOSA":
                            if (datos != null && datos.getClass().getName().equals("dominio.p2p.Peer")) {
                                try {
                                    dominio.p2p.Peer p = (dominio.p2p.Peer) datos;
                                    dto.p2p.DTOPeer sd = new dto.p2p.DTOPeer();
                                    sd.setId(p.getId() != null ? p.getId().toString() : null);
                                    sd.setIp(p.getIp());
                                    sd.setEstado(p.getEstado() == dominio.p2p.Peer.Estado.ONLINE ? "ONLINE" : "OFFLINE");
                                    observador.actualizar(tipoDeDato, sd);
                                    return;
                                } catch (Exception ignored) {}
                            }
                            break;
                        default:
                            break;
                    }
                    // fallback: reenviar lo que llegue
                    observador.actualizar(tipoDeDato, datos);
                } catch (Exception e) {
                    try { observador.actualizar("SERVICE_WRAPPER_ERROR", e.getMessage()); } catch (Exception ignored) {}
                }
            }
        };
        observerWrappers.put(observador, wrapper);
        facade.registrarObservador(wrapper);
    }

    @Override
    public void removerObservador(IObservador observador) {
        if (observador == null) return;
        IObservador wrapper = observerWrappers.remove(observador);
        if (wrapper != null) {
            facade.removerObservador(wrapper);
        }
    }
}
