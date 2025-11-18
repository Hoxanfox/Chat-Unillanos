package conexion.fabrica;

import transporte.ITransporte;

/**
 * Interfaz para fábrica de transportes inyectable (permite mockear en tests).
 */
public interface ITransporteFactory {
    ITransporte crearTransporte(String tipo);
}

