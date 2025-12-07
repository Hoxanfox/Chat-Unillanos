package servicio.canales;

import dto.vista.DTOCanalVista;
import gestorCanales.GestorCanales;
import logger.LoggerCentral;

import java.util.List;

/**
 * Servicio de Gestión de Canales
 * Capa intermedia entre el controlador y el gestor
 */
public class ServicioGestionCanales {

    private static final String TAG = "ServicioGestionCanales";
    private final GestorCanales gestor;

    public ServicioGestionCanales(GestorCanales gestor) {
        this.gestor = gestor;
        LoggerCentral.info(TAG, "ServicioGestionCanales inicializado");
    }

    /**
     * Obtiene la lista completa de canales
     */
    public List<DTOCanalVista> listarCanales() {
        try {
            LoggerCentral.debug(TAG, "Obteniendo lista de canales");
            List<DTOCanalVista> canales = gestor.listarCanales();
            LoggerCentral.debug(TAG, "Canales obtenidos: " + canales.size());
            return canales;
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al listar canales: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Busca un canal por su ID
     */
    public DTOCanalVista buscarPorId(String id) {
        try {
            LoggerCentral.debug(TAG, "Buscando canal por ID: " + id);
            return gestor.buscarPorId(id);
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al buscar canal: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Obtiene los miembros de un canal
     */
    public List<String> obtenerMiembrosCanal(String canalId) {
        try {
            LoggerCentral.debug(TAG, "Obteniendo miembros del canal: " + canalId);
            return gestor.obtenerMiembrosCanal(canalId);
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error al obtener miembros: " + e.getMessage());
            throw e;
        }
    }
}

