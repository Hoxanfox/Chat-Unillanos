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
import java.util.List;

/**
 * Implementación del gestor de contactos, ahora enfocado solo en la actualización de la lista.
 */
public class GestionContactosImpl implements IGestionContactos {

    private final List<IObservador> observadores = new ArrayList<>();
    private List<DTOContacto> contactosCache = new ArrayList<>();
    private final IEnviadorPeticiones enviadorPeticiones;
    private final IGestorRespuesta gestorRespuesta;
    private final Gson gson;

    public GestionContactosImpl() {
        this.enviadorPeticiones = new EnviadorPeticiones();
        this.gestorRespuesta = GestorRespuesta.getInstancia();
        this.gson = new Gson();

        // Registrar manejadores para ambas acciones posibles
        this.gestorRespuesta.registrarManejador("actualizarListaContactos", this::manejarActualizacion);
        this.gestorRespuesta.registrarManejador("solicitarListaContactos", this::manejarActualizacion);

        System.out.println("✅ [GestionContactos]: Manejadores registrados para actualización de contactos");
    }

    private void manejarActualizacion(DTOResponse respuesta) {
        System.out.println("📥 [GestionContactos]: Respuesta recibida - Action: " + respuesta.getAction() + ", Status: " + respuesta.getStatus());

        if (respuesta.fueExitoso()) {
            try {
                Type tipoLista = new TypeToken<ArrayList<DTOContacto>>() {}.getType();
                this.contactosCache = gson.fromJson(gson.toJson(respuesta.getData()), tipoLista);

                System.out.println("✅ [GestionContactos]: " + contactosCache.size() + " contactos procesados correctamente");

                // Log detallado de los contactos (solo en debug)
                if (contactosCache.size() > 0) {
                    System.out.println("📋 [GestionContactos]: Contactos recibidos:");
                    for (DTOContacto contacto : contactosCache) {
                        System.out.println("   - ID: " + contacto.getId() + ", Nombre: " + contacto.getNombre() + ", Estado: " + contacto.getEstado());
                    }
                }

                notificarObservadores("ACTUALIZAR_CONTACTOS", this.contactosCache);
            } catch (Exception e) {
                System.err.println("❌ [GestionContactos]: Error al parsear contactos: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("❌ [GestionContactos]: Error en respuesta del servidor: " + respuesta.getMessage());
        }
    }

    @Override
    public void solicitarActualizacionContactos() {
        DTORequest peticion = new DTORequest("solicitarListaContactos", null);
        enviadorPeticiones.enviar(peticion);
    }

    @Override
    public List<DTOContacto> getContactos() {
        return new ArrayList<>(contactosCache);
    }

    // --- Métodos del Patrón Observador ---
    @Override
    public void registrarObservador(IObservador observador) {
        if (!observadores.contains(observador)) observadores.add(observador);
    }
    @Override
    public void removerObservador(IObservador observador) {
        observadores.remove(observador);
    }
    @Override
    public void notificarObservadores(String tipoDeDato, Object datos) {
        for (IObservador observador : observadores) {
            observador.actualizar(tipoDeDato, datos);
        }
    }
}
