package interfazGrafica.vistaPrincipal.componentes;

import javax.swing.*;
import java.awt.*;

/**
 * Panel que contiene el overview del sistema
 * Muestra 3 tarjetas con estadísticas: USERS, CHANNELS, CONNECTIONS
 */
public class PanelOverview extends JPanel {

    private TarjetaEstadistica tarjetaUsuarios;
    private TarjetaEstadistica tarjetaCanales;
    private TarjetaEstadistica tarjetaConexiones;

    public PanelOverview() {
        configurarPanel();
        inicializarComponentes();
    }

    private void configurarPanel() {
        this.setLayout(new BorderLayout());
    }

    private void inicializarComponentes() {
        // Título del overview
        JLabel lblTitulo = new JLabel("SYSTEM OVERVIEW");
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 14));
        lblTitulo.setForeground(new Color(0, 102, 204));
        lblTitulo.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        // Panel para las 3 tarjetas
        JPanel panelTarjetas = new JPanel(new GridLayout(1, 3, 15, 0));

        // Crear las tarjetas
        tarjetaUsuarios = new TarjetaEstadistica("USERS", new Color(52, 152, 219));
        tarjetaCanales = new TarjetaEstadistica("CHANNELS", new Color(46, 204, 113));
        tarjetaConexiones = new TarjetaEstadistica("CONNECTIONS", new Color(155, 89, 182));

        panelTarjetas.add(tarjetaUsuarios);
        panelTarjetas.add(tarjetaCanales);
        panelTarjetas.add(tarjetaConexiones);

        // Agregar componentes
        this.add(lblTitulo, BorderLayout.NORTH);
        this.add(panelTarjetas, BorderLayout.CENTER);
    }

    /**
     * Actualiza las estadísticas mostradas en las tarjetas
     */
    public void actualizarEstadisticas(int totalUsuarios, int usuariosActivos,
                                      int totalCanales, int canalesActivos,
                                      int totalConexiones, int conexionesActivas) {
        tarjetaUsuarios.actualizarDatos(totalUsuarios, usuariosActivos);
        tarjetaCanales.actualizarDatos(totalCanales, canalesActivos);
        tarjetaConexiones.actualizarDatos(totalConexiones, conexionesActivas);
    }
}

