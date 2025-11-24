package gestorClientes.servicios;

import com.google.gson.JsonObject;
import conexion.clientes.interfaces.IGestorConexionesCliente;
import conexion.clientes.interfaces.IRouterMensajesCliente;
import dominio.clienteServidor.Usuario;
import dto.comunicacion.DTOResponse;
import gestorClientes.interfaces.IServicioCliente;
import logger.LoggerCentral;
import repositorio.clienteServidor.UsuarioRepositorio;

public class ServicioAutenticacion implements IServicioCliente {

    private static final String TAG = "AuthService";
    private IGestorConexionesCliente gestor;
    private final UsuarioRepositorio repoUsuario;

    public ServicioAutenticacion() {
        this.repoUsuario = new UsuarioRepositorio();
    }

    @Override
    public String getNombre() { return "ServicioAutenticacion"; }

    @Override
    public void inicializar(IGestorConexionesCliente gestor, IRouterMensajesCliente router) {
        this.gestor = gestor;

        // RUTA: LOGIN
        router.registrarAccion("login", (datos, idSesion) -> {
            try {
                JsonObject creds = datos.getAsJsonObject();
                String email = creds.get("email").getAsString();
                String pass = creds.get("password").getAsString();

                // 1. Buscar usuario en BD (Necesitas implementar buscarPorEmail en tu repo)
                // Usuario u = repoUsuario.buscarPorEmail(email);

                // MOCKUP para el ejemplo:
                boolean loginExitoso = true;
                String idUsuario = "uuid-del-usuario-en-bd"; // u.getId().toString()

                if (loginExitoso) {
                    // 2. Vincular sesión anónima con usuario
                    gestor.registrarUsuarioEnSesion(idSesion, idUsuario);

                    LoggerCentral.info(TAG, "Usuario logueado: " + email);
                    return new DTOResponse("login", "success", "Bienvenido", null);
                } else {
                    return new DTOResponse("login", "error", "Credenciales inválidas", null);
                }
            } catch (Exception e) {
                return new DTOResponse("login", "error", "Datos malformados", null);
            }
        });
    }

    @Override public void iniciar() {}
    @Override public void detener() {}
}