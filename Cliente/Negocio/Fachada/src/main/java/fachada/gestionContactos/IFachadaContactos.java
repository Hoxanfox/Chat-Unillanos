package fachada.gestionContactos;

import observador.ISujeto;

/**
 * Contrato para la Fachada que gestiona la lógica de contactos.
 * Es el punto de entrada desde el Servicio.
 */
public interface IFachadaContactos extends ISujeto {
    void solicitarActualizacionContactos();
}
