package conexion.clientes.impl;

import conexion.clientes.interfaces.IGestorConexionesCliente;
import conexion.clientes.interfaces.IRouterMensajesCliente;
import dto.cliente.DTOSesionCliente;
import transporte.p2p.interfaces.IMensajeListener;
import transporte.p2p.interfaces.ITransporteTcp;
import transporte.p2p.impl.NettyTransporteImpl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GestorConexionesClienteImpl implements IGestorConexionesCliente, IMensajeListener {

    // --- COLORES ---
    private static final String RESET = "\u001B[0m";
    private static final String CYAN = "\u001B[36m";
    private static final String TAG = "\u001B[35m[GestorClientes] \u001B[0m";

    private final ITransporteTcp transporte;

    // Pool Principal: ID_SESION (ip:port) -> OBJETO SESIÓN
    private final Map<String, DTOSesionCliente> poolSesiones;

    // Índice Inverso: ID_USUARIO -> ID_SESION (Para búsquedas rápidas)
    private final Map<String, String> mapaUsuarioSesion;

    private IRouterMensajesCliente router;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public GestorConexionesClienteImpl() {
        this.poolSesiones = new ConcurrentHashMap<>();
        this.mapaUsuarioSesion = new ConcurrentHashMap<>();
        // Usamos una instancia NUEVA de Netty para el puerto de clientes
        this.transporte = new NettyTransporteImpl(this);
    }

    public void setRouter(IRouterMensajesCliente router) {
        this.router = router;
    }

    @Override
    public void iniciarServidor(int puertoEscucha) {
        try {
            System.out.println(TAG + "Iniciando servidor para CLIENTES en puerto " + CYAN + puertoEscucha + RESET);
            transporte.iniciarEscucha(puertoEscucha);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void enviarMensaje(String idSesion, String mensaje) {
        DTOSesionCliente sesion = poolSesiones.get(idSesion);
        if (sesion != null) {
            transporte.enviarMensaje(sesion.getIp(), sesion.getPuerto(), mensaje);
        } else {
            System.err.println(TAG + "Error: Sesión no encontrada " + idSesion);
        }
    }

    @Override
    public void enviarMensajeAUsuario(String idUsuario, String mensaje) {
        String idSesion = mapaUsuarioSesion.get(idUsuario);
        if (idSesion != null) {
            enviarMensaje(idSesion, mensaje);
        } else {
            // El usuario no está conectado a este servidor (podría estar en otro peer)
            // Aquí el servicio de nivel superior decidirá si enrutar via P2P.
            System.out.println(TAG + "Usuario " + idUsuario + " no está conectado localmente.");
        }
    }

    @Override
    public boolean registrarUsuarioEnSesion(String idSesion, String idUsuario) {
        DTOSesionCliente sesion = poolSesiones.get(idSesion);
        if (sesion != null) {
            // Si el usuario ya tenía otra sesión abierta, la desvinculamos (o permitimos multiseción)
            // Aquí asumimos 1 sesión por usuario para simplificar
            String sesionAntigua = mapaUsuarioSesion.get(idUsuario);
            if (sesionAntigua != null && !sesionAntigua.equals(idSesion)) {
                desconectar(sesionAntigua); // Kick sesión antigua
            }

            sesion.setIdUsuario(idUsuario);
            mapaUsuarioSesion.put(idUsuario, idSesion);
            System.out.println(TAG + "Usuario " + idUsuario + " vinculado a sesión " + idSesion);
            return true;
        }
        return false;
    }

    @Override
    public void broadcast(String mensaje) {
        poolSesiones.values().forEach(s ->
                transporte.enviarMensaje(s.getIp(), s.getPuerto(), mensaje)
        );
    }

    @Override
    public void desconectar(String idSesion) {
        DTOSesionCliente sesion = poolSesiones.remove(idSesion);
        if (sesion != null) {
            if (sesion.getIdUsuario() != null) {
                mapaUsuarioSesion.remove(sesion.getIdUsuario());
            }
            // Desconexión física
            transporte.desconectar(sesion.getIp(), sesion.getPuerto());
            System.out.println(TAG + "Cliente desconectado: " + idSesion);
        }
    }

    @Override
    public DTOSesionCliente obtenerSesion(String idSesion) {
        return poolSesiones.get(idSesion);
    }

    @Override
    public List<DTOSesionCliente> obtenerClientesConectados() {
        return new ArrayList<>(poolSesiones.values());
    }

    @Override
    public void apagar() {
        transporte.detener();
        poolSesiones.clear();
        mapaUsuarioSesion.clear();
    }

    // --- EVENTOS NETTY (Desde el transporte) ---

    @Override
    public void onNuevaConexion(String origen) {
        // Origen viene como "ip:puerto"
        try {
            String[] parts = origen.split(":");
            String ip = parts[0].startsWith("/") ? parts[0].substring(1) : parts[0];
            int puerto = Integer.parseInt(parts[1]);

            DTOSesionCliente sesion = new DTOSesionCliente(
                    origen, ip, puerto, "CONECTADO", LocalDateTime.now().format(FORMATTER)
            );
            poolSesiones.put(origen, sesion);
            // System.out.println(TAG + "Nuevo cliente conectado: " + origen);
        } catch (Exception e) {
            System.err.println(TAG + "Error registrando cliente: " + e.getMessage());
        }
    }

    @Override
    public void onMensajeRecibido(String mensaje, String origen) {
        // Verificamos que la sesión exista
        if (!poolSesiones.containsKey(origen)) {
            onNuevaConexion(origen);
        }

        if (router != null) {
            router.procesarMensaje(mensaje, origen);
        }
    }

    @Override
    public void onDesconexion(String origen) {
        DTOSesionCliente sesion = poolSesiones.remove(origen);
        if (sesion != null) {
            if (sesion.getIdUsuario() != null) {
                mapaUsuarioSesion.remove(sesion.getIdUsuario());
            }
            System.out.println(TAG + "Cliente salió: " + origen);
        }
    }
}