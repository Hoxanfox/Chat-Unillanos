// java
package interfazGrafica;

import interfazGrafica.features.usuarios.UsuariosPanel;
import interfazGrafica.features.conexiones.ConexionesPanel;
import interfazGrafica.features.logs.LogsPanel;
import interfazGrafica.features.logs.LogWatcher;
import interfazGrafica.features.channels.ChannelsPanel;
import interfazGrafica.features.p2p.P2PPanel;

import controlador.p2p.P2PController;
import controlador.p2p.PeerDTO;
import controlador.p2p.P2PListener;
import interfazGrafica.features.p2p.PeerInfo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Ventana principal de la aplicación. Contiene pestañas por cada feature.
 */
public class MainWindow extends JFrame {

    private static final Logger LOGGER = Logger.getLogger(MainWindow.class.getName());

    private final JTabbedPane tabs = new JTabbedPane();

    // Panels como campos para poder acceder a ellos (ej. LogsPanel)
    private final UsuariosPanel usuariosPanel = new UsuariosPanel();
    private final ConexionesPanel conexionesPanel = new ConexionesPanel();
    private final ChannelsPanel channelsPanel = new ChannelsPanel();
    private final LogsPanel logsPanel = new LogsPanel();
    private final P2PPanel p2pPanel = new P2PPanel();

    private LogWatcher logWatcher;
    private final P2PController p2pController;

    public MainWindow() {
        super("Chat Server Administration");
        System.out.println("[DEBUG] MainWindow.<init> - inicio constructor");
        LOGGER.log(Level.INFO, "MainWindow constructor start");

        GUIConfig.configureFrame(this);
        initComponents();

        // Iniciar el watcher de logs
        System.out.println("[DEBUG] MainWindow.<init> - iniciando LogWatcher");
        logWatcher = new LogWatcher(logsPanel);
        logWatcher.start();
        System.out.println("[DEBUG] MainWindow.<init> - LogWatcher.start() llamado");

        // Instanciar el controlador (la UI depende del módulo Controlador)
        System.out.println("[DEBUG] MainWindow.<init> - creando P2PController");
        p2pController = new P2PController();
        LOGGER.log(Level.INFO, "P2PController instanciado: {0}", p2pController.getClass().getName());

        // Registrar listener para actualizar el panel P2P con datos convertidos
        System.out.println("[DEBUG] MainWindow.<init> - registrando P2PListener");
        p2pController.addListener(new P2PListener() {
            @Override
            public void onEvent(String tipoDeDato, Object datos) {
                System.out.println("[DEBUG] P2PListener.onEvent - tipo: " + tipoDeDato + ", datos: " + (datos != null ? datos.getClass().getSimpleName() : "null"));
                switch (tipoDeDato) {
                    case "P2P_PEER_LIST_RECIBIDA":
                        if (datos instanceof List) {
                            List<?> list = (List<?>) datos;
                            System.out.println("[DEBUG] P2P_PEER_LIST_RECIBIDA - tamaño lista: " + list.size());
                            List<PeerInfo> uiPeers = new ArrayList<>();
                            int idx = 0;
                            for (Object o : list) {
                                idx++;
                                if (o instanceof PeerDTO) {
                                    PeerDTO dto = (PeerDTO) o;
                                    System.out.println("[DEBUG] P2P_PEER_LIST_RECIBIDA - dto[" + idx + "]: uuid=" + dto.getUuid() + ", label=" + dto.getLabel() + ", ip=" + dto.getIp() + ", port=" + dto.getPort() + ", online=" + dto.isOnline());
                                    uiPeers.add(new PeerInfo(dto.getUuid(), dto.getLabel(), dto.getIp(), dto.getPort(), dto.isOnline()));
                                } else {
                                    System.out.println("[DEBUG] P2P_PEER_LIST_RECIBIDA - elemento no es PeerDTO: " + (o != null ? o.getClass().getName() : "null"));
                                }
                            }
                            SwingUtilities.invokeLater(() -> {
                                System.out.println("[DEBUG] P2P_PEER_LIST_RECIBIDA - actualizando UI p2pPanel con " + uiPeers.size() + " peers");
                                p2pPanel.setPeers(uiPeers);
                            });
                        } else {
                            System.out.println("[DEBUG] P2P_PEER_LIST_RECIBIDA - datos no eran List");
                        }
                        break;
                    case "P2P_JOIN_EXITOSA":
                        if (datos instanceof PeerDTO) {
                            PeerDTO dto = (PeerDTO) datos;
                            System.out.println("[DEBUG] P2P_JOIN_EXITOSA - dto: uuid=" + dto.getUuid() + ", ip=" + dto.getIp() + ", port=" + dto.getPort());
                            PeerInfo p = new PeerInfo(dto.getUuid(), dto.getLabel(), dto.getIp(), dto.getPort(), dto.isOnline());
                            SwingUtilities.invokeLater(() -> {
                                System.out.println("[DEBUG] P2P_JOIN_EXITOSA - agregando/actualizando peer en UI y seteando info local");
                                p2pPanel.addOrUpdatePeer(p);
                                p2pPanel.setLocalInfo(p.getUuid(), p.getIp(), p.getPort());
                            });
                        } else {
                            System.out.println("[DEBUG] P2P_JOIN_EXITOSA - datos no son PeerDTO");
                        }
                        break;
                    case "P2P_JOIN_ERROR":
                    case "P2P_PEER_LIST_ERROR":
                        // Manejar errores estructurados enviados como Map
                        if (datos instanceof Map) {
                            Map<?,?> map = (Map<?,?>) datos;
                            Object reqId = map.get("requestId");
                            Object target = map.get("targetSocketInfo");
                            Object message = map.get("message");
                            Object resp = map.get("response");
                            Object exObj = map.get("exception");
                            System.out.println("[DEBUG] " + tipoDeDato + " - requestId=" + reqId + ", target=" + target + ", message=" + message + ", response=" + (resp!=null?resp.getClass().getSimpleName():"null") + ", exception=" + (exObj!=null?exObj.getClass().getSimpleName():"null"));
                            LOGGER.log(Level.WARNING, "P2P error [{0}] target={1} msg={2}", new Object[]{reqId, target, message});
                            // Mostrar diálogo de error para join (más visible al usuario)
                            if ("P2P_JOIN_ERROR".equals(tipoDeDato)) {
                                // Construir el mensaje antes de la lambda y asignarlo a una variable final
                                StringBuilder sb = new StringBuilder();
                                sb.append("RequestId: ").append(reqId).append('\n');
                                sb.append("Target: ").append(target).append('\n');
                                sb.append("Error: ").append(message);
                                if (resp != null) sb.append('\n').append("Response: ").append(resp.toString());
                                final String detailsFinal = sb.toString();
                                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(MainWindow.this, "Error unirse a la red:\n" + detailsFinal, "Error P2P_JOIN", JOptionPane.ERROR_MESSAGE));
                            }
                        } else if (datos != null) {
                            // si es un String u otro tipo, mostrarlo tal cual
                            System.out.println("[DEBUG] " + tipoDeDato + " - detalle: " + datos.toString());
                            LOGGER.log(Level.WARNING, "P2P error raw: {0}", datos.toString());
                            if ("P2P_JOIN_ERROR".equals(tipoDeDato)) {
                                final String msg = datos.toString();
                                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(MainWindow.this, "Error unirse a la red:\n" + msg, "Error P2P_JOIN", JOptionPane.ERROR_MESSAGE));
                            }
                        } else {
                            System.out.println("[DEBUG] " + tipoDeDato + " - sin datos adicionales");
                        }
                        break;
                    default:
                        System.out.println("[DEBUG] P2PListener.onEvent - evento desconocido: " + tipoDeDato);
                        // Ignorar o mostrar mensajes si es necesario
                }
            }
        });

        // Intentar iniciar la red P2P (no bloquear el EDT)
        System.out.println("[DEBUG] MainWindow.<init> - llamando iniciarRed()");
        p2pController.iniciarRed().thenRun(() -> {
            System.out.println("[DEBUG] MainWindow - iniciarRed completado correctamente");
            LOGGER.log(Level.INFO, "Red P2P iniciada correctamente");
        }).exceptionally(ex -> {
            String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
            System.out.println("[DEBUG] MainWindow - iniciarRed fallo: " + msg);
            LOGGER.log(Level.SEVERE, "No se pudo iniciar la red P2P: {0}", msg);
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this, "No se pudo iniciar la red P2P: " + msg, "Error P2P", JOptionPane.ERROR_MESSAGE)
            );
            return null;
        });

        // Asegurar detener watcher al cerrar la ventana
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.out.println("[DEBUG] MainWindow.windowClosing - evento recibido");
                if (logWatcher != null) {
                    System.out.println("[DEBUG] MainWindow.windowClosing - deteniendo LogWatcher");
                    logWatcher.stop();
                }
                try {
                    System.out.println("[DEBUG] MainWindow.windowClosing - cerrando p2pController");
                    p2pController.cerrar();
                    System.out.println("[DEBUG] MainWindow.windowClosing - p2pController cerrado");
                } catch (Exception ex) {
                    System.out.println("[DEBUG] MainWindow.windowClosing - excepción al cerrar p2pController: " + ex.getMessage());
                    LOGGER.log(Level.WARNING, "Excepción al cerrar p2pController", ex);
                }
            }
        });

        System.out.println("[DEBUG] MainWindow.<init> - fin constructor");
        LOGGER.log(Level.INFO, "MainWindow constructor finished");
    }

    private void initComponents() {
        System.out.println("[DEBUG] MainWindow.initComponents - configurando layout y pestañas");
        setLayout(new BorderLayout());

        // Pestañas por feature
        tabs.addTab("Usuarios", usuariosPanel);
        tabs.addTab("Conexiones", conexionesPanel);
        tabs.addTab("Channels", channelsPanel);
        tabs.addTab("P2P", p2pPanel);
        tabs.addTab("Logs", logsPanel);

        add(tabs, BorderLayout.CENTER);
        System.out.println("[DEBUG] MainWindow.initComponents - componentes inicializados");
    }
}
