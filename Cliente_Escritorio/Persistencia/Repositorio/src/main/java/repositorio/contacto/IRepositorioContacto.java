package repositorio.contacto;

import dominio.Contacto;

import java.util.List;
import java.util.UUID;

/**
 * Contrato para el Repositorio de Contactos.
 * Define operaciones CRUD básicas.
 */
public interface IRepositorioContacto {

    void guardar(Contacto contacto);

    Contacto obtenerPorId(UUID idContacto);

    void actualizar(Contacto contacto);

    void eliminar(UUID idContacto);

    List<Contacto> obtenerTodos();

    List<Contacto> obtenerActivos();
}

