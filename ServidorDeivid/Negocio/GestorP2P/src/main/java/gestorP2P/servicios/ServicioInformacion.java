package gestorP2P.servicios;

import conexion.p2p.interfaces.IRouterMensajes;
import conexion.p2p.interfaces.IGestorConexiones;
import dto.p2p.DTOPeerDetails;
import gestorP2P.interfaces.IServicioP2P;
import logger.LoggerCentral;
import repositorio.p2p.PeerRepositorio;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de solo lectura para obtener información del estado de la red.
 * Combina datos en tiempo real (Memoria) con datos históricos (BD).
 */
public class ServicioInformacion implements IServicioP2P {

    private static final String TAG = "ServicioInfo";

    private IGestorConexiones gestorConexiones;
    private final PeerRepositorio repositorio;

    public ServicioInformacion() {
        this.repositorio = new PeerRepositorio();
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
     * Marca como "ONLINE" los que están en el pool de conexiones activo.
     * Marca como "OFFLINE" el resto.
     */
    public List<DTOPeerDetails> obtenerHistorialCompleto() {
        // LoggerCentral.debug(TAG, "Consultando historial de peers (BD + Memoria)...");

        // 1. Obtener todos los de la BD (Historial)
        List<PeerRepositorio.PeerInfo> historico = repositorio.listarPeersInfo();

        // 2. Obtener los activos en memoria
        List<DTOPeerDetails> activos = gestorConexiones.obtenerDetallesPeers();
        List<String> ipsActivas = activos.stream().map(DTOPeerDetails::getIp).collect(Collectors.toList());

        List<DTOPeerDetails> resultado = new ArrayList<>();

        for (PeerRepositorio.PeerInfo info : historico) {
            // Determinamos estado real cruzando datos
            // (Simplificación: si la IP está en los sockets abiertos, está online)
            boolean isOnline = ipsActivas.contains(info.ip);

            String estado = isOnline ? "ONLINE" : "OFFLINE";

            // Para los online, intentamos buscar el puerto servidor actualizado si está en memoria
            // Esto es vital para mostrar el puerto real (ej. 9000) y no el efímero (ej. 54321)
            int puertoMostrar = info.puerto;
            if (isOnline) {
                // Buscar en activos para ver si tiene un puerto servidor actualizado
                DTOPeerDetails activo = activos.stream()
                        .filter(a -> a.getIp().equals(info.ip))
                        .findFirst()
                        .orElse(null);

                if (activo != null && activo.getPuertoServidor() > 0) {
                    puertoMostrar = activo.getPuertoServidor();
                }
            }

            resultado.add(new DTOPeerDetails(
                    info.id.toString(),
                    info.ip,
                    puertoMostrar,
                    estado,
                    info.fechaCreacion.toString()
            ));
        }

        // LoggerCentral.debug(TAG, "Consulta finalizada. Peers encontrados: " + resultado.size());
        return resultado;
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