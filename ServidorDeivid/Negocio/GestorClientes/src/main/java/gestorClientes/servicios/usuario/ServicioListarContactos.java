package gestorClientes.servicios.usuario;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import conexion.clientes.interfaces.IGestorConexionesCliente;
import conexion.clientes.interfaces.IRouterMensajesCliente;
import dominio.clienteServidor.Usuario;
import dto.comunicacion.DTOResponse;
import dto.contactos.DTOContacto;
import gestorClientes.interfaces.IServicioCliente;
import logger.LoggerCentral;
import repositorio.clienteServidor.UsuarioRepositorio;

import java.util.ArrayList;
import java.util.List;

/**
 * Servicio que responde a la petición "listarContactos" del cliente.
 * Devuelve todos los usuarios registrados en el servidor como contactos.
 */
public class ServicioListarContactos implements IServicioCliente {

    private static final String TAG = "ServicioListarContactos";
    private final UsuarioRepositorio usuarioRepositorio;
    private final Gson gson;

    public ServicioListarContactos() {
        this.usuarioRepositorio = new UsuarioRepositorio();
        this.gson = new Gson();
    }

    @Override
    public String getNombre() {
        return "ServicioListarContactos";
    }

    @Override
    public void inicializar(IGestorConexionesCliente gestor, IRouterMensajesCliente router) {
        // Registrar la ruta "listarContactos"
        router.registrarAccion("listarContactos", (datos, idSesion) -> {
            LoggerCentral.info(TAG, "📥 Petición listarContactos recibida de sesión: " + idSesion);
            // ✅ CORREGIDO: Manejar payload null
            JsonObject payload = (datos != null && datos.isJsonObject()) ? datos.getAsJsonObject() : new JsonObject();
            return handleListarContactos(payload, idSesion);
        });

        LoggerCentral.info(TAG, "✅ Servicio inicializado - Ruta 'listarContactos' registrada");
    }

    @Override
    public void iniciar() {
        LoggerCentral.info(TAG, "Servicio de listar contactos iniciado");
    }

    @Override
    public void detener() {
        LoggerCentral.info(TAG, "Servicio de listar contactos detenido");
    }

    /**
     * Maneja la petición "listarContactos".
     * Devuelve todos los usuarios del sistema como lista de contactos.
     */
    private DTOResponse handleListarContactos(JsonObject datos, String idSesion) {
        try {
            // Obtener todos los usuarios del repositorio
            List<Usuario> usuarios = usuarioRepositorio.obtenerTodosParaSync();
            LoggerCentral.info(TAG, "📋 Obtenidos " + usuarios.size() + " usuarios de la BD");

            // Convertir a DTOContacto
            List<DTOContacto> contactos = new ArrayList<>(usuarios.size());
            for (Usuario u : usuarios) {
                DTOContacto dto = new DTOContacto();
                dto.setId(u.getId().toString());
                dto.setNombre(u.getNombre());
                dto.setEmail(u.getEmail());

                // Estado como string ("ONLINE", "OFFLINE")
                dto.setEstado(u.getEstado() != null ? u.getEstado().name() : "OFFLINE");

                // PhotoId desde el campo foto
                dto.setPhotoId(u.getFoto());

                // PeerId: usar peerPadre si existe, sino usar ip
                String peerId = "";
                if (u.getPeerPadre() != null) {
                    peerId = u.getPeerPadre().toString();
                } else if (u.getIp() != null && !u.getIp().isEmpty()) {
                    peerId = u.getIp();
                }
                dto.setPeerId(peerId);

                // Fecha de registro
                dto.setFechaRegistro(u.getFechaCreacion() != null ? u.getFechaCreacion().toString() : "");

                contactos.add(dto);
            }

            LoggerCentral.info(TAG, "✅ Convertidos " + contactos.size() + " contactos a DTO");

            // Devolver respuesta exitosa con la lista de contactos
            return new DTOResponse(
                "listarContactos",
                "success",
                "Lista de contactos obtenida",
                gson.toJsonTree(contactos)
            );

        } catch (Exception e) {
            LoggerCentral.error(TAG, "❌ Error al listar contactos: " + e.getMessage());
            e.printStackTrace();

            return new DTOResponse(
                "listarContactos",
                "error",
                "Error al obtener contactos: " + e.getMessage(),
                null
            );
        }
    }
}
