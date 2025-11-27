package gestorClientes.servicios;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import conexion.clientes.interfaces.IGestorConexionesCliente;
import conexion.clientes.interfaces.IRouterMensajesCliente;
import dominio.clienteServidor.Canal;
import dominio.clienteServidor.relaciones.CanalMiembro;
import dto.comunicacion.DTOResponse;
import gestorClientes.interfaces.IServicioCliente;
import logger.LoggerCentral;
import repositorio.clienteServidor.CanalMiembroRepositorio;
import repositorio.clienteServidor.CanalRepositorio;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio que responde a la petición "listarCanales" del cliente.
 * Devuelve los canales a los que pertenece un usuario específico.
 */
public class ServicioListarCanales implements IServicioCliente {

    private static final String TAG = "ServicioListarCanales";
    private final CanalRepositorio canalRepositorio;
    private final CanalMiembroRepositorio miembroRepositorio;
    private final Gson gson;

    public ServicioListarCanales() {
        this.canalRepositorio = new CanalRepositorio();
        this.miembroRepositorio = new CanalMiembroRepositorio();
        this.gson = new Gson();
    }

    @Override
    public String getNombre() {
        return "ServicioListarCanales";
    }

    @Override
    public void inicializar(IGestorConexionesCliente gestor, IRouterMensajesCliente router) {
        // Registrar la ruta "listarCanales"
        router.registrarAccion("listarCanales", (datos, idSesion) -> {
            LoggerCentral.info(TAG, "📥 Petición listarCanales recibida de sesión: " + idSesion);
            return handleListarCanales(datos.getAsJsonObject(), idSesion);
        });

        LoggerCentral.info(TAG, "✅ Servicio inicializado - Ruta 'listarCanales' registrada");
    }

    @Override
    public void iniciar() {
        LoggerCentral.info(TAG, "Servicio de listar canales iniciado");
    }

    @Override
    public void detener() {
        LoggerCentral.info(TAG, "Servicio de listar canales detenido");
    }

    /**
     * Maneja la petición "listarCanales".
     * Espera que el payload contenga "usuarioId" para filtrar los canales.
     */
    private DTOResponse handleListarCanales(JsonObject datos, String idSesion) {
        try {
            // Extraer userId del payload
            final String userId;

            if (datos != null && datos.has("usuarioId")) {
                userId = datos.get("usuarioId").getAsString();
            } else if (datos != null && datos.has("userId")) {
                userId = datos.get("userId").getAsString();
            } else {
                userId = null;
            }

            if (userId == null || userId.isEmpty()) {
                LoggerCentral.warn(TAG, "❌ usuarioId no proporcionado en la petición");
                return new DTOResponse(
                    "listarCanales",
                    "error",
                    "usuarioId no proporcionado en la petición",
                    null
                );
            }

            LoggerCentral.info(TAG, "📋 Listando canales para usuario: " + userId);

            // Obtener todos los canales y relaciones miembro-canal
            List<Canal> todosCanales = canalRepositorio.obtenerTodosParaSync();
            List<CanalMiembro> miembros = miembroRepositorio.obtenerTodosParaSync();

            LoggerCentral.info(TAG, "   Total canales en BD: " + todosCanales.size());
            LoggerCentral.info(TAG, "   Total relaciones miembro: " + miembros.size());

            // 🔍 DEBUG: Mostrar todas las relaciones
            LoggerCentral.info(TAG, "   🔍 DEBUG - Relaciones en BD:");
            for (CanalMiembro cm : miembros) {
                LoggerCentral.info(TAG, "      Canal: " + cm.getCanalId() + " -> Usuario: " + cm.getUsuarioId());
            }

            // Conjunto de IDs de canales donde el usuario pertenece (normalizado a minúsculas)
            Set<String> canalesDelUsuario = miembros.stream()
                    .filter(cm -> {
                        String miembroId = cm.getUsuarioId().toString().toLowerCase();
                        String targetId = userId.toLowerCase();
                        boolean match = miembroId.equals(targetId);
                        if (match) {
                            LoggerCentral.info(TAG, "   ✅ Match encontrado: Canal " + cm.getCanalId());
                        }
                        return match;
                    })
                    .map(cm -> cm.getCanalId().toString().toLowerCase())
                    .collect(Collectors.toSet());

            LoggerCentral.info(TAG, "   Usuario pertenece a " + canalesDelUsuario.size() + " canales");
            
            // 🔍 DEBUG: Mostrar IDs de canales del usuario
            if (!canalesDelUsuario.isEmpty()) {
                LoggerCentral.info(TAG, "   🔍 Canales del usuario:");
                for (String canalId : canalesDelUsuario) {
                    LoggerCentral.info(TAG, "      - " + canalId);
                }
            }

            // Filtrar canales por pertenencia y convertir al formato esperado
            List<Map<String, Object>> resultado = new ArrayList<>();
            for (Canal c : todosCanales) {
                String canalIdNormalizado = c.getId().toString().toLowerCase();
                LoggerCentral.info(TAG, "   🔍 Verificando canal: " + c.getNombre() + " (" + canalIdNormalizado + ")");
                
                if (canalesDelUsuario.contains(canalIdNormalizado)) {
                    Map<String, Object> mapa = new HashMap<>();
                    mapa.put("idCanal", c.getId().toString());
                    mapa.put("nombreCanal", c.getNombre());
                    mapa.put("ownerId", c.getCreador() != null ? c.getCreador().getId().toString() : null);
                    resultado.add(mapa);

                    LoggerCentral.info(TAG, "   ✅ Canal agregado: " + c.getNombre() + " (" + c.getId() + ")");
                } else {
                    LoggerCentral.info(TAG, "   ❌ Canal NO coincide: " + c.getNombre());
                }
            }

            LoggerCentral.info(TAG, "✅ Devolviendo " + resultado.size() + " canales al cliente");

            // Devolver respuesta exitosa
            return new DTOResponse(
                "listarCanales",
                "success",
                "Canales del usuario obtenidos",
                gson.toJsonTree(resultado)
            );

        } catch (Exception e) {
            LoggerCentral.error(TAG, "❌ Error al listar canales: " + e.getMessage());
            e.printStackTrace();

            return new DTOResponse(
                "listarCanales",
                "error",
                "Error al listar canales: " + e.getMessage(),
                null
            );
        }
    }
}
