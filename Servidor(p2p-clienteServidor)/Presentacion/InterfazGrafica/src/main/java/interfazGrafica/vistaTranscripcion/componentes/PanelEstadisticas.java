package interfazGrafica.vistaTranscripcion.componentes;

import dto.transcripcion.DTOAudioTranscripcion;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * Panel de estadísticas de transcripciones
 */
public class PanelEstadisticas extends JPanel {

    private JLabel lblEstadisticas;

    public PanelEstadisticas() {
        inicializarUI();
    }

    private void inicializarUI() {
        setLayout(new FlowLayout(FlowLayout.LEFT));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(189, 195, 199), 1),
                new EmptyBorder(5, 10, 5, 10)
        ));

        lblEstadisticas = new JLabel("Total: 0 audios | Transcritos: 0 | Pendientes: 0");
        lblEstadisticas.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        add(lblEstadisticas);
    }

    /**
     * Actualiza las estadísticas con la lista de audios
     */
    public void actualizarEstadisticas(List<DTOAudioTranscripcion> audios) {
        int total = audios.size();
        long transcritos = audios.stream().filter(DTOAudioTranscripcion::isTranscrito).count();
        long pendientes = total - transcritos;

        lblEstadisticas.setText(String.format(
                "Total: %d audios | ✅ Transcritos: %d | ⏳ Pendientes: %d",
                total, transcritos, pendientes
        ));
    }
}
