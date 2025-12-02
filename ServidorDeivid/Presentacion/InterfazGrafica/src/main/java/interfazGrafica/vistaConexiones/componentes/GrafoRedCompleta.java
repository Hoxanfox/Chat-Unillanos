package interfazGrafica.vistaConexiones.componentes;

import controlador.p2p.ControladorP2P;
import dto.topologia.DTOTopologiaRed;
import dto.cliente.DTOSesionCliente;
import logger.LoggerCentral;
import observador.IObservador;
import configuracion.Configuracion;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Componente que representa un grafo unificado mostrando toda la red:
 * Peers P2P conectados entre sí y usuarios conectados a cada peer
 * ✅ ACTUALIZADO: Ahora implementa IObservador y usa topología sincronizada automáticamente
 */
public class GrafoRedCompleta extends JPanel implements IObservador {

    private static final String TAG = "GrafoRedCompleta";

    private final Map<String, NodoPeer> peers;
    private final List<NodoUsuario> usuarios;
    private final List<ConexionP2P> conexionesP2P;
    private ControladorP2P controlador;

    // ✅ NUEVO: Información del peer local para identificarlo correctamente
    private String idPeerLocal;
    private String ipLocal;
    private int puertoLocal;

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
        cargarConfiguracionLocal();
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
            // Obtener ID del peer local desde el controlador
            obtenerIdPeerLocalDesdeControlador();
        }
    }

    /**
     * ✅ NUEVO: Cargar la configuración del peer local
     */
    private void cargarConfiguracionLocal() {
        try {
            Configuracion config = Configuracion.getInstance();
            this.ipLocal = config.getPeerHost();
            this.puertoLocal = config.getPeerPuerto();
            LoggerCentral.info(TAG, "Configuración local cargada: " + ipLocal + ":" + puertoLocal);
        } catch (Exception e) {
            this.ipLocal = "127.0.0.1";
            this.puertoLocal = 8000;
            LoggerCentral.warn(TAG, "Error cargando configuración, usando valores por defecto");
        }
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
     * ✅ NUEVO: Determina si un peer es el local basándose en múltiples criterios
     */
    private boolean esPeerLocal(String idPeer, String ip) {
        // Criterio 1: Comparar por ID del peer
        if (idPeerLocal != null && idPeerLocal.equals(idPeer)) {
            return true;
        }
        
        // Criterio 2: Comparar por ID "LOCAL" (convención)
        if ("LOCAL".equalsIgnoreCase(idPeer)) {
            return true;
        }
        
        // Criterio 3: Comparar por IP local
        String ipNormalizada = normalizarIP(ip);
        String ipLocalNormalizada = normalizarIP(ipLocal);
        if (ipNormalizada.equals(ipLocalNormalizada)) {
            return true;
        }
        
        // Criterio 4: IPs típicas de localhost
        if (ipNormalizada.equals("127.0.0.1") || ipNormalizada.equals("localhost")) {
            return true;
        }
        
        return false;
    }

    /**
     * ✅ NUEVO: Normaliza direcciones IP
     */
    private String normalizarIP(String ip) {
        if (ip == null || ip.isEmpty()) return "127.0.0.1";
        
        // Remover prefijo "/" si existe
        if (ip.startsWith("/")) {
            ip = ip.substring(1);
        }
        
        // Normalizar localhost y 0.0.0.0
        if (ip.equals("localhost") || ip.equals("0.0.0.0")) {
            return "127.0.0.1";
        }
        
        return ip;
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

            // Agregar todos los peers usando SIEMPRE idPeer como clave interna
            for (Map.Entry<String, DTOTopologiaRed> entry : topologia.entrySet()) {
                DTOTopologiaRed topo = entry.getValue();

                String idPeer = topo.getIdPeer();
                String ipPeer = topo.getIpPeer();
                int puertoPeer = topo.getPuertoPeer();
                
                // ✅ CORREGIDO: Usar método mejorado para detectar peer local
                boolean esLocal = esPeerLocal(idPeer, ipPeer);
                boolean esOnline = "ONLINE".equalsIgnoreCase(topo.getEstadoPeer());

                // ✅ NUEVO: Mostrar IP con puerto para mejor identificación
                String ipDisplay = (puertoPeer > 0) ? ipPeer + ":" + puertoPeer : ipPeer;

                LoggerCentral.debug(TAG, "📍 Procesando peer - KeyMap: '" + entry.getKey() +
                    "' | IdPeer: '" + idPeer + "' | IP: " + ipDisplay + " | Local: " + esLocal);

                agregarPeer(idPeer, ipDisplay, esLocal, esOnline);

                // Agregar clientes de este peer colgando del mismo idPeer
                for (DTOSesionCliente cliente : topo.getClientesConectados()) {
                    String nombreCliente = cliente.getIdUsuario() != null ?
                        cliente.getIdUsuario() : cliente.getIdSesion();
                    boolean clienteOnline = "AUTENTICADO".equalsIgnoreCase(cliente.getEstado());
                    agregarUsuario(nombreCliente, idPeer, clienteOnline);
                }
            }

            LoggerCentral.info(TAG, "🔍 Peers agregados al mapa: " + peers.keySet());

            // Agregar conexiones P2P (malla completa entre todos los peers conocidos)
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

        // ✅ NUEVO: Agregar listener para clics en usuarios (desconectar)
        agregarListenerClics();
    }

    /**
     * ✅ NUEVO: Agregar listener de clics para desconectar clientes
     */
    private void agregarListenerClics() {
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (controlador == null) return;

                // Detectar clic derecho o doble clic en un usuario
                boolean clicDerecho = SwingUtilities.isRightMouseButton(evt);
                boolean dobleClick = evt.getClickCount() == 2;

                if (clicDerecho || dobleClick) {
                    int mouseX = evt.getX();
                    int mouseY = evt.getY();

                    // Buscar usuario bajo el cursor
                    for (NodoUsuario usuario : usuarios) {
                        int radio = 15;
                        double distancia = Math.sqrt(Math.pow(mouseX - usuario.x, 2) + Math.pow(mouseY - usuario.y, 2));

                        if (distancia <= radio) {
                            // Usuario encontrado - confirmar desconexión
                            confirmarYDesconectarCliente(usuario);
                            break;
                        }
                    }
                }
            }
        });

        // ✅ NUEVO: Tooltip para indicar cómo desconectar
        setToolTipText("Clic derecho o doble clic en un usuario para desconectarlo");
    }

    /**
     * ✅ NUEVO: Confirmar y desconectar un cliente
     */
    private void confirmarYDesconectarCliente(NodoUsuario usuario) {
        String nombreMostrar = usuario.nombre != null ? usuario.nombre : "Cliente anónimo";
        String peerMostrar = usuario.peer != null ? usuario.peer.id : "desconocido";
        String estado = usuario.esOnline ? "autenticado" : "conectado";

        int opcion = JOptionPane.showConfirmDialog(
            this,
            "¿Desconectar a " + nombreMostrar + " (" + estado + ") del peer " + peerMostrar + "?",
            "Confirmar Desconexión",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (opcion == JOptionPane.YES_OPTION) {
            // Obtener topología completa
            Map<String, DTOTopologiaRed> topologia = controlador.obtenerTopologiaCompleta();

            boolean encontrado = false;
            for (DTOTopologiaRed topo : topologia.values()) {
                for (DTOSesionCliente cliente : topo.getClientesConectados()) {
                    String idUsuario = cliente.getIdUsuario();
                    String idSesion = cliente.getIdSesion();

                    // Comparar por idUsuario si está autenticado, o por idSesion si no
                    boolean coincide;
                    if (usuario.esOnline && idUsuario != null) {
                        coincide = usuario.nombre.equals(idUsuario);
                    } else {
                        coincide = usuario.nombre.equals(idSesion);
                    }

                    if (coincide) {
                        // Verificar si es cliente local o remoto
                        boolean esLocal = "LOCAL".equalsIgnoreCase(topo.getIdPeer());

                        if (esLocal) {
                            // ✅ CORREGIDO: Cliente local - informar al usuario que debe usar el grafo Cliente-Servidor
                            LoggerCentral.info(TAG, "Cliente local identificado: " + nombreMostrar);
                            JOptionPane.showMessageDialog(
                                this,
                                "Para desconectar clientes locales, use el grafo 'Cliente-Servidor'.\n" +
                                "Este grafo muestra la red completa P2P y no permite gestionar clientes directamente.",
                                "Cliente Local",
                                JOptionPane.INFORMATION_MESSAGE
                            );
                        } else {
                            // Cliente remoto - no se puede desconectar
                            JOptionPane.showMessageDialog(
                                this,
                                "No se puede desconectar clientes remotos.\n" +
                                "Los clientes remotos son gestionados por su peer correspondiente.",
                                "Cliente Remoto",
                                JOptionPane.INFORMATION_MESSAGE
                            );
                        }

                        encontrado = true;
                        break;
                    }
                }
                if (encontrado) break;
            }

            if (!encontrado) {
                LoggerCentral.warn(TAG, "Cliente no encontrado: " + nombreMostrar);
                JOptionPane.showMessageDialog(
                    this,
                    "Cliente no encontrado en la topología actual",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        }
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
            width = 1000;
            height = 800;
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
                    double rangoAngular = Math.PI / 4; // 45 grados de rango
                    double anguloUsuario = angulo + rangoAngular * ((j - (cantidadUsuarios - 1) / 2.0) / Math.max(cantidadUsuarios - 1, 1));

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
            g2d.drawLine(conexion.origen.x, conexion.origen.y, conexion.destino.x, conexion.destino.y);
        }

        // Dibujar conexiones Cliente-Servidor (usuarios a peers)
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.setColor(COLOR_CONEXION_CS);
        for (NodoUsuario usuario : usuarios) {
            g2d.drawLine(usuario.peer.x, usuario.peer.y, usuario.x, usuario.y);
        }

        // Dibujar usuarios
        for (NodoUsuario usuario : usuarios) {
            dibujarUsuario(g2d, usuario);
        }

        // Dibujar peers
        for (NodoPeer peer : peers.values()) {
            dibujarPeer(g2d, peer);
        }

        // Si no hay peers, mostrar mensaje
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
        int radio = 30;

        // Determinar color según estado
        Color color;
        if (peer.esLocal) {
            color = COLOR_PEER_LOCAL;
        } else if (peer.esOnline) {
            color = COLOR_PEER_ONLINE;
        } else {
            color = COLOR_PEER_OFFLINE;
        }

        // ✅ MODIFICADO: Dibujar CUADRADO para el peer en lugar de círculo
        int x = peer.x - radio;
        int y = peer.y - radio;
        int tamaño = radio * 2;

        g2d.setColor(color);
        g2d.fillRect(x, y, tamaño, tamaño);

        g2d.setColor(color.darker());
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(x, y, tamaño, tamaño);

        // Texto: ID
        g2d.setColor(COLOR_TEXTO);
        g2d.setFont(new Font("Arial", Font.BOLD, 10));
        String idTruncado = peer.id.length() > 8 ? peer.id.substring(0, 8) + "..." : peer.id;
        FontMetrics fm = g2d.getFontMetrics();
        int textoX = peer.x - fm.stringWidth(idTruncado) / 2;
        int textoY = peer.y + radio + 15;
        g2d.drawString(idTruncado, textoX, textoY);

        // IP
        g2d.setFont(new Font("Arial", Font.PLAIN, 9));
        fm = g2d.getFontMetrics();
        int ipX = peer.x - fm.stringWidth(peer.ip) / 2;
        int ipY = textoY + 12;
        g2d.drawString(peer.ip, ipX, ipY);

        // Contador de usuarios
        g2d.setFont(new Font("Arial", Font.BOLD, 10));
        g2d.setColor(Color.WHITE);
        String contador = String.valueOf(peer.usuarios.size());
        fm = g2d.getFontMetrics();
        int contX = peer.x - fm.stringWidth(contador) / 2;
        int contY = peer.y + 5;
        g2d.drawString(contador, contX, contY);
    }

    private void dibujarUsuario(Graphics2D g2d, NodoUsuario usuario) {
        int radio = 15;

        Color color = usuario.esOnline ? COLOR_USUARIO_ONLINE : COLOR_USUARIO_OFFLINE;

        // Dibujar círculo
        g2d.setColor(color);
        g2d.fillOval(usuario.x - radio, usuario.y - radio, radio * 2, radio * 2);

        g2d.setColor(color.darker());
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawOval(usuario.x - radio, usuario.y - radio, radio * 2, radio * 2);

        // Nombre del usuario
        g2d.setColor(COLOR_TEXTO);
        g2d.setFont(new Font("Arial", Font.BOLD, 9));
        String nombreTruncado = usuario.nombre != null && usuario.nombre.length() > 12 ?
                                usuario.nombre.substring(0, 12) + "..." :
                                (usuario.nombre != null ? usuario.nombre : "Anónimo");
        FontMetrics fm = g2d.getFontMetrics();
        int textoX = usuario.x - fm.stringWidth(nombreTruncado) / 2;
        int textoY = usuario.y + radio + 12;
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
            case "PEER_CONECTADO":
            case "PEER_DESCONECTADO":
            case "CLIENTE_CONECTADO":
            case "CLIENTE_DESCONECTADO":
            case "SESIONES_ACTUALIZADAS":
                // Actualizar grafo completo
                LoggerCentral.info(TAG, "🔄 Actualizando grafo red completa por evento: " + tipoDeDato);
                SwingUtilities.invokeLater(this::actualizarDesdeControlador);
                break;

            default:
                LoggerCentral.debug(TAG, "Evento no manejado: " + tipoDeDato);
        }
    }

    // =========================================================================
    // CLASES INTERNAS
    // =========================================================================

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

