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

/**
 * Ventana principal de la aplicación. Contiene pestañas por cada feature.
 */
public class MainWindow extends JFrame {

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
        GUIConfig.configureFrame(this);
        initComponents();

        // Iniciar el watcher de logs
        logWatcher = new LogWatcher(logsPanel);
        logWatcher.start();

        // Instanciar el controlador (la UI depende del módulo Controlador)
        p2pController = new P2PController();
        // Registrar listener para actualizar el panel P2P con datos convertidos
        p2pController.addListener(new P2PListener() {
            @Override
            public void onEvent(String tipoDeDato, Object datos) {
                switch (tipoDeDato) {
                    case "P2P_PEER_LIST_RECIBIDA":
                        if (datos instanceof List) {
                            List<?> list = (List<?>) datos;
                            List<PeerInfo> uiPeers = new ArrayList<>();
                            for (Object o : list) {
                                if (o instanceof PeerDTO) {
                                    PeerDTO dto = (PeerDTO) o;
                                    uiPeers.add(new PeerInfo(dto.getUuid(), dto.getLabel(), dto.getIp(), dto.getPort(), dto.isOnline()));
                                }
                            }
                            SwingUtilities.invokeLater(() -> p2pPanel.setPeers(uiPeers));
                        }
                        break;
                    case "P2P_JOIN_EXITOSA":
                        if (datos instanceof PeerDTO) {
                            PeerDTO dto = (PeerDTO) datos;
                            PeerInfo p = new PeerInfo(dto.getUuid(), dto.getLabel(), dto.getIp(), dto.getPort(), dto.isOnline());
                            SwingUtilities.invokeLater(() -> {
                                p2pPanel.addOrUpdatePeer(p);
                                p2pPanel.setLocalInfo(p.getUuid(), p.getIp(), p.getPort());
                            });
                        }
                        break;
                    default:
                        // Ignorar o mostrar mensajes si es necesario
                }
            }
        });

        // Intentar iniciar la red P2P (no bloquear el EDT)
        p2pController.iniciarRed().exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "No se pudo iniciar la red P2P: " + ex.getMessage(), "Error P2P", JOptionPane.ERROR_MESSAGE));
            return null;
        });

        // Asegurar detener watcher al cerrar la ventana
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (logWatcher != null) logWatcher.stop();
                try { p2pController.cerrar(); } catch (Exception ignored) {}
            }
        });
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        // Pestañas por feature
        tabs.addTab("Usuarios", usuariosPanel);
        tabs.addTab("Conexiones", conexionesPanel);
        tabs.addTab("Channels", channelsPanel);
        tabs.addTab("P2P", p2pPanel);
        tabs.addTab("Logs", logsPanel);

        add(tabs, BorderLayout.CENTER);
    }
}
