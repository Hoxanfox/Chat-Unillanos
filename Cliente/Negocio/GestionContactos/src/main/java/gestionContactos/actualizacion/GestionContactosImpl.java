package gestionContactos.actualizacion;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import comunicacion.EnviadorPeticiones;
import comunicacion.GestorRespuesta;
import comunicacion.IEnviadorPeticiones;
import comunicacion.IGestorRespuesta;
import dto.comunicacion.DTORequest;
import dto.comunicacion.DTOResponse;
import dto.featureContactos.DTOContacto;
import observador.IObservador;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementación del gestor de contactos.
 * - listarContactos: Respuesta a petición REQUEST del cliente
 * - solicitarListaContactos: Notificación PUSH del servidor
 */
public class GestionContactosImpl implements IGestionContactos {

    private final List<IObservador> observadores = new ArrayList<>();
    private List<DTOContacto> contactosCache = new ArrayList<>();
    private final IEnviadorPeticiones enviadorPeticiones;
    private final IGestorRespuesta gestorRespuesta;
    private final Gson gson;
    private String usuarioIdActual;

    public GestionContactosImpl() {
        this.enviadorPeticiones = new EnviadorPeticiones();
        this.gestorRespuesta = GestorRespuesta.getInstancia();
        this.gson = new Gson();

        // REQUEST: Respuesta a petición del cliente
        this.gestorRespuesta.registrarManejador("listarContactos", this::manejarRespuestaListarContactos);
        
        // PUSH: Notificación del servidor (actualización automática)
        this.gestorRespuesta.registrarManejador("solicitarListaContactos", this::manejarPushActualizacionContactos);
        
        // PUSH: Actualización de lista de contactos (legacy)
        this.gestorRespuesta.registrarManejador("actualizarListaContactos", this::manejarPushActualizacionContactos);

        System.out.println("✅ [GestionContactos]: Manejadores registrados");
        System.out.println("   📤 REQUEST: listarContactos");
        System.out.println("   📥 PUSH: solicitarListaContactos, actualizarListaContactos");
    }

    /**
     * Maneja la respuesta a la petición REQUEST "listarContactos"
     */
    private void manejarRespuestaListarContactos(DTOResponse respuesta) {
        System.out.println("📤 [GestionContactos][REQUEST]: Respuesta a listarContactos recibida");
        System.out.println("   Status: " + respuesta.getStatus() + ", Message: " + respuesta.getMessage());

        if (respuesta.fueExitoso()) {
            procesarListaContactos(respuesta, "REQUEST");
        } else {
            System.err.println("❌ [GestionContactos][REQUEST]: Error del servidor: " + respuesta.getMessage());
            notificarObservadores("ERROR_CONTACTOS", respuesta.getMessage());
        }
    }

    /**
     * Maneja la notificación PUSH del servidor "solicitarListaContactos"
     */
    private void manejarPushActualizacionContactos(DTOResponse respuesta) {
        System.out.println("📥 [GestionContactos][PUSH]: Notificación de actualización recibida");
        System.out.println("   Action: " + respuesta.getAction() + ", Status: " + respuesta.getStatus());

        if (respuesta.fueExitoso()) {
            procesarListaContactos(respuesta, "PUSH");
        } else {
            System.err.println("❌ [GestionContactos][PUSH]: Error en notificación: " + respuesta.getMessage());
        }
    }

    /**
     * Procesa la lista de contactos recibida (común para REQUEST y PUSH)
     */
    private void procesarListaContactos(DTOResponse respuesta, String tipo) {
        try {
            Type tipoLista = new TypeToken<ArrayList<DTOContacto>>() {}.getType();
            this.contactosCache = gson.fromJson(gson.toJson(respuesta.getData()), tipoLista);

            System.out.println("✅ [GestionContactos][" + tipo + "]: " + contactosCache.size() + " contactos procesados");

            // Log detallado de los contactos
            if (contactosCache.size() > 0) {
                System.out.println("📋 [GestionContactos][" + tipo + "]: Contactos actualizados:");
                for (DTOContacto contacto : contactosCache) {
                    System.out.println("   - " + contacto.getNombre() + 
                        " (" + contacto.getEmail() + ") " +
                        "[" + contacto.getEstado() + "]" +
                        " ID: " + contacto.getId());
                }
            } else {
                System.out.println("ℹ️ [GestionContactos][" + tipo + "]: Lista de contactos vacía");
            }

            notificarObservadores("ACTUALIZAR_CONTACTOS", this.contactosCache);
        } catch (Exception e) {
            System.err.println("❌ [GestionContactos][" + tipo + "]: Error al parsear contactos: " + e.getMessage());
            e.printStackTrace();
            notificarObservadores("ERROR_CONTACTOS", "Error al procesar lista de contactos");
        }
    }

    @Override
    public void solicitarActualizacionContactos() {
        System.out.println("📤 [GestionContactos]: Solicitando lista de contactos al servidor...");

        Map<String, Object> data = new HashMap<>();
        if (usuarioIdActual != null && !usuarioIdActual.isEmpty()) {
            data.put("usuarioId", usuarioIdActual);
            System.out.println("   UsuarioId: " + usuarioIdActual);
        }

        DTORequest peticion = new DTORequest("listarContactos", data.isEmpty() ? null : data);
        enviadorPeticiones.enviar(peticion);
    }

    /**
     * Establece el ID del usuario actual para las peticiones
     */
    public void setUsuarioId(String usuarioId) {
        this.usuarioIdActual = usuarioId;
        System.out.println("✅ [GestionContactos]: Usuario ID establecido: " + usuarioId);
    }

    @Override
    public List<DTOContacto> getContactos() {
        return new ArrayList<>(contactosCache);
    }

    // --- Métodos del Patrón Observador ---
    @Override
    public void registrarObservador(IObservador observador) {
        if (!observadores.contains(observador)) {
            observadores.add(observador);
            System.out.println("👁️ [GestionContactos]: Observador registrado");
        }
    }

    @Override
    public void removerObservador(IObservador observador) {
        observadores.remove(observador);
        System.out.println("👁️ [GestionContactos]: Observador removido");
    }

    @Override
    public void notificarObservadores(String tipoDeDato, Object datos) {
        System.out.println("📢 [GestionContactos]: Notificando a " + observadores.size() + " observadores - Tipo: " + tipoDeDato);
        for (IObservador observador : observadores) {
            observador.actualizar(tipoDeDato, datos);
        }
    }
}
