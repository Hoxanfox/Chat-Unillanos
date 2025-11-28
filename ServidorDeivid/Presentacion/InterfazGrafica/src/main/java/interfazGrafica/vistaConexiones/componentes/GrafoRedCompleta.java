package interfazGrafica.vistaConexiones.componentes;

import controlador.p2p.ControladorP2P;
import dto.topologia.DTOTopologiaRed;
import dto.cliente.DTOSesionCliente;
import logger.LoggerCentral;
import observador.IObservador;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;

/**
 * Componente que representa un grafo unificado mostrando toda la red:
 * Peers P2P conectados entre sí y usuarios conectados a cada peer
 * ✅ ACTUALIZADO: Ahora implementa IObservador y usa topología sincronizada automáticamente
 */
public class GrafoRedCompleta extends JPanel implements IObservador {

    private static final String TAG = "GrafoRedCompleta";

    private Map<String, NodoPeer> peers;
    private List<NodoUsuario> usuarios;
    private List<ConexionP2P> conexionesP2P;
    private ControladorP2P controlador;

    // Colores
    private static final Color COLOR_PEER_LOCAL = new Color(52, 152, 219);   // Azul
    private static final Color COLOR_PEER_ONLINE = new Color(46, 204, 113);  // Verde
    private static final Color COLOR_PEER_OFFLINE = new Color(149, 165, 166); // Gris
    private static final Color COLOR_USUARIO_ONLINE = new Color(26, 188, 156); // Verde agua
    private static final Color COLOR_USUARIO_OFFLINE = new Color(189, 195, 199); // Gris claro
    private static final Color COLOR_CONEXION_P2P = new Color(52, 73, 94);   // Gris oscuro
    private static final Color COLOR_CONEXION_CS = new Color(149, 165, 166);  // Gris medio
    private static final Color COLOR_TEXTO = Color.BLACK;

    public GrafoRedCompleta() {
        this.peers = new LinkedHashMap<>();
        this.usuarios = new ArrayList<>();
        this.conexionesP2P = new ArrayList<>();
        configurarPanel();
    }

    /**
     * ✅ NUEVO: Constructor con controlador para suscribirse a eventos
     */
    public GrafoRedCompleta(ControladorP2P controlador) {
        this();
        this.controlador = controlador;
        if (controlador != null) {
            suscribirseAEventos();
        }
    }

    /**
     * ✅ NUEVO: Suscribirse a cambios en la topología completa
     */
    private void suscribirseAEventos() {
        // Suscribirse a cambios de topología (incluye peers + clientes)
        controlador.suscribirseATopologia(this);

        LoggerCentral.info(TAG, "GrafoRedCompleta suscrito a eventos de topología");

        // Cargar datos iniciales
        actualizarDesdeControlador();
    }

    /**
     * ✅ NUEVO: Actualizar el grafo con la topología completa sincronizada
     */
    private void actualizarDesdeControlador() {
        if (controlador == null) return;

        SwingUtilities.invokeLater(() -> {
            limpiar();

            // Obtener topología completa (peers + clientes de TODOS los peers)
            Map<String, DTOTopologiaRed> topologia = controlador.obtenerTopologiaCompleta();

            if (topologia.isEmpty()) {
                LoggerCentral.debug(TAG, "Topología vacía, no hay datos para mostrar");
                repaint();
                return;
            }

            LoggerCentral.info(TAG, "📊 Actualizando desde controlador: " + topologia.size() + " peers");
            LoggerCentral.debug(TAG, "Keys de topología recibida: " + topologia.keySet());

            // Agregar todos los peers
            for (Map.Entry<String, DTOTopologiaRed> entry : topologia.entrySet()) {
                String keyTopologia = entry.getKey();
                DTOTopologiaRed topo = entry.getValue();

                boolean esLocal = "LOCAL".equalsIgnoreCase(topo.getIdPeer());
                boolean esOnline = "ONLINE".equalsIgnoreCase(topo.getEstadoPeer());

                LoggerCentral.debug(TAG, "📍 Procesando peer - Key: '" + keyTopologia +
                    "' | IdPeer: '" + topo.getIdPeer() + "' | IP: " + topo.getIpPeer());

                // Usar la KEY del mapa como ID del peer (es la que se usa para buscar después)
                agregarPeer(keyTopologia, topo.getIpPeer(), esLocal, esOnline);

                // Agregar clientes de este peer
                for (DTOSesionCliente cliente : topo.getClientesConectados()) {
                    String nombreCliente = cliente.getIdUsuario() != null ?
                        cliente.getIdUsuario() : cliente.getIdSesion();
                    boolean clienteOnline = "AUTENTICADO".equalsIgnoreCase(cliente.getEstado());
                    agregarUsuario(nombreCliente, keyTopologia, clienteOnline);
                }
            }

            // Verificar peers agregados
            LoggerCentral.info(TAG, "🔍 Peers agregados al mapa: " + peers.keySet());

            // Agregar conexiones P2P (todos conectados entre sí en malla completa)
            List<String> peerIds = new ArrayList<>(peers.keySet());
            LoggerCentral.info(TAG, "🔗 Creando conexiones P2P entre " + peerIds.size() + " peers...");
            LoggerCentral.debug(TAG, "IDs de peers para conexiones: " + peerIds);

            int conexionesCreadas = 0;
            for (int i = 0; i < peerIds.size(); i++) {
                for (int j = i + 1; j < peerIds.size(); j++) {
                    String id1 = peerIds.get(i);
                    String id2 = peerIds.get(j);
                    LoggerCentral.debug(TAG, "Intentando conectar: '" + id1 + "' <-> '" + id2 + "'");
                    agregarConexionP2P(id1, id2);
                    conexionesCreadas++;
                }
            }

            LoggerCentral.info(TAG, "✅ Conexiones P2P totales: " + conexionesP2P.size() +
                              " (intentadas: " + conexionesCreadas + ")");

            repaint();

            int totalClientes = topologia.values().stream()
                .mapToInt(DTOTopologiaRed::getNumeroClientes)
                .sum();

            LoggerCentral.info(TAG, "Grafo Red Completa actualizado: " +
                topologia.size() + " peers, " + totalClientes + " clientes");
        });
    }

    private void configurarPanel() {
        this.setBackground(Color.WHITE);
        this.setBorder(BorderFactory.createLineBorder(new Color(189, 195, 199), 1));
        // Tamaño más grande para mejor visualización con zoom
        this.setPreferredSize(new Dimension(1000, 800));
    }

    public void agregarPeer(String id, String ip, boolean esLocal, boolean esOnline) {
        NodoPeer peer = new NodoPeer(id, ip, esLocal, esOnline);
        peers.put(id, peer);
        calcularPosiciones();
        repaint();
    }

    public void agregarUsuario(String nombre, String peerId, boolean esOnline) {
        NodoPeer peer = peers.get(peerId);
        if (peer != null) {
            NodoUsuario usuario = new NodoUsuario(nombre, peer, esOnline);
            usuarios.add(usuario);
            peer.usuarios.add(usuario);
            calcularPosiciones();
            repaint();
        }
    }

    public void agregarConexionP2P(String idOrigen, String idDestino) {
        NodoPeer origen = peers.get(idOrigen);
        NodoPeer destino = peers.get(idDestino);

        if (origen != null && destino != null) {
            conexionesP2P.add(new ConexionP2P(origen, destino));
            LoggerCentral.debug(TAG, "✅ Conexión P2P agregada: " + idOrigen + " <-> " + idDestino);
            repaint();
        } else {
            LoggerCentral.warn(TAG, "❌ No se pudo agregar conexión P2P: origen=" +
                (origen != null ? "OK" : "NULL") + " destino=" + (destino != null ? "OK" : "NULL") +
                " | IDs: " + idOrigen + " <-> " + idDestino);
            LoggerCentral.debug(TAG, "Peers disponibles: " + peers.keySet());
        }
    }

    public void limpiar() {
        peers.clear();
        usuarios.clear();
        conexionesP2P.clear();
        repaint();
    }

    private void calcularPosiciones() {
        int width = getWidth();
        int height = getHeight();

        if (width == 0 || height == 0) {
            width = 600;
            height = 400;
        }

        if (peers.isEmpty()) return;

        int centerX = width / 2;
        int centerY = height / 2;
        int radioPeers = Math.min(width, height) / 3;

        // Distribuir peers en círculo
        int cantidadPeers = peers.size();
        int i = 0;
        for (NodoPeer peer : peers.values()) {
            double angulo = 2 * Math.PI * i / cantidadPeers;
            peer.x = (int) (centerX + radioPeers * Math.cos(angulo));
            peer.y = (int) (centerY + radioPeers * Math.sin(angulo));

            // Posicionar usuarios FUERA del círculo de peers
            int cantidadUsuarios = peer.usuarios.size();
            if (cantidadUsuarios > 0) {
                // Radio más grande para usuarios, colocándolos fuera
                int radioUsuarios = 90;
                for (int j = 0; j < cantidadUsuarios; j++) {
                    // Calcular ángulo basado en la posición del peer
                    double anguloBase = angulo;
                    double rangoAngular = Math.PI / 4; // 45 grados de rango
                    double anguloUsuario = anguloBase + rangoAngular * ((j - (cantidadUsuarios - 1) / 2.0) / Math.max(cantidadUsuarios - 1, 1));

                    NodoUsuario usuario = peer.usuarios.get(j);
                    usuario.x = (int) (peer.x + radioUsuarios * Math.cos(anguloUsuario));
                    usuario.y = (int) (peer.y + radioUsuarios * Math.sin(anguloUsuario));
                }
            }

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

        // Dibujar conexiones P2P
        g2d.setStroke(new BasicStroke(2.5f));
        for (ConexionP2P conexion : conexionesP2P) {
            g2d.setColor(COLOR_CONEXION_P2P);
            g2d.draw(new Line2D.Double(
                conexion.origen.x, conexion.origen.y,
                conexion.destino.x, conexion.destino.y
            ));
        }

        // Dibujar conexiones Cliente-Servidor
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.setColor(COLOR_CONEXION_CS);
        for (NodoUsuario usuario : usuarios) {
            g2d.draw(new Line2D.Double(
                usuario.peer.x, usuario.peer.y,
                usuario.x, usuario.y
            ));
        }

        // Dibujar usuarios
        for (NodoUsuario usuario : usuarios) {
            dibujarUsuario(g2d, usuario);
        }

        // Dibujar peers
        for (NodoPeer peer : peers.values()) {
            dibujarPeer(g2d, peer);
        }

        if (peers.isEmpty()) {
            g2d.setColor(Color.GRAY);
            g2d.setFont(new Font("Arial", Font.ITALIC, 14));
            String mensaje = "No hay conexiones activas";
            FontMetrics fm = g2d.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(mensaje)) / 2;
            int y = getHeight() / 2;
            g2d.drawString(mensaje, x, y);
        }
    }

    private void dibujarPeer(Graphics2D g2d, NodoPeer peer) {
        int radio = 28;

        Color color;
        if (peer.esLocal) {
            color = COLOR_PEER_LOCAL;
        } else if (peer.esOnline) {
            color = COLOR_PEER_ONLINE;
        } else {
            color = COLOR_PEER_OFFLINE;
        }

        // Dibujar cuadrado para el peer
        Rectangle2D rect = new Rectangle2D.Double(
            peer.x - radio, peer.y - radio,
            radio * 2, radio * 2
        );
        g2d.setColor(color);
        g2d.fill(rect);

        g2d.setColor(color.darker());
        g2d.setStroke(new BasicStroke(2.5f));
        g2d.draw(rect);

        // ID
        g2d.setColor(COLOR_TEXTO);
        g2d.setFont(new Font("Arial", Font.BOLD, 9));
        String idTruncado = peer.id.length() > 8 ? peer.id.substring(0, 8) + "..." : peer.id;
        FontMetrics fm = g2d.getFontMetrics();
        int textoX = peer.x - fm.stringWidth(idTruncado) / 2;
        int textoY = peer.y + radio + 14;
        g2d.drawString(idTruncado, textoX, textoY);

        // Contador
        g2d.setFont(new Font("Arial", Font.BOLD, 11));
        g2d.setColor(Color.WHITE);
        String contador = String.valueOf(peer.usuarios.size());
        fm = g2d.getFontMetrics();
        int contX = peer.x - fm.stringWidth(contador) / 2;
        int contY = peer.y + 5;
        g2d.drawString(contador, contX, contY);
    }

    private void dibujarUsuario(Graphics2D g2d, NodoUsuario usuario) {
        int radio = 12;

        Color color = usuario.esOnline ? COLOR_USUARIO_ONLINE : COLOR_USUARIO_OFFLINE;

        Ellipse2D circulo = new Ellipse2D.Double(
            usuario.x - radio, usuario.y - radio,
            radio * 2, radio * 2
        );
        g2d.setColor(color);
        g2d.fill(circulo);

        g2d.setColor(color.darker());
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.draw(circulo);

        // Nombre
        g2d.setColor(COLOR_TEXTO);
        g2d.setFont(new Font("Arial", Font.PLAIN, 8));
        String nombreTruncado = usuario.nombre.length() > 8 ?
                                usuario.nombre.substring(0, 8) + "..." : usuario.nombre;
        FontMetrics fm = g2d.getFontMetrics();
        int textoX = usuario.x - fm.stringWidth(nombreTruncado) / 2;
        int textoY = usuario.y + radio + 10;
        g2d.drawString(nombreTruncado, textoX, textoY);
    }

    /**
     * ✅ IMPLEMENTACIÓN IObservador
     */
    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        LoggerCentral.debug(TAG, "Evento recibido: " + tipoDeDato);

        switch (tipoDeDato) {
            case "TOPOLOGIA_ACTUALIZADA":
            case "TOPOLOGIA_REMOTA_RECIBIDA":
                // La topología cambió, actualizar todo el grafo
                if (datos instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, DTOTopologiaRed> nuevaTopologia =
                        (Map<String, DTOTopologiaRed>) datos;

                    SwingUtilities.invokeLater(() -> {
                        actualizarConTopologia(nuevaTopologia);
                    });
                }
                break;

            case "PEER_CONECTADO":
            case "PEER_DESCONECTADO":
            case "CLIENTE_CONECTADO":
            case "CLIENTE_DESCONECTADO":
            case "USUARIO_AUTENTICADO":
            case "USUARIO_DESCONECTADO":
            case "CLIENTE_AUTENTICADO":
                // Un peer o cliente cambió de estado, refrescar todo el grafo
                LoggerCentral.info(TAG, "🔄 Actualizando grafo completo por evento: " + tipoDeDato);
                SwingUtilities.invokeLater(this::actualizarDesdeControlador);
                break;

            default:
                LoggerCentral.debug(TAG, "Evento no manejado: " + tipoDeDato);
        }
    }

    /**
     * ✅ NUEVO: Actualizar con topología recibida directamente
     */
    private void actualizarConTopologia(Map<String, DTOTopologiaRed> topologia) {
        limpiar();

        // Agregar todos los peers y sus clientes
        for (DTOTopologiaRed topo : topologia.values()) {
            boolean esLocal = "LOCAL".equalsIgnoreCase(topo.getIdPeer());
            boolean esOnline = "ONLINE".equalsIgnoreCase(topo.getEstadoPeer());
            agregarPeer(topo.getIdPeer(), topo.getIpPeer(), esLocal, esOnline);

            for (DTOSesionCliente cliente : topo.getClientesConectados()) {
                String nombreCliente = cliente.getIdUsuario() != null ?
                    cliente.getIdUsuario() : cliente.getIdSesion();
                boolean clienteOnline = "AUTENTICADO".equalsIgnoreCase(cliente.getEstado());
                agregarUsuario(nombreCliente, topo.getIdPeer(), clienteOnline);
            }
        }

        // Conectar todos los peers entre sí
        List<String> peerIds = new ArrayList<>(topologia.keySet());
        for (int i = 0; i < peerIds.size(); i++) {
            for (int j = i + 1; j < peerIds.size(); j++) {
                agregarConexionP2P(peerIds.get(i), peerIds.get(j));
            }
        }

        repaint();
    }

    // Clases internas
    private static class NodoPeer {
        String id;
        String ip;
        boolean esLocal;
        boolean esOnline;
        List<NodoUsuario> usuarios;
        int x, y;

        NodoPeer(String id, String ip, boolean esLocal, boolean esOnline) {
            this.id = id;
            this.ip = ip;
            this.esLocal = esLocal;
            this.esOnline = esOnline;
            this.usuarios = new ArrayList<>();
            this.x = 0;
            this.y = 0;
        }
    }

    private static class NodoUsuario {
        String nombre;
        NodoPeer peer;
        boolean esOnline;
        int x, y;

        NodoUsuario(String nombre, NodoPeer peer, boolean esOnline) {
            this.nombre = nombre;
            this.peer = peer;
            this.esOnline = esOnline;
            this.x = 0;
            this.y = 0;
        }
    }

    private static class ConexionP2P {
        NodoPeer origen;
        NodoPeer destino;

        ConexionP2P(NodoPeer origen, NodoPeer destino) {
            this.origen = origen;
            this.destino = destino;
        }
    }
}
