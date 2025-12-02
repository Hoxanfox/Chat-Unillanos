package interfazGrafica.vistaConexiones.componentes;

import controlador.p2p.ControladorP2P;
import dto.p2p.DTOPeerDetails;
import logger.LoggerCentral;
import observador.IObservador;
import configuracion.Configuracion;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.*;
import java.util.List;

/**
 * Componente que representa un grafo visual de la red P2P
 * Muestra los peers conectados y sus relaciones
 * ✅ ACTUALIZADO: Ahora implementa IObservador para actualización automática
 */
public class GrafoP2P extends JPanel implements IObservador {

    private static final String TAG = "GrafoP2P";

    private Map<String, NodoP2P> nodos;
    private List<ConexionP2P> conexiones;
    private ControladorP2P controlador;

    // ✅ NUEVO: Información del peer local para identificarlo
    private String idPeerLocal;
    private String ipLocal;
    private int puertoLocal;

    // Colores para el grafo
    private static final Color COLOR_PEER_ONLINE = new Color(46, 204, 113);  // Verde
    private static final Color COLOR_PEER_OFFLINE = new Color(149, 165, 166); // Gris
    private static final Color COLOR_PEER_LOCAL = new Color(52, 152, 219);   // Azul
    private static final Color COLOR_CONEXION = new Color(52, 73, 94);       // Gris oscuro
    private static final Color COLOR_TEXTO = Color.BLACK;

    public GrafoP2P() {
        this.nodos = new LinkedHashMap<>();
        this.conexiones = new ArrayList<>();
        configurarPanel();
        cargarConfiguracionLocal();
    }

    /**
     * ✅ NUEVO: Constructor con controlador para suscribirse a eventos
     */
    public GrafoP2P(ControladorP2P controlador) {
        this();
        this.controlador = controlador;
        if (controlador != null) {
            suscribirseAEventos();
        }
    }

    /**
     * ✅ MEJORADO: Cargar la configuración del peer local para identificarlo
     */
    private void cargarConfiguracionLocal() {
        Configuracion config = Configuracion.getInstance();
        this.ipLocal = config.getPeerHost();
        this.puertoLocal = config.getPeerPuerto();
        
        // ✅ NUEVO: Si la IP configurada es genérica, intentar detectar la IP real
        if (ipLocal == null || ipLocal.equals("0.0.0.0") || ipLocal.equals("127.0.0.1")) {
            String ipDetectada = detectarIPReal();
            if (ipDetectada != null) {
                LoggerCentral.info(TAG, "IP detectada automáticamente: " + ipDetectada);
                this.ipLocal = ipDetectada;
            }
        }
        
        LoggerCentral.info(TAG, "Configuración local cargada: " + ipLocal + ":" + puertoLocal);
    }

    /**
     * ✅ NUEVO: Detecta la IP real de red (prioriza Hamachi)
     */
    private String detectarIPReal() {
        try {
            java.util.Enumeration<java.net.NetworkInterface> interfaces = 
                java.net.NetworkInterface.getNetworkInterfaces();
            
            String ipFallback = null;
            
            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface iface = interfaces.nextElement();
                
                // Saltar interfaces inactivas o loopback
                if (!iface.isUp() || iface.isLoopback()) continue;
                
                java.util.Enumeration<java.net.InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    java.net.InetAddress addr = addresses.nextElement();
                    
                    // Solo IPv4, no loopback
                    if (addr instanceof java.net.Inet4Address && !addr.isLoopbackAddress()) {
                        String ip = addr.getHostAddress();
                        
                        // Priorizar IPs de Hamachi (25.x.x.x)
                        if (ip.startsWith("25.")) {
                            LoggerCentral.info(TAG, "✅ IP Hamachi detectada: " + ip);
                            return ip;
                        }
                        
                        // Guardar como fallback
                        if (ipFallback == null) {
                            ipFallback = ip;
                        }
                    }
                }
            }
            
            return ipFallback;
            
        } catch (Exception e) {
            LoggerCentral.warn(TAG, "Error detectando IP de red: " + e.getMessage());
            return null;
        }
    }

    /**
     * ✅ MEJORADO: Determina si un peer es el local usando múltiples criterios
     */
    private boolean esPeerLocal(String idPeer, String ip, int puerto) {
        // Criterio 1: Comparar por ID del peer (más confiable)
        if (idPeerLocal != null && idPeerLocal.equals(idPeer)) {
            LoggerCentral.debug(TAG, "Peer LOCAL identificado por ID: " + idPeer);
            return true;
        }
        
        // Criterio 2: ID "LOCAL" (convención)
        if ("LOCAL".equalsIgnoreCase(idPeer)) {
            LoggerCentral.debug(TAG, "Peer LOCAL identificado por convención");
            return true;
        }
        
        // Criterio 3: Comparar por IP y puerto
        String ipNormalizada = normalizarIP(ip);
        String ipLocalNormalizada = normalizarIP(ipLocal);

        if (ipNormalizada.equals(ipLocalNormalizada) && puerto == puertoLocal) {
            LoggerCentral.debug(TAG, "Peer LOCAL identificado por IP:puerto: " + ip + ":" + puerto);
            return true;
        }
        
        // Criterio 4: Si la IP es localhost
        if ((ipNormalizada.equals("127.0.0.1") || ipNormalizada.equals("localhost")) && puerto == puertoLocal) {
            LoggerCentral.debug(TAG, "Peer LOCAL identificado por localhost");
            return true;
        }

        return false;
    }

    /**
     * ✅ Método de compatibilidad para llamadas con solo IP y puerto
     */
    private boolean esPeerLocal(String ip, int puerto) {
        return esPeerLocal(null, ip, puerto);
    }

    /**
     * ✅ MEJORADO: Normaliza direcciones IP
     */
    private String normalizarIP(String ip) {
        if (ip == null || ip.isEmpty()) return "127.0.0.1";

        // Remover prefijo "/" si existe
        if (ip.startsWith("/")) {
            ip = ip.substring(1);
        }
        
        // Remover puerto si está incluido
        if (ip.contains(":")) {
            ip = ip.split(":")[0];
        }

        // Normalizar localhost y 0.0.0.0
        if (ip.equals("localhost") || ip.equals("0.0.0.0")) {
            return "127.0.0.1";
        }

        return ip;
    }

    /**
     * ✅ NUEVO: Suscribirse a cambios en los peers P2P
     */
    private void suscribirseAEventos() {
        // ✅ NUEVO: Obtener ID del peer local desde el controlador
        obtenerIdPeerLocalDesdeControlador();
        
        // Suscribirse a eventos de conexión/desconexión
        controlador.suscribirseAEventosConexion();

        // Suscribirse a eventos del repositorio de peers
        controlador.suscribirseAPeerRepositorio(this);

        // ✅ NUEVO: Suscribirse a eventos de topología para recibir LISTA_PEERS_ACTIVOS
        controlador.suscribirseATopologia(this);

        // Registrarse como observador
        controlador.suscribirActualizacionLista(this::actualizarConListaPeers);
        controlador.suscribirConexionPeer(peer -> actualizarDesdeControlador());
        controlador.suscribirDesconexionPeer(peer -> actualizarDesdeControlador());

        LoggerCentral.info(TAG, "GrafoP2P suscrito a eventos de peers");
        LoggerCentral.info(TAG, "✓ Suscrito a ServicioTopologiaRed para LISTA_PEERS_ACTIVOS");
        LoggerCentral.info(TAG, "✓ ID del peer local: " + idPeerLocal);

        // Cargar datos iniciales desde la base de datos
        cargarPeersDesdeBaseDatos();
    }

    /**
     * ✅ NUEVO: Obtener el ID del peer local desde el controlador
     */
    private void obtenerIdPeerLocalDesdeControlador() {
        if (controlador != null) {
            try {
                var servicioP2P = controlador.getServicioP2PInterno();
                if (servicioP2P != null) {
                    var idLocal = servicioP2P.obtenerIdPeerLocal();
                    if (idLocal != null) {
                        this.idPeerLocal = idLocal.toString();
                        LoggerCentral.info(TAG, "ID del peer local obtenido: " + this.idPeerLocal);
                    }
                }
            } catch (Exception e) {
                LoggerCentral.warn(TAG, "No se pudo obtener ID del peer local: " + e.getMessage());
            }
        }
    }

    /**
     * ✅ NUEVO: Carga los peers desde la base de datos al iniciar
     */
    private void cargarPeersDesdeBaseDatos() {
        try {
            LoggerCentral.info(TAG, "📂 Cargando peers desde la base de datos...");

            // Obtener todos los peers de la BD (incluye ONLINE y OFFLINE)
            List<DTOPeerDetails> peersDB = controlador.obtenerListaPeers();

            LoggerCentral.info(TAG, "✅ Peers cargados desde BD: " + peersDB.size());

            // Actualizar el grafo con los datos de la BD
            actualizarConListaPeers(peersDB);

        } catch (Exception e) {
            LoggerCentral.error(TAG, "❌ Error al cargar peers desde BD: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * ✅ ACTUALIZADO: Actualizar con lista de peers - SOLO PEERS P2P (sin clientes)
     */
    private void actualizarConListaPeers(List<DTOPeerDetails> peers) {
        SwingUtilities.invokeLater(() -> {
            limpiar();

            LoggerCentral.info(TAG, "📊 Actualizando GrafoP2P: " + peers.size() + " peers recibidos");

            // ✅ SOLO agregar PEERS (identificarlos correctamente)
            for (DTOPeerDetails peer : peers) {
                String idPeer = peer.getId();
                String ip = peer.getIp();
                int puerto = peer.getPuertoServidor() > 0 ? peer.getPuertoServidor() : peer.getPuerto();

                // ✅ MEJORADO: Usar el nuevo método con ID
                boolean esLocal = esPeerLocal(idPeer, ip, puerto);
                boolean esOnline = "ONLINE".equalsIgnoreCase(peer.getEstado());

                LoggerCentral.debug(TAG, "📍 Peer: " + idPeer + " | " + ip + ":" + puerto +
                                   " | Local: " + esLocal + " | Online: " + esOnline);

                // Solo agregar si no existe (evitar duplicados)
                if (!nodos.containsKey(idPeer)) {
                    agregarPeer(idPeer, ip, puerto, esLocal, esOnline);
                }
            }

            // Crear conexiones P2P en malla completa
            List<DTOPeerDetails> peersAgregados = peers.stream()
                .filter(p -> nodos.containsKey(p.getId()))
                .collect(java.util.stream.Collectors.toList());

            agregarConexionesP2P(peersAgregados);

            LoggerCentral.info(TAG, "✅ Grafo P2P actualizado: " + nodos.size() + " peers, " +
                              conexiones.size() + " conexiones");

            repaint();
        });
    }

    /**
     * ✅ NUEVO: Actualizar el grafo con datos del controlador
     */
    private void actualizarDesdeControlador() {
        if (controlador == null) return;

        List<DTOPeerDetails> peers = controlador.obtenerListaPeers();
        actualizarConListaPeers(peers);

        LoggerCentral.debug(TAG, "Grafo P2P actualizado: " + peers.size() + " peers");
    }

    /**
     * ✅ ACTUALIZADO: Agregar conexiones P2P (todos los peers se conectan entre sí)
     */
    private void agregarConexionesP2P(List<DTOPeerDetails> peers) {
        // En una red P2P, típicamente todos están conectados entre sí
        for (int i = 0; i < peers.size(); i++) {
            for (int j = i + 1; j < peers.size(); j++) {
                agregarConexion(peers.get(i).getId(), peers.get(j).getId());
            }
        }
    }

    private void configurarPanel() {
        this.setBackground(Color.WHITE);
        this.setBorder(BorderFactory.createLineBorder(new Color(189, 195, 199), 1));
        // Tamaño más grande para mejor visualización con zoom
        this.setPreferredSize(new Dimension(800, 600));
    }

    /**
     * ✅ ACTUALIZADO: Método original ahora es wrapper del nuevo método
     */
    public void agregarPeer(String id, String ip, boolean esLocal, boolean esOnline) {
        agregarPeer(id, ip, 0, esLocal, esOnline);
    }

    /**
     * ✅ NUEVO: Método mejorado que incluye el puerto para mostrar IP completa
     */
    public void agregarPeer(String id, String ip, int puerto, boolean esLocal, boolean esOnline) {
        NodoP2P nodo = new NodoP2P(id, ip, puerto, esLocal, esOnline);
        nodos.put(id, nodo);
        calcularPosiciones();
        repaint();
    }

    public void agregarConexion(String idOrigen, String idDestino) {
        NodoP2P origen = nodos.get(idOrigen);
        NodoP2P destino = nodos.get(idDestino);
        if (origen != null && destino != null) {
            conexiones.add(new ConexionP2P(origen, destino));
            repaint();
        }
    }

    public void limpiar() {
        nodos.clear();
        conexiones.clear();
        repaint();
    }

    private void calcularPosiciones() {
        int cantidadNodos = nodos.size();
        if (cantidadNodos == 0) return;

        int width = getWidth();
        int height = getHeight();

        if (width == 0 || height == 0) {
            width = 800;
            height = 600;
        }

        int centerX = width / 2;
        int centerY = height / 2;
        // Radio más pequeño para mejor centrado
        int radio = Math.min(width, height) / 3;

        // Distribuir los nodos en círculo
        int i = 0;
        for (NodoP2P nodo : nodos.values()) {
            double angulo = 2 * Math.PI * i / cantidadNodos;
            nodo.x = (int) (centerX + radio * Math.cos(angulo));
            nodo.y = (int) (centerY + radio * Math.sin(angulo));
            i++;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Calcular posiciones cada vez para asegurar centrado correcto
        calcularPosiciones();

        // Dibujar conexiones primero
        g2d.setStroke(new BasicStroke(2));
        for (ConexionP2P conexion : conexiones) {
            g2d.setColor(COLOR_CONEXION);
            g2d.draw(new Line2D.Double(
                    conexion.origen.x, conexion.origen.y,
                    conexion.destino.x, conexion.destino.y
            ));
        }

        // Dibujar nodos
        for (NodoP2P nodo : nodos.values()) {
            dibujarNodo(g2d, nodo);
        }

        // Si no hay nodos, mostrar mensaje
        if (nodos.isEmpty()) {
            g2d.setColor(Color.GRAY);
            g2d.setFont(new Font("Arial", Font.ITALIC, 14));
            String mensaje = "No hay peers conectados";
            FontMetrics fm = g2d.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(mensaje)) / 2;
            int y = getHeight() / 2;
            g2d.drawString(mensaje, x, y);
        }
    }

    private void dibujarNodo(Graphics2D g2d, NodoP2P nodo) {
        int radio = 25;

        // Determinar color según estado
        Color color;
        if (nodo.esLocal) {
            color = COLOR_PEER_LOCAL; // ✅ Azul para peer local
        } else if (nodo.esOnline) {
            color = COLOR_PEER_ONLINE;
        } else {
            color = COLOR_PEER_OFFLINE;
        }

        // Dibujar círculo
        Ellipse2D circulo = new Ellipse2D.Double(
                nodo.x - radio, nodo.y - radio,
                radio * 2, radio * 2
        );
        g2d.setColor(color);
        g2d.fill(circulo);

        // Borde
        g2d.setColor(color.darker());
        g2d.setStroke(new BasicStroke(2));
        g2d.draw(circulo);

        // ✅ Etiqueta especial para peer local
        if (nodo.esLocal) {
            g2d.setColor(COLOR_PEER_LOCAL);
            g2d.setFont(new Font("Arial", Font.BOLD, 9));
            String etiqueta = "LOCAL";
            FontMetrics fm = g2d.getFontMetrics();
            int etiquetaX = nodo.x - fm.stringWidth(etiqueta) / 2;
            int etiquetaY = nodo.y + 4;
            g2d.drawString(etiqueta, etiquetaX, etiquetaY);
        }

        // Texto: ID (truncado)
        g2d.setColor(COLOR_TEXTO);
        g2d.setFont(new Font("Arial", Font.BOLD, 10));
        String idTruncado = nodo.id.length() > 8 ? nodo.id.substring(0, 8) + "..." : nodo.id;
        FontMetrics fm = g2d.getFontMetrics();
        int textoX = nodo.x - fm.stringWidth(idTruncado) / 2;
        int textoY = nodo.y + radio + 15;
        g2d.drawString(idTruncado, textoX, textoY);

        // ✅ CORREGIDO: Mostrar IP con puerto para diferenciarlos
        g2d.setFont(new Font("Arial", Font.PLAIN, 9));
        String ipDisplay = nodo.puerto > 0 ? nodo.ip + ":" + nodo.puerto : nodo.ip;
        fm = g2d.getFontMetrics();
        int ipX = nodo.x - fm.stringWidth(ipDisplay) / 2;
        int ipY = textoY + 12;
        g2d.drawString(ipDisplay, ipX, ipY);
    }

    /**
     * ✅ IMPLEMENTACIÓN IObservador
     */
    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        LoggerCentral.debug(TAG, "Evento recibido: " + tipoDeDato);

        switch (tipoDeDato) {
            case "PEER_CONECTADO":
            case "PEER_DESCONECTADO":
            case "LISTA_PEERS_ACTIVOS":  // ✅ NUEVO: Evento de ServicioTopologiaRed
            case "PEER_CREADO":       // ✅ NUEVO: Evento del repositorio
            case "PEER_ACTUALIZADO":  // ✅ NUEVO: Evento del repositorio
                // ✅ MEJORADO: Si es LISTA_PEERS_ACTIVOS, convertir y actualizar directamente
                if ("LISTA_PEERS_ACTIVOS".equals(tipoDeDato) && datos instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<DTOPeerDetails> peers = (List<DTOPeerDetails>) datos;
                    LoggerCentral.info(TAG, "🔄 Actualizando grafo P2P con LISTA_PEERS_ACTIVOS: " + peers.size() + " peers");
                    actualizarConListaPeers(peers);
                } else {
                    // Actualizar grafo cuando cambian los peers o la topología de red
                    LoggerCentral.info(TAG, "🔄 Actualizando grafo P2P por evento: " + tipoDeDato);
                    SwingUtilities.invokeLater(this::actualizarDesdeControlador);
                }
                break;

            default:
                LoggerCentral.debug(TAG, "Evento no manejado: " + tipoDeDato);
        }
    }

    // Clase interna para representar un nodo P2P
    private static class NodoP2P {
        String id;
        String ip;
        int puerto; // ✅ NUEVO: Incluir puerto
        boolean esLocal;
        boolean esOnline; // ✅ AGREGADO: Campo faltante
        int x, y;

        NodoP2P(String id, String ip, int puerto, boolean esLocal, boolean esOnline) {
            this.id = id;
            this.ip = ip;
            this.puerto = puerto;
            this.esLocal = esLocal;
            this.esOnline = esOnline;
            this.x = 0;
            this.y = 0;
        }
    }

    // Clase interna para representar una conexión
    private static class ConexionP2P {
        NodoP2P origen;
        NodoP2P destino;

        ConexionP2P(NodoP2P origen, NodoP2P destino) {
            this.origen = origen;
            this.destino = destino;
        }
    }
}
