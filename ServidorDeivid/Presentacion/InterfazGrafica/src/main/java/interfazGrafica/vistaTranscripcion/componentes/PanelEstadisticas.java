package interfazGrafica.vistaTranscripcion.componentes;

import dto.transcripcion.DTOAudioTranscripcion;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * Panel de estadísticas de transcripciones
 * Muestra información sobre audios y estado del modelo Vosk
 */
public class PanelEstadisticas extends JPanel {

    private JLabel lblEstadisticas;
    private JLabel lblEstadoModelo;
    private JLabel lblEnCola;
    
    // Colores para estados
    private static final Color COLOR_DISPONIBLE = new Color(39, 174, 96);
    private static final Color COLOR_NO_DISPONIBLE = new Color(231, 76, 60);
    private static final Color COLOR_EN_PROCESO = new Color(52, 152, 219);

    public PanelEstadisticas() {
        inicializarUI();
    }

    private void inicializarUI() {
        setLayout(new BorderLayout(20, 0));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(189, 195, 199), 1),
                new EmptyBorder(8, 15, 8, 15)
        ));

        // Panel izquierdo: Estadísticas de audios
        JPanel panelIzquierdo = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panelIzquierdo.setBackground(Color.WHITE);
        
        lblEstadisticas = new JLabel("Total: 0 audios | Transcritos: 0 | Pendientes: 0");
        lblEstadisticas.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panelIzquierdo.add(lblEstadisticas);
        
        add(panelIzquierdo, BorderLayout.WEST);

        // Panel derecho: Estado del modelo y cola
        JPanel panelDerecho = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        panelDerecho.setBackground(Color.WHITE);
        
        // Indicador de audios en cola
        lblEnCola = new JLabel("📋 En cola: 0");
        lblEnCola.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblEnCola.setForeground(COLOR_EN_PROCESO);
        panelDerecho.add(lblEnCola);
        
        // Separador visual
        JSeparator sep = new JSeparator(JSeparator.VERTICAL);
        sep.setPreferredSize(new Dimension(1, 20));
        panelDerecho.add(sep);
        
        // Estado del modelo Vosk
        lblEstadoModelo = new JLabel("🔴 Vosk: No disponible");
        lblEstadoModelo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblEstadoModelo.setForeground(COLOR_NO_DISPONIBLE);
        panelDerecho.add(lblEstadoModelo);
        
        add(panelDerecho, BorderLayout.EAST);
    }

    /**
     * Actualiza las estadísticas con la lista de audios
     */
    public void actualizarEstadisticas(List<DTOAudioTranscripcion> audios) {
        int total = audios.size();
        long transcritos = audios.stream().filter(DTOAudioTranscripcion::isTranscrito).count();
        long pendientes = total - transcritos;

        lblEstadisticas.setText(String.format(
                "📊 Total: %d audios  |  ✅ Transcritos: %d  |  ⏳ Pendientes: %d",
                total, transcritos, pendientes
        ));
    }
    
    /**
     * Actualiza el estado del modelo Vosk
     */
    public void actualizarEstadoModelo(boolean disponible) {
        if (disponible) {
            lblEstadoModelo.setText("🟢 Vosk: Disponible");
            lblEstadoModelo.setForeground(COLOR_DISPONIBLE);
        } else {
            lblEstadoModelo.setText("🔴 Vosk: No disponible");
            lblEstadoModelo.setForeground(COLOR_NO_DISPONIBLE);
        }
    }
    
    /**
     * Actualiza el número de audios en cola de transcripción
     */
    public void actualizarEnCola(int cantidad) {
        if (cantidad > 0) {
            lblEnCola.setText("📋 En cola: " + cantidad);
            lblEnCola.setForeground(COLOR_EN_PROCESO);
        } else {
            lblEnCola.setText("📋 En cola: 0");
            lblEnCola.setForeground(Color.GRAY);
        }
    }
}
