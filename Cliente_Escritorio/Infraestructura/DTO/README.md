Estructura y organización de DTO (Infraestructura/DTO)

Resumen
-------
Este módulo contiene los DTOs utilizados por el proyecto. Se ha realizado una reorganización para separar claramente:

- Request (peticiones)  -> paquetes bajo `dto.comunicacion.request...`
- Response (respuestas) -> paquetes bajo `dto.comunicacion.response...`
- Features organizadas por responsabilidad (mensaje, canal, gestionArchivos, etc.)

Motivación
----------
Mantener DTOs organizados por dirección (request/response) y por feature facilita la mantenibilidad, descubrimiento y evolución de la API entre cliente/servidor.

Qué se hizo
-----------
- Se añadieron wrappers organizados por feature y por tipo (request/response). Estos wrappers usan composición para delegar en los DTOs originales (ubicados en paquetes previos como `dto.peticion`, `dto.comunicacion.peticion.*`, `dto.gestionArchivos`, etc.).
- Los DTO originales se mantuvieron para evitar rupturas inmediatas en el resto del código.
- Se corrigieron delegaciones y nombres de getters para que el módulo compile correctamente.

Paquetes principales nuevos (ejemplos)
--------------------------------------
- dto.comunicacion.request.mensaje    -> Request relacionados a mensajes
- dto.comunicacion.response.mensaje   -> Responses de historial/msgs
- dto.comunicacion.request.canal      -> Requests relacionados a canales (crear, listar, invitar...)
- dto.gestionArchivos.request         -> Requests de subida/descarga
- dto.gestionArchivos.response        -> Responses de descarga

Mapeo (ejemplos) -> wrapper
---------------------------
- `dto.comunicacion.peticion.mensaje.DTOSolicitarHistorial`  => `dto.comunicacion.request.mensaje.DTOSolicitarHistorial` (wrapper)
- `dto.comunicacion.respuesta.DTOHistorialMensajes`          => `dto.comunicacion.response.mensaje.DTOHistorialMensajes` (wrapper)
- `dto.gestionArchivos.DTODownloadChunk`                     => `dto.gestionArchivos.response.DTODownloadChunk` (wrapper)

Notas de migración
------------------
- Las clases originales siguen existiendo y el código antiguo continúa funcionando.
- Para migrar a la nueva organización, actualiza los imports en los módulos consumidores a los wrappers deseados.
- Ten en cuenta que los wrappers usan composición: algunos no exponeconstructores idénticos; revisa la API pública antes de migrar automáticamente.

Sugerencia para migración manual (búsqueda/reemplazo):
- Buscar importaciones existentes como `import dto.comunicacion.peticion.mensaje.*` y reemplazarlas por `import dto.comunicacion.request.mensaje.*` donde proceda.
- Revisa llamadas directas a constructores si migras a wrappers; puede que necesites instanciar primero el DTO original y luego el wrapper.

Pasos siguientes recomendados
---------------------------
1. Decidir estrategia: mantener ambos (wrappers + originales) o mover definitivamente las clases originales a los nuevos paquetes (más trabajo pero más limpio).
2. Si se decide migrar automáticamente, puedo:
   - Actualizar imports en todo el repo (con precaución), compilar y arreglar los puntos que fallen.
   - O realizar una migración por etapas (modulo por modulo) y ejecutar compilación después de cada paso.

Estado actual
-------------
- El módulo `Infraestructura/DTO` compila correctamente.
- No se realizaron cambios que rompan la compilación del proyecto.

Si quieres, procedo a una migración automática de imports en todo el repo (y luego compilo para validar). Antes de hacerlo, dime si prefieres una migración total (reemplazar imports y adaptar llamadas a constructores) o una migración gradual por módulos.

