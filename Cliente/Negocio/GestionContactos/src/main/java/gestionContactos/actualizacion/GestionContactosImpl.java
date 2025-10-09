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
 * Implementación del gestor de contactos. Contiene la lógica de negocio real
 * y se comunica con la capa de persistencia (comunicación).
 */
public class GestionContactosImpl implements IGestionContactos {

    private final List<IObservador> observadores = new ArrayList<>();
    private List<DTOContacto> contactosCache = new ArrayList<>();

    // Dependencias con la capa de comunicación
    private final IEnviadorPeticiones enviadorPeticiones;
    private final IGestorRespuesta gestorRespuesta;
    private final Gson gson;

    public GestionContactosImpl() {
        this.enviadorPeticiones = new EnviadorPeticiones();
        this.gestorRespuesta = new GestorRespuesta();
        this.gson = new Gson();
        this.gestorRespuesta.registrarManejador("actualizarListaContactos", this::manejarActualizacion);
    }

    private void manejarActualizacion(DTOResponse respuesta) {
        if (respuesta.fueExitoso()) {
            Type tipoLista = new TypeToken<ArrayList<DTOContacto>>() {}.getType();
            this.contactosCache = gson.fromJson(gson.toJson(respuesta.getData()), tipoLista);
            // Notifica a sus observadores (la Fachada) que los datos han cambiado.
            notificarObservadores("ACTUALIZAR_CONTACTOS", this.contactosCache);
        }
    }

    @Override
    public void solicitarActualizacionContactos() {
        DTORequest peticion = new DTORequest("solicitarListaContactos", null);
        enviadorPeticiones.enviar(peticion);
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
