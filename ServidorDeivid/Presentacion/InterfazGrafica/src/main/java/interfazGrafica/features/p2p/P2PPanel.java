package interfazGrafica.features.p2p;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Panel para la sección P2P: muestra info local (UUID, socket) y un grafo con peers.
 */
public class P2PPanel extends JPanel {

    private final JLabel lblUuid = new JLabel("UUID: -");
    private final JLabel lblSocket = new JLabel("Socket: -");
    private final JLabel lblIp = new JLabel("IP local: -");
    private final P2PGraphPanel graph = new P2PGraphPanel();

    public P2PPanel() {
        setLayout(new BorderLayout(8,8));

        // Top: local info + acciones
        JPanel top = new JPanel(new BorderLayout());
        JPanel info = new JPanel(new GridLayout(3,1));
        info.add(lblUuid);
        info.add(lblSocket);
        info.add(lblIp);
        top.add(info, BorderLayout.WEST);

        JPanel actions = new JPanel();
        JButton btnRefresh = new JButton("Refresh");
        btnRefresh.addActionListener((ActionEvent e) -> refreshLocalInfo());
        JButton btnDemo = new JButton("Demo Peers");
        btnDemo.addActionListener((ActionEvent e) -> populateDemoPeers());
        actions.add(btnRefresh);
        actions.add(btnDemo);
        top.add(actions, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);

        // Center: grafo
        add(new JScrollPane(graph), BorderLayout.CENTER);

        // Inicializar info local
        refreshLocalInfo();
    }

    public void setLocalInfo(String uuid, String ip, int port) {
        lblUuid.setText("UUID: " + (uuid != null ? uuid : "-"));
        lblIp.setText("IP local: " + (ip != null ? ip : "-"));
        lblSocket.setText("Socket: " + (ip != null ? ip + ":" + port : "-"));
    }

    public void setPeers(List<PeerInfo> peers) {
        graph.setPeers(peers);
    }

    public void addOrUpdatePeer(PeerInfo p) { graph.addOrUpdatePeer(p); }
    public void removePeer(String uuid) { graph.removePeer(uuid); }

    private void refreshLocalInfo() {
        // Intentar detectar IP local no loopback
        String ip = detectLocalIp();
        setLocalInfo("-", ip, -1);
    }

    private String detectLocalIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    String s = addr.getHostAddress();
                    if (s != null && !s.contains(":") && !s.equals("127.0.0.1")) return s;
                }
            }
        } catch (Exception ignored) {}
        return "127.0.0.1";
    }

    private void populateDemoPeers() {
        List<PeerInfo> demo = new ArrayList<>();
        demo.add(new PeerInfo("11111111-1111-1111-1111-111111111111", "Me", "192.168.0.10", 5000, true));
        demo.add(new PeerInfo("22222222-2222-2222-2222-222222222222", "PeerA", "192.168.0.11", 5000, true));
        demo.add(new PeerInfo("33333333-3333-3333-3333-333333333333", "PeerB", "192.168.0.12", 5000, false));
        demo.add(new PeerInfo("44444444-4444-4444-4444-444444444444", "PeerC", "192.168.0.13", 5000, true));
        setPeers(demo);
        // marcar info local usando el primero
        setLocalInfo(demo.get(0).getUuid(), demo.get(0).getIp(), demo.get(0).getPort());
    }
}

