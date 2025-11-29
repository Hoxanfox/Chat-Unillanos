package interfazGrafica.vistaConexiones.componentes;

import controlador.clienteServidor.ControladorClienteServidor;
import dto.cliente.DTOSesionCliente;
import logger.LoggerCentral;
import observador.IObservador;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.*;
import java.util.List;

/**
 * Componente que representa un grafo visual de la red Cliente-Servidor
 * Muestra los usuarios conectados a peers específicos
 * ✅ ACTUALIZADO: Ahora implementa IObservador para actualización automática
 */
public class GrafoClienteServidor extends JPanel implements IObservador {

    private static final String TAG = "GrafoClienteServidor";

    private Map<String, NodoPeer> peers;
    private List<NodoUsuario> usuarios;
    private ControladorClienteServidor controlador;

    // Colores para el grafo
    private static final Color COLOR_PEER = new Color(52, 152, 219);      // Azul
    private static final Color COLOR_USUARIO_AUTENTICADO = new Color(46, 204, 113);  // Verde - Usuario autenticado
    private static final Color COLOR_USUARIO_CONECTADO = new Color(241, 196, 15);     // Amarillo - Solo conectado, sin autenticar
    private static final Color COLOR_USUARIO_OFFLINE = new Color(149, 165, 166);      // Gris - Offline
    private static final Color COLOR_CONEXION = new Color(52, 73, 94);    // Gris oscuro
    private static final Color COLOR_TEXTO = Color.BLACK;

    public GrafoClienteServidor() {
        this.peers = new LinkedHashMap<>();
        this.usuarios = new ArrayList<>();
        configurarPanel();
    }

    /**
     * ✅ NUEVO: Constructor con controlador para suscribirse a eventos
     */
    public GrafoClienteServidor(ControladorClienteServidor controlador) {
        this();
        this.controlador = controlador;
        if (controlador != null) {
            suscribirseAEventos();
        }
    }

    /**
     * ✅ ACTUALIZADO: Suscribirse a cambios usando los callbacks del controlador
     */
    private void suscribirseAEventos() {
        // Usar callbacks del controlador en lugar de registrar como observador
        controlador.setOnClienteConectado(idCliente -> {
            LoggerCentral.debug(TAG, "Cliente conectado: " + idCliente);
            SwingUtilities.invokeLater(this::actualizarDesdeControlador);
        });

        controlador.setOnClienteDesconectado(idCliente -> {
            LoggerCentral.debug(TAG, "Cliente desconectado: " + idCliente);
            SwingUtilities.invokeLater(this::actualizarDesdeControlador);
        });

        controlador.setOnRedIniciada(info -> {
            LoggerCentral.info(TAG, "Red iniciada: " + info);
            SwingUtilities.invokeLater(this::actualizarDesdeControlador);
        });

        controlador.setOnRedDetenida(info -> {
            LoggerCentral.info(TAG, "Red detenida: " + info);
            SwingUtilities.invokeLater(() -> {
                limpiar();
                repaint();
            });
        });

        LoggerCentral.info(TAG, "GrafoClienteServidor suscrito a eventos de sesiones");

        // Cargar datos iniciales
        actualizarDesdeControlador();
    }

    /**
     * ✅ MEJORADO: Actualizar el grafo con datos del controlador
     * Ahora diferencia entre clientes conectados y autenticados con información completa
     */
    private void actualizarDesdeControlador() {
        if (controlador == null) return;

        limpiar();

        // Obtener sesiones activas del controlador
        List<DTOSesionCliente> sesiones = controlador.getSesionesActivas();

        if (!sesiones.isEmpty()) {
            // Agregar el servidor como peer único (arquitectura Cliente-Servidor)
            String idServidor = "SERVER-LOCAL";
            String ipServidor = "localhost:8000";
            agregarPeer(idServidor, ipServidor);

            // Agregar cada cliente conectado
            int autenticados = 0;
            int conectados = 0;

            for (DTOSesionCliente sesion : sesiones) {
                boolean estaAutenticado = sesion.estaAutenticado();

                // Determinar qué nombre mostrar
                String nombreCliente;
                if (estaAutenticado && sesion.getIdUsuario() != null) {
                    // Usuario autenticado - mostrar el ID de usuario
                    nombreCliente = sesion.getIdUsuario();
                    autenticados++;
                    LoggerCentral.debug(TAG, "✓ Cliente AUTENTICADO: " + nombreCliente + " (" + sesion.getIdSesion() + ")");
                } else {
                    // Cliente solo conectado - mostrar ID de sesión
                    nombreCliente = sesion.getIdSesion();
                    conectados++;
                    LoggerCentral.debug(TAG, "○ Cliente CONECTADO: " + sesion.getIdSesion());
                }

                // Agregar al grafo con el estado correcto
                agregarUsuario(nombreCliente, idServidor, estaAutenticado);
            }

            LoggerCentral.info(TAG, String.format("📊 Grafo actualizado: %d autenticados (verde), %d conectados (amarillo)",
                autenticados, conectados));
        }

        SwingUtilities.invokeLater(this::repaint);
    }

    private void configurarPanel() {
        this.setBackground(Color.WHITE);
        this.setBorder(BorderFactory.createLineBorder(new Color(189, 195, 199), 1));
        // Tamaño más grande para mejor visualización con zoom
        this.setPreferredSize(new Dimension(800, 600));

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
        String estado = usuario.esOnline ? "autenticado" : "conectado";

        int opcion = JOptionPane.showConfirmDialog(
            this,
            "¿Desconectar a " + nombreMostrar + " (" + estado + ")?",
            "Confirmar Desconexión",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (opcion == JOptionPane.YES_OPTION) {
            // Buscar la sesión real del usuario
            List<dto.cliente.DTOSesionCliente> sesiones = controlador.getSesionesActivas();
            for (dto.cliente.DTOSesionCliente sesion : sesiones) {
                String idUsuario = sesion.getIdUsuario();
                String idSesion = sesion.getIdSesion();

                // Comparar por idUsuario si está autenticado, o por idSesion si no
                boolean coincide = false;
                if (usuario.esOnline && idUsuario != null) {
                    coincide = usuario.nombre.equals(idUsuario);
                } else {
                    coincide = usuario.nombre.equals(idSesion);
                }

                if (coincide) {
                    boolean exito = controlador.desconectarCliente(idSesion);
                    if (exito) {
                        LoggerCentral.info(TAG, "✓ Cliente desconectado exitosamente: " + nombreMostrar);
                        JOptionPane.showMessageDialog(
                            this,
                            "Cliente desconectado exitosamente",
                            "Desconexión Exitosa",
                            JOptionPane.INFORMATION_MESSAGE
                        );
                    } else {
                        LoggerCentral.error(TAG, "✗ Error al desconectar cliente: " + nombreMostrar);
                        JOptionPane.showMessageDialog(
                            this,
                            "Error al desconectar el cliente",
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                        );
                    }
                    break;
                }
            }
        }
    }

    public void agregarPeer(String id, String ip) {
        NodoPeer nodo = new NodoPeer(id, ip);
        peers.put(id, nodo);
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

    public void limpiar() {
        peers.clear();
        usuarios.clear();
        repaint();
    }

    private void calcularPosiciones() {
        int width = getWidth();
        int height = getHeight();

        if (width == 0 || height == 0) {
            width = 800;
            height = 600;
        }

        if (peers.isEmpty()) return;

        // Posicionar peers centrados horizontalmente en la parte superior-media
        int peerY = height / 3;
        int espacioPeers = width / (peers.size() + 1);

        int i = 1;
        for (NodoPeer peer : peers.values()) {
            peer.x = espacioPeers * i;
            peer.y = peerY;

            // Posicionar usuarios en semicírculo debajo del peer
            int cantidadUsuarios = peer.usuarios.size();
            if (cantidadUsuarios > 0) {
                int radioSemiCirculo = 80;
                int centroY = peerY + 100;

                for (int j = 0; j < cantidadUsuarios; j++) {
                    double angulo = Math.PI * (j + 1) / (cantidadUsuarios + 1);
                    NodoUsuario usuario = peer.usuarios.get(j);
                    usuario.x = (int) (peer.x + radioSemiCirculo * Math.cos(angulo + Math.PI));
                    usuario.y = (int) (centroY + radioSemiCirculo * Math.sin(angulo));
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

        // Dibujar conexiones
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.setColor(COLOR_CONEXION);
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

        // Dibujar cuadrado para el peer
        int x = peer.x - radio;
        int y = peer.y - radio;

        g2d.setColor(COLOR_PEER);
        g2d.fillRect(x, y, radio * 2, radio * 2);

        g2d.setColor(COLOR_PEER.darker());
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(x, y, radio * 2, radio * 2);

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

        // ✅ MEJORADO: Usar esOnline para determinar si está autenticado
        // esOnline = true significa AUTENTICADO (verde)
        // esOnline = false significa solo CONECTADO (amarillo)
        Color color = usuario.esOnline ? COLOR_USUARIO_AUTENTICADO : COLOR_USUARIO_CONECTADO;

        Ellipse2D circulo = new Ellipse2D.Double(
            usuario.x - radio, usuario.y - radio,
            radio * 2, radio * 2
        );
        g2d.setColor(color);
        g2d.fill(circulo);

        g2d.setColor(color.darker());
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.draw(circulo);

        // Indicador visual de estado (icono pequeño)
        if (usuario.esOnline) {
            // Círculo verde pequeño para autenticado
            g2d.setColor(COLOR_USUARIO_AUTENTICADO.brighter());
            g2d.fillOval(usuario.x + radio - 5, usuario.y - radio, 8, 8);
            g2d.setColor(COLOR_USUARIO_AUTENTICADO.darker());
            g2d.setStroke(new BasicStroke(1));
            g2d.drawOval(usuario.x + radio - 5, usuario.y - radio, 8, 8);
        }

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

        // Estado debajo del nombre
        g2d.setFont(new Font("Arial", Font.ITALIC, 7));
        String estado = usuario.esOnline ? "AUTENTICADO" : "CONECTADO";
        fm = g2d.getFontMetrics();
        int estadoX = usuario.x - fm.stringWidth(estado) / 2;
        int estadoY = textoY + 10;
        g2d.setColor(usuario.esOnline ? COLOR_USUARIO_AUTENTICADO : COLOR_USUARIO_CONECTADO);
        g2d.drawString(estado, estadoX, estadoY);
    }

    /**
     * ✅ IMPLEMENTACIÓN IObservador
     */
    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        LoggerCentral.debug(TAG, "Evento recibido: " + tipoDeDato);

        switch (tipoDeDato) {
            case "CLIENTE_CONECTADO":
            case "CLIENTE_DESCONECTADO":
            case "CLIENTE_AUTENTICADO":
            case "SESIONES_ACTUALIZADAS":
            case "USUARIO_AUTENTICADO":
            case "USUARIO_ONLINE":
            case "USUARIO_OFFLINE":
            case "USUARIO_DESCONECTADO":
                // Actualizar grafo cuando cambian las sesiones o estados de usuario
                LoggerCentral.info(TAG, "🔄 Actualizando grafo por evento: " + tipoDeDato);
                SwingUtilities.invokeLater(this::actualizarDesdeControlador);
                break;

            default:
                LoggerCentral.debug(TAG, "Evento no manejado: " + tipoDeDato);
        }
    }

    // Clase interna para representar un peer
    private static class NodoPeer {
        String id;
        String ip;
        List<NodoUsuario> usuarios;
        int x, y;

        NodoPeer(String id, String ip) {
            this.id = id;
            this.ip = ip;
            this.usuarios = new ArrayList<>();
            this.x = 0;
            this.y = 0;
        }
    }

    // Clase interna para representar un usuario
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
}
