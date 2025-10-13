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
        this.gestorRespuesta.registrarManejador("actualizarListaContactos", this::manejarActualizacion);
    }

    private void manejarActualizacion(DTOResponse respuesta) {
        if (respuesta.fueExitoso()) {
            Type tipoLista = new TypeToken<ArrayList<DTOContacto>>() {}.getType();
            this.contactosCache = gson.fromJson(gson.toJson(respuesta.getData()), tipoLista);
            notificarObservadores("ACTUALIZAR_CONTACTOS", this.contactosCache);
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
