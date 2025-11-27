package interfazGrafica.vistaConexiones.componentes;

import controlador.p2p.ControladorP2P;
import dto.p2p.DTOPeerDetails;
import logger.LoggerCentral;
import observador.IObservador;

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
     * ✅ NUEVO: Suscribirse a cambios en los peers P2P
     */
    private void suscribirseAEventos() {
        // Suscribirse a eventos de conexión/desconexión
        controlador.suscribirseAEventosConexion();

        // Registrarse como observador
        controlador.suscribirActualizacionLista(this::actualizarConListaPeers);
        controlador.suscribirConexionPeer(peer -> actualizarDesdeControlador());
        controlador.suscribirDesconexionPeer(peer -> actualizarDesdeControlador());

        LoggerCentral.info(TAG, "GrafoP2P suscrito a eventos de peers");

        // Cargar datos iniciales
        actualizarDesdeControlador();
    }

    /**
     * ✅ NUEVO: Actualizar con lista de peers
     */
    private void actualizarConListaPeers(List<DTOPeerDetails> peers) {
        SwingUtilities.invokeLater(() -> {
            limpiar();
            for (DTOPeerDetails peer : peers) {
                boolean esLocal = "LOCAL".equalsIgnoreCase(peer.getId());
                boolean esOnline = "ONLINE".equalsIgnoreCase(peer.getEstado());
                agregarPeer(peer.getId(), peer.getIp(), esLocal, esOnline);
            }
            // Las conexiones se infieren (todos conectados entre sí en P2P)
            agregarConexionesP2P(peers);
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
     * ✅ NUEVO: Agregar conexiones P2P (todos los peers se conectan entre sí)
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

    public void agregarPeer(String id, String ip, boolean esLocal, boolean esOnline) {
        NodoP2P nodo = new NodoP2P(id, ip, esLocal, esOnline);
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
            color = COLOR_PEER_LOCAL;
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

        // Texto: ID (truncado)
        g2d.setColor(COLOR_TEXTO);
        g2d.setFont(new Font("Arial", Font.BOLD, 10));
        String idTruncado = nodo.id.length() > 8 ? nodo.id.substring(0, 8) + "..." : nodo.id;
        FontMetrics fm = g2d.getFontMetrics();
        int textoX = nodo.x - fm.stringWidth(idTruncado) / 2;
        int textoY = nodo.y + radio + 15;
        g2d.drawString(idTruncado, textoX, textoY);

        // IP
        g2d.setFont(new Font("Arial", Font.PLAIN, 9));
        fm = g2d.getFontMetrics();
        int ipX = nodo.x - fm.stringWidth(nodo.ip) / 2;
        int ipY = textoY + 12;
        g2d.drawString(nodo.ip, ipX, ipY);
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
            case "LISTA_PEERS":
            case "CLIENTE_CONECTADO":
            case "CLIENTE_DESCONECTADO":
            case "TOPOLOGIA_ACTUALIZADA":
                // Actualizar grafo cuando cambian los peers o la topología de red
                LoggerCentral.info(TAG, "🔄 Actualizando grafo P2P por evento: " + tipoDeDato);
                SwingUtilities.invokeLater(this::actualizarDesdeControlador);
                break;

            default:
                LoggerCentral.debug(TAG, "Evento no manejado: " + tipoDeDato);
        }
    }

    // Clase interna para representar un nodo P2P
    private static class NodoP2P {
        String id;
        String ip;
        boolean esLocal;
        boolean esOnline;
        int x, y;

        NodoP2P(String id, String ip, boolean esLocal, boolean esOnline) {
            this.id = id;
            this.ip = ip;
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
