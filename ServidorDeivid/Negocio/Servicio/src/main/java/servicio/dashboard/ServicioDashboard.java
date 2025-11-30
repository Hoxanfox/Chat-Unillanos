package servicio.dashboard;

import dto.dashboard.DTORecursosSistema;
import gestorDashboard.GestorDashboard;
import logger.LoggerCentral;
import observador.IObservador;

/**
 * Servicio de Dashboard del sistema
 * Intermediario entre el controlador y el gestor
 * Proporciona métodos para obtener y actualizar estadísticas del sistema
 */
public class ServicioDashboard implements IServicioDashboardControl {

    private static final String TAG = "ServicioDashboard";
    private final GestorDashboard gestor;

    public ServicioDashboard() {
        this.gestor = new GestorDashboard();
        LoggerCentral.info(TAG, "ServicioDashboard inicializado");
    }

    @Override
    public void iniciar() {
        LoggerCentral.info(TAG, "Iniciando servicio de dashboard...");
        gestor.iniciar();
        LoggerCentral.info(TAG, "✅ Servicio de dashboard iniciado");
    }

    @Override
    public void detener() {
        LoggerCentral.info(TAG, "Deteniendo servicio de dashboard...");
        gestor.detener();
        LoggerCentral.info(TAG, "Servicio de dashboard detenido");
    }

    @Override
    public void actualizarUsuariosActivos(int cantidad) {
        gestor.actualizarUsuariosActivos(cantidad);
    }

    @Override
    public void actualizarConexionesP2P(int cantidad) {
        gestor.actualizarConexionesP2P(cantidad);
    }

    @Override
    public void actualizarConexionesClientes(int cantidad) {
        gestor.actualizarConexionesClientes(cantidad);
    }

    @Override
    public void registrarActividad(String actividad) {
        gestor.registrarActividad(actividad);
    }

    @Override
    public int obtenerUsuariosActivos() {
        return gestor.getUsuariosActivos();
    }

    @Override
    public int obtenerConexionesP2P() {
        return gestor.getConexionesP2P();
    }

    @Override
    public int obtenerConexionesClientes() {
        return gestor.getConexionesClientes();
    }

    @Override
    public DTORecursosSistema obtenerRecursos() {
        return gestor.getRecursos();
    }

    @Override
    public void registrarObservador(IObservador observador) {
        gestor.registrarObservador(observador);
        LoggerCentral.debug(TAG, "Observador registrado en el gestor");
    }

    @Override
    public void removerObservador(IObservador observador) {
        gestor.removerObservador(observador);
        LoggerCentral.debug(TAG, "Observador eliminado del gestor");
    }

    @Override
    public boolean estaActivo() {
        return gestor.estaActivo();
    }
}
