package gestorP2P.servicios;

import conexion.p2p.interfaces.IRouterMensajes;
import conexion.p2p.interfaces.IGestorConexiones;
import dominio.p2p.Peer;
import dto.p2p.DTOPeerDetails;
import gestorP2P.interfaces.IServicioP2P;
import logger.LoggerCentral;
import repositorio.p2p.PeerRepositorio;
import observador.IObservador;
import observador.ISujeto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio para obtener información sobre peers conocidos y activos.
 * ✅ ACTUALIZADO: Implementa ISujeto para notificar cambios a la interfaz
 */
public class ServicioInformacion implements IServicioP2P, ISujeto {

    private static final String TAG = "ServicioInfo";

    private IGestorConexiones gestorConexiones;
    private final PeerRepositorio repositorio;
    private final List<IObservador> observadores;

    public ServicioInformacion() {
        this.repositorio = new PeerRepositorio();
        this.observadores = new ArrayList<>();

        // ✅ NUEVO: Suscribirse a eventos del repositorio
        this.repositorio.registrarObservador(new IObservador() {
            @Override
            public void actualizar(String tipoDeDato, Object datos) {
                // Cuando cambia un peer en la BD, notificar a los observadores
                LoggerCentral.info(TAG, "🔔 Evento de repositorio: " + tipoDeDato);
                notificarObservadores(tipoDeDato, datos);
            }
        });

        LoggerCentral.info(TAG, "✓ ServicioInformacion suscrito a PeerRepositorio");
    }

    // ✅ NUEVO: Implementación del patrón Observador
    @Override
    public void registrarObservador(IObservador observador) {
        if (observador != null && !observadores.contains(observador)) {
            observadores.add(observador);
            LoggerCentral.info(TAG, "✓ Observador registrado");
        }
    }

    @Override
    public void removerObservador(IObservador observador) {
        observadores.remove(observador);
    }

    @Override
    public void notificarObservadores(String tipoDeDato, Object datos) {
        for (IObservador observador : observadores) {
            observador.actualizar(tipoDeDato, datos);
        }
    }

    @Override
    public String getNombre() { return "ServicioInformacion"; }

    @Override
    public void inicializar(IGestorConexiones gestor, IRouterMensajes router) {
        this.gestorConexiones = gestor;
        LoggerCentral.info(TAG, "Servicio de Información listo para consultas.");
        // No registra rutas de red, solo atiende consultas locales
    }

    /**
     * Obtiene la lista completa de peers conocidos.
     * Usa el estado de la BD como fuente de verdad.
     * Si el peer está en memoria pero no en BD, se marca como ONLINE.
     */
    public List<DTOPeerDetails> obtenerHistorialCompleto() {
        LoggerCentral.debug(TAG, "Consultando historial de peers (BD)...");

        // 1. Obtener todos los de la BD (Fuente de verdad para el estado)
        List<PeerRepositorio.PeerInfo> historico = repositorio.listarPeersInfo();
        LoggerCentral.debug(TAG, "Peers obtenidos de BD: " + historico.size());

        // 2. Obtener los activos en memoria (para actualizar puerto servidor si aplica)
        List<DTOPeerDetails> activos = gestorConexiones.obtenerDetallesPeers();
        LoggerCentral.debug(TAG, "Peers activos en memoria: " + activos.size());

        List<DTOPeerDetails> resultado = new ArrayList<>();

        for (PeerRepositorio.PeerInfo info : historico) {
            // CORREGIDO: Usar el estado de la BD como fuente de verdad
            String estado = info.estado.name(); // Tomar directamente de la BD

            LoggerCentral.debug(TAG, "Procesando peer: " + info.id + " | " + info.ip + ":" + info.puerto + " | Estado BD: " + estado);

            // Para los online, buscar el puerto servidor actualizado si está en memoria
            int puertoMostrar = info.puerto;

            // Buscar en activos para ver si tiene un puerto servidor actualizado
            DTOPeerDetails activo = activos.stream()
                    .filter(a -> a.getIp() != null && a.getIp().equals(info.ip))
                    .findFirst()
                    .orElse(null);

            if (activo != null && activo.getPuertoServidor() > 0) {
                puertoMostrar = activo.getPuertoServidor();
                LoggerCentral.debug(TAG, "  -> Puerto actualizado de memoria: " + puertoMostrar);
            }

            resultado.add(new DTOPeerDetails(
                    info.id.toString(),
                    info.ip,
                    puertoMostrar,
                    estado,
                    info.fechaCreacion.toString()
            ));
        }

        LoggerCentral.info(TAG, "Consulta finalizada. Peers retornados: " + resultado.size());
        return resultado;
    }

    /**
     * ✅ NUEVO: Expone el repositorio para que otros componentes puedan suscribirse
     */
    public PeerRepositorio getRepositorio() {
        return repositorio;
    }

    @Override
    public void iniciar() {
        // Servicio pasivo, no requiere hilo de inicio
    }

    @Override
    public void detener() {
        // Nada que detener
    }
}