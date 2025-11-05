## Usuario
| **Acción**       | **Descripción**         | **Dirección**      | Ruta                 |
| ---------------- | ----------------------- | ------------------ | -------------------- |
| authenticateUser | Autentica un usuario    | Cliente → Servidor | [[authenticateUser]] |
| logoutUser       | Cerrar la sesion actual | Cliente → Servidor | [[logoutUser]]       |
## Contacto

| **Acción**                | **Descripción**                                                  | **Dirección**      | Ruta                          |
| ------------------------- | ---------------------------------------------------------------- | ------------------ | ----------------------------- |
| listarContactos           | Listar los contactos online                                      | Cliente → Servidor | [[listarContactos]]           |
| enviarMensajeDirecto      | Envia mensaje a contacto                                         | Cliente → Servidor | [[enviarMensajeDirecto]]      |
| enviarMensajeDirectoAudio | Envia mensaje de audio a contacto                                | Cliente → Servidor | [[enviarMensajeDirectoAudio]] |
| solicitarListaContactos   | Solicitar la lista de contactos actualizada                      | Servidor → Cliente | [[solicitarListaContactos]]   |
| nuevoMensajeDirecto       | Actualiza un mensaje nuevo de algun contacto                     | Servidor → Cliente | [[nuevoMensajeDirecto]]       |
| nuevoMensajeDirectoAudio  | Actualiza un mensaje nuevo de audio de algun contacto            | Servidor → Cliente | [[nuevoMensajeDirectoAudio]]  |
| solicitarHistorialPrivado | Solicita el historial de mensajes entre un usuario y un contacto | Servidor → Cliente | [[solicitarHistorialPrivado]] |
## Grupo

| **Acción**                        | **Descripción**                                               | **Dirección**      | Ruta                                  |
| --------------------------------- | ------------------------------------------------------------- | ------------------ | ------------------------------------- |
| listarGruposPropios               | Listar grupo a los que pertenece                              | Cliente → Servidor | [[listarGruposPropios]]               |
| crearGrupo                        | Crea un grupo privado                                         | Cliente → Servidor | [[crearGrupo]]                        |
| invitarMiembroGrupo               | Enviar una invitacion de canal a un contacto                  | Cliente → Servidor | [[invitarMiembroGrupo]]               |
| notificacionInvitacionGrupo       | Notifica la respuesta del contacto a la invitacion            | Servidor → Cliente | [[notificacionInvitacionGrupo]]       |
| respuestaInvitacionGrupo          | Respuesta de la invitación a pertenecer a un grupo            | Cliente → Servidor | [[respuestaInvitacionGrupo]]          |
| notificacionRespuestaNuevoMiembro | es una notifiacion de la respuesta de un nuevo posible miebro | Servidor → Cliente | [[notificacionRespuestaNuevoMiembro]] |


## Archivos
| **Acción** | **Descripción** | **Dirección** | Ruta |
| ---------- | --------------- | ------------- | ---- |
|            |                 |               |      |
