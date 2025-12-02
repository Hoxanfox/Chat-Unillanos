package controlador.canales;

import dto.vista.DTOCanalVista;
import logger.LoggerCentral;
import servicio.canales.ServicioGestionCanales;

import java.util.List;

/**
 * Controlador de Canales - Capa de Presentación
 * Actúa como intermediario entre la vista y el servicio
 */
public class ControladorCanales {

    private static final String TAG = "ControladorCanales";
    private final ServicioGestionCanales servicio;

    public ControladorCanales(ServicioGestionCanales servicio) {
        this.servicio = servicio;
        LoggerCentral.info(TAG, "ControladorCanales inicializado");
    }

    /**
     * Obtiene la lista de todos los canales
     */
    public List<DTOCanalVista> listarCanales() {
        try {
            return servicio.listarCanales();
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al listar canales: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Busca un canal por su ID
     */
    public DTOCanalVista buscarPorId(String id) {
        try {
            return servicio.buscarPorId(id);
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al buscar canal: " + e.getMessage());
            return null;
        }
    }

    /**
     * Obtiene los miembros de un canal
     */
    public List<String> obtenerMiembrosCanal(String canalId) {
        try {
            return servicio.obtenerMiembrosCanal(canalId);
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al obtener miembros: " + e.getMessage());
            return List.of();
        }
    }
}

