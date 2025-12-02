package gestorCanales;

import dominio.clienteServidor.Canal;
import dominio.clienteServidor.Usuario;
import dominio.clienteServidor.relaciones.CanalMiembro;
import dto.vista.DTOCanalVista;
import logger.LoggerCentral;
import repositorio.clienteServidor.CanalMiembroRepositorio;
import repositorio.clienteServidor.CanalRepositorio;
import repositorio.clienteServidor.UsuarioRepositorio;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Gestor de Canales - Capa de Negocio
 * Maneja la lógica de negocio relacionada con canales
 */
public class GestorCanales {

    private static final String TAG = "GestorCanales";
    private static final DateTimeFormatter formatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final CanalRepositorio canalRepositorio;
    private final CanalMiembroRepositorio miembroRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;

    public GestorCanales() {
        this.canalRepositorio = new CanalRepositorio();
        this.miembroRepositorio = new CanalMiembroRepositorio();
        this.usuarioRepositorio = new UsuarioRepositorio();
        LoggerCentral.info(TAG, "GestorCanales inicializado");
    }

    /**
     * Obtiene todos los canales del sistema
     */
    public List<DTOCanalVista> listarCanales() {
        LoggerCentral.debug(TAG, "Listando todos los canales");
        List<Canal> canales = canalRepositorio.obtenerTodosParaSync();
        List<DTOCanalVista> dtos = new ArrayList<>();

        for (Canal canal : canales) {
            dtos.add(convertirADTOVista(canal));
        }

        LoggerCentral.debug(TAG, "Total canales encontrados: " + dtos.size());
        return dtos;
    }

    /**
     * Busca un canal por su ID
     */
    public DTOCanalVista buscarPorId(String id) {
        LoggerCentral.debug(TAG, "Buscando canal por ID: " + id);
        try {
            Canal canal = canalRepositorio.obtenerPorId(UUID.fromString(id));
            return canal != null ? convertirADTOVista(canal) : null;
        } catch (IllegalArgumentException e) {
            LoggerCentral.error(TAG, "ID de canal inválido: " + id);
            return null;
        }
    }

    /**
     * Obtiene los miembros de un canal
     */
    public List<String> obtenerMiembrosCanal(String canalId) {
        LoggerCentral.debug(TAG, "Obteniendo miembros del canal: " + canalId);
        try {
            List<String> miembroIds = miembroRepositorio.obtenerMiembrosDelCanal(canalId);
            List<String> nombres = new ArrayList<>();
            
            for (String miembroId : miembroIds) {
                Usuario usuario = usuarioRepositorio.buscarPorId(miembroId);
                if (usuario != null) {
                    nombres.add(usuario.getNombre());
                }
            }
            
            return nombres;
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error obteniendo miembros: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Cuenta el número de miembros de un canal
     */
    public int contarMiembros(String canalId) {
        try {
            List<String> miembros = miembroRepositorio.obtenerMiembrosDelCanal(canalId);
            return miembros.size();
        } catch (Exception e) {
            LoggerCentral.error(TAG, "Error contando miembros: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Obtiene el nombre del creador de un canal
     */
    private String obtenerNombreCreador(String creadorId) {
        try {
            Usuario creador = usuarioRepositorio.buscarPorId(creadorId);
            return creador != null ? creador.getNombre() : "Desconocido";
        } catch (Exception e) {
            return "Desconocido";
        }
    }

    /**
     * Convierte una entidad Canal a DTOCanalVista
     */
    private DTOCanalVista convertirADTOVista(Canal canal) {
        String fechaFormateada = canal.getFechaCreacion() != null 
            ? formatter.format(canal.getFechaCreacion()) 
            : "N/A";
        
        String creadorId = canal.getCreador() != null 
            ? canal.getCreador().getId().toString() 
            : "N/A";
        
        String creadorNombre = canal.getCreador() != null 
            ? obtenerNombreCreador(canal.getCreador().getId()) 
            : "Desconocido";
        
        String peerPadreId = canal.getPeerPadre() != null 
            ? canal.getPeerPadre().toString() 
            : null;

        int numeroMiembros = contarMiembros(canal.getId());

        return new DTOCanalVista(
            canal.getId().toString(),
            canal.getNombre(),
            canal.getTipo() != null ? canal.getTipo().name() : "PUBLICO",
            creadorId,
            creadorNombre,
            numeroMiembros,
            fechaFormateada,
            peerPadreId
        );
    }
}

