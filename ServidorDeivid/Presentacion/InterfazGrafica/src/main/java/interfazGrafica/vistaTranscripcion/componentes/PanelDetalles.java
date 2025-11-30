package interfazGrafica.vistaTranscripcion.componentes;

import dto.transcripcion.DTOAudioTranscripcion;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Panel de detalles y transcripción de audio
 */
public class PanelDetalles extends JPanel {

    private static final Color COLOR_PRIMARY = new Color(52, 152, 219);
    private static final Color COLOR_SUCCESS = new Color(46, 204, 113);

    private JTextArea areaTranscripcion;
    private JButton btnGuardar;
    private JButton btnReproducir;
    private DTOAudioTranscripcion audioActual;

    public PanelDetalles() {
        inicializarUI();
    }

    private void inicializarUI() {
        setLayout(new BorderLayout(10, 10));
        setPreferredSize(new Dimension(350, 0));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(189, 195, 199), 1),
                new EmptyBorder(10, 10, 10, 10)
        ));

        // Título
        JLabel lblTitulo = new JLabel("Transcripción");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        add(lblTitulo, BorderLayout.NORTH);

        // Área de transcripción
        areaTranscripcion = new JTextArea();
        areaTranscripcion.setLineWrap(true);
        areaTranscripcion.setWrapStyleWord(true);
        areaTranscripcion.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        areaTranscripcion.setBorder(new EmptyBorder(5, 5, 5, 5));
        JScrollPane scrollTranscripcion = new JScrollPane(areaTranscripcion);
        add(scrollTranscripcion, BorderLayout.CENTER);

        // Panel de botones
        JPanel panelBotones = new JPanel(new GridLayout(2, 1, 5, 5));
        panelBotones.setBackground(Color.WHITE);

        btnGuardar = new JButton("💾 Guardar Transcripción");
        btnGuardar.setEnabled(false);
        estilizarBoton(btnGuardar, COLOR_SUCCESS);
        panelBotones.add(btnGuardar);

        btnReproducir = new JButton("▶ Reproducir Audio");
        btnReproducir.setEnabled(false);
        estilizarBoton(btnReproducir, COLOR_PRIMARY);
        panelBotones.add(btnReproducir);

        add(panelBotones, BorderLayout.SOUTH);
    }

    private void estilizarBoton(JButton boton, Color color) {
        boton.setBackground(color);
        boton.setForeground(Color.WHITE);
        boton.setFocusPainted(false);
        boton.setBorderPainted(false);
        boton.setFont(new Font("Segoe UI", Font.BOLD, 11));
        boton.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    /**
     * Muestra los detalles de un audio
     */
    public void mostrarAudio(DTOAudioTranscripcion audio) {
        this.audioActual = audio;
        if (audio != null) {
            String transcripcion = audio.getTranscripcion();
            areaTranscripcion.setText(transcripcion != null ? transcripcion : "");
            areaTranscripcion.setEditable(!audio.isTranscrito());
            btnGuardar.setEnabled(true);
            btnReproducir.setEnabled(true);
        } else {
            limpiar();
        }
    }

    /**
     * Limpia el panel
     */
    public void limpiar() {
        areaTranscripcion.setText("");
        areaTranscripcion.setEditable(false);
        btnGuardar.setEnabled(false);
        btnReproducir.setEnabled(false);
        audioActual = null;
    }

    /**
     * Obtiene el texto de la transcripción
     */
    public String getTextoTranscripcion() {
        return areaTranscripcion.getText().trim();
    }

    /**
     * Obtiene el audio actual
     */
    public DTOAudioTranscripcion getAudioActual() {
        return audioActual;
    }

    /**
     * Establece el listener para el botón guardar
     */
    public void setListenerGuardar(ActionListener listener) {
        btnGuardar.addActionListener(listener);
    }

    /**
     * Establece el listener para el botón reproducir
     */
    public void setListenerReproducir(ActionListener listener) {
        btnReproducir.addActionListener(listener);
    }
}

