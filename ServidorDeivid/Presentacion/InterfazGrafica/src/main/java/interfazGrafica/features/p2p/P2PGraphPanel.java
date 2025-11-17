package interfazGrafica.features.p2p;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import logger.LoggerCentral;

/**
 * Panel que dibuja peers como nodos en un grafo y enlaces entre ellos.
 * Layout básico en círculo; nodos verdes=online, rojos=offline.
 */
public class P2PGraphPanel extends JPanel {

    private final List<PeerInfo> peers = new ArrayList<>();
    private final List<Point> nodePositions = new ArrayList<>();
    private int nodeSize = 30;

    public P2PGraphPanel() {
        LoggerCentral.debug("P2PGraphPanel: constructor - inicializando panel");
        System.out.println("[DEBUG] P2PGraphPanel: constructor");
        setPreferredSize(new Dimension(700, 480));
        setBackground(Color.white);

        // Mostrar tooltip con info de peer al mover el ratón
        ToolTipManager.sharedInstance().registerComponent(this);
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                String tip = getTooltipAt(e.getPoint());
                setToolTipText(tip);
            }
        });
    }

    public synchronized void setPeers(List<PeerInfo> newPeers) {
        LoggerCentral.debug("P2PGraphPanel.setPeers: actualizando lista de peers, count=" + (newPeers==null?0:newPeers.size()));
        System.out.println("[DEBUG] P2PGraphPanel.setPeers: newPeers.count=" + (newPeers==null?0:newPeers.size()));
        peers.clear();
        nodePositions.clear();
        if (newPeers != null) peers.addAll(newPeers);
        layoutNodes();
        repaint();
    }

    public synchronized void addOrUpdatePeer(PeerInfo p) {
        LoggerCentral.debug("P2PGraphPanel.addOrUpdatePeer: peer=" + (p==null?"null":p));
        System.out.println("[DEBUG] P2PGraphPanel.addOrUpdatePeer: " + (p==null?"null":p));
        if (p == null) return;
        for (int i = 0; i < peers.size(); i++) {
            if (peers.get(i).getUuid().equals(p.getUuid())) { peers.set(i, p); layoutNodes(); repaint(); LoggerCentral.debug("P2PGraphPanel.addOrUpdatePeer: actualizado peer existente " + p.getUuid()); System.out.println("[DEBUG] actualizado peer " + p.getUuid()); return; }
        }
        peers.add(p);
        layoutNodes();
        repaint();
        LoggerCentral.debug("P2PGraphPanel.addOrUpdatePeer: añadido nuevo peer " + p.getUuid());
        System.out.println("[DEBUG] añadido nuevo peer " + p.getUuid());
    }

    public synchronized void removePeer(String uuid) {
        LoggerCentral.debug("P2PGraphPanel.removePeer: uuid=" + uuid);
        System.out.println("[DEBUG] P2PGraphPanel.removePeer: " + uuid);
        peers.removeIf(x -> x.getUuid().equals(uuid));
        layoutNodes();
        repaint();
    }

    private void layoutNodes() {
        try {
            LoggerCentral.debug("P2PGraphPanel.layoutNodes: recalculando layout para " + peers.size() + " peers");
            System.out.println("[DEBUG] P2PGraphPanel.layoutNodes: peers=" + peers.size());
            nodePositions.clear();
            int n = peers.size();
            if (n == 0) return;
            int w = getWidth() > 0 ? getWidth() : 700;
            int h = getHeight() > 0 ? getHeight() : 480;
            int cx = w / 2;
            int cy = h / 2;
            int radius = Math.max(80, Math.min(w, h) / 2 - 80);
            for (int i = 0; i < n; i++) {
                double ang = 2 * Math.PI * i / n - Math.PI / 2; // empezar arriba
                int x = cx + (int) (Math.cos(ang) * radius);
                int y = cy + (int) (Math.sin(ang) * radius);
                nodePositions.add(new Point(x, y));
            }
        } catch (Exception e) {
            LoggerCentral.error("P2PGraphPanel.layoutNodes: excepción -> " + e.getMessage(), e);
            System.out.println("[ERROR] P2PGraphPanel.layoutNodes: " + e.getMessage());
        }
    }

    private String getTooltipAt(Point p) {
        try {
            for (int i = 0; i < nodePositions.size(); i++) {
                Point np = nodePositions.get(i);
                int dx = p.x - np.x;
                int dy = p.y - np.y;
                if (dx * dx + dy * dy <= (nodeSize/2) * (nodeSize/2)) {
                    PeerInfo info = peers.get(i);
                    String tip = String.format("%s\n%s:%s\n%s", info.getLabel(), info.getIp(), info.getPort() <= 0 ? "-" : info.getPort(), info.isOnline() ? "ONLINE" : "OFFLINE");
                    LoggerCentral.debug("P2PGraphPanel.getTooltipAt: en nodo idx=" + i + " tip=" + tip.replace("\n"," | "));
                    System.out.println("[DEBUG] P2PGraphPanel.getTooltipAt: idx=" + i + " tip=" + tip.replace("\n"," | "));
                    return tip;
                }
            }
        } catch (Exception e) {
            LoggerCentral.error("P2PGraphPanel.getTooltipAt: excepción -> " + e.getMessage(), e);
            System.out.println("[ERROR] P2PGraphPanel.getTooltipAt: " + e.getMessage());
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        synchronized (this) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                LoggerCentral.debug("P2PGraphPanel.paintComponent: empezando pintura");
                System.out.println("[DEBUG] P2PGraphPanel.paintComponent: empezar");
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int n = peers.size();
                if (n == 0) {
                    g2.setColor(Color.GRAY);
                    g2.drawString("No peers", getWidth() / 2 - 20, getHeight() / 2);
                    LoggerCentral.debug("P2PGraphPanel.paintComponent: no peers, terminado");
                    System.out.println("[DEBUG] P2PGraphPanel.paintComponent: no peers");
                    return;
                }

                // Recalcular layout si el tamaño cambió
                if (nodePositions.size() != n) layoutNodes();

                // Dibujar enlaces (malla completa para demo)
                g2.setStroke(new BasicStroke(1f));
                g2.setColor(new Color(200, 200, 200));
                for (int i = 0; i < n; i++) {
                    for (int j = i + 1; j < n; j++) {
                        Point a = nodePositions.get(i);
                        Point b = nodePositions.get(j);
                        g2.drawLine(a.x, a.y, b.x, b.y);
                    }
                }

                // Dibujar nodos
                for (int i = 0; i < n; i++) {
                    PeerInfo p = peers.get(i);
                    Point pt = nodePositions.get(i);
                    Color fill = p.isOnline() ? new Color(67, 160, 71) : new Color(229, 57, 53);
                    g2.setColor(fill);
                    g2.fillOval(pt.x - nodeSize/2, pt.y - nodeSize/2, nodeSize, nodeSize);
                    g2.setColor(Color.DARK_GRAY);
                    g2.drawOval(pt.x - nodeSize/2, pt.y - nodeSize/2, nodeSize, nodeSize);

                    // label debajo
                    String lbl = p.getLabel() != null ? p.getLabel() : (p.getUuid() != null ? p.getUuid().substring(0, Math.min(6, p.getUuid().length())) : "?");
                    FontMetrics fm = g2.getFontMetrics();
                    int sw = fm.stringWidth(lbl);
                    g2.setColor(Color.BLACK);
                    g2.drawString(lbl, pt.x - sw/2, pt.y + nodeSize/2 + fm.getAscent());
                }
                LoggerCentral.debug("P2PGraphPanel.paintComponent: terminado pintura para " + n + " nodos");
                System.out.println("[DEBUG] P2PGraphPanel.paintComponent: terminado para " + n + " nodos");
            } finally {
                g2.dispose();
            }
        }
    }

    @Override
    public void invalidate() {
        LoggerCentral.debug("P2PGraphPanel.invalidate: invalidando y recalculando layout");
        System.out.println("[DEBUG] P2PGraphPanel.invalidate");
        super.invalidate();
        layoutNodes();
    }
}
