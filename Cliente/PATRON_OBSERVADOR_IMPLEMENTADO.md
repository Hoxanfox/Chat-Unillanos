```java
// En FachadaLobby.java
public DTOUsuario cargarInformacionUsuarioLogueado() {
    String userId = gestorSesion.getUserId();
    UUID userIdUUID = UUID.fromString(userId);
    DTOUsuario dtoUsuario = especialistaUsuarios.obtenerUsuarioPorIdComoDTO(userIdUUID);
    return dtoUsuario;
}
```

---

## ✅ Ventajas de esta Implementación

1. **Desacoplamiento**: Las vistas no conocen los detalles de implementación de los gestores
2. **Reactividad**: La UI se actualiza automáticamente cuando cambia el estado
3. **Escalabilidad**: Es fácil agregar nuevos observadores sin modificar el código existente
4. **Mantenibilidad**: Cada capa tiene una responsabilidad clara
5. **Testabilidad**: Se pueden crear observadores mock para testing
6. **Sincronización**: Los datos se guardan en BD local automáticamente tras autenticación exitosa

---

## 🎯 Próximos Pasos

Para implementar el mismo patrón en otras funcionalidades:

1. **Gestión de Contactos**: Notificar cuando se agregue/elimine un contacto
2. **Gestión de Canales**: Notificar cuando se cree/actualice un canal
3. **Chat en Tiempo Real**: Notificar cuando llegue un nuevo mensaje
4. **Actualizaciones de Usuario**: Notificar cuando se actualice el perfil

### Patrón a seguir:
1. Hacer que el gestor de negocio implemente `ISujeto`
2. Agregar método `registrarObservador` en Fachada
3. Agregar método `registrarObservador` en Servicio
4. Agregar método `registrarObservador` en Controlador
5. Hacer que la Vista implemente `IObservador`
6. Registrar la vista en su constructor
7. Implementar el método `actualizar()` en la vista

---

## 📚 Documentación Relacionada

- **Gestores de Negocio**: Ver documentación en `Negocio/GestionUsuario/`
- **Repositorios**: Ver `Persistencia/Repositorio/README_REPOSITORIO.md`
- **Base de Datos H2**: Ver `BASE_DATOS_H2.md`
- **Sistema de Usuarios**: Ver `SISTEMA_GESTION_USUARIOS.md`

---

**Fecha de Implementación**: 16 de Octubre, 2025  
**Estado**: ✅ Implementado y Compilado Exitosamente
# Patrón Observador - Implementación Completa

## 📋 Descripción General

Se ha implementado el **Patrón Observador** en todo el sistema siguiendo el flujo arquitectónico:

```
Vista → Controlador → Servicio → Fachada → Gestores de Negocio
```

Este patrón permite que las **vistas** (UI) sean notificadas automáticamente cuando ocurren eventos importantes en la capa de negocio, especialmente durante los procesos de **autenticación** y **registro**.

---

## 🏗️ Arquitectura del Patrón Observador

### Flujo de Registro de Observadores

```
┌─────────────────┐
│  VistaLogin     │ (implementa IObservador)
│  VistaRegistro  │
└────────┬────────┘
         │ registrarObservador()
         ↓
┌─────────────────────────┐
│  ControladorAutenticacion│
└────────┬────────────────┘
         │ registrarObservador()
         ↓
┌─────────────────────────┐
│  ServicioAutenticacion   │
└────────┬────────────────┘
         │ registrarObservador()
         ↓
┌──────────────────────────────┐
│ FachadaAutenticacionUsuario  │
└────────┬─────────────────────┘
         │ registrarObservador()
         ↓
┌─────────────────────────────┐
│  AutenticarUsuario          │ (Gestor de Negocio)
│  RegistroUsuarioImpl        │ (Gestor de Negocio)
└─────────────────────────────┘
```

### Flujo de Notificaciones

```
┌─────────────────────────────┐
│  AutenticarUsuario          │ (Gestor de Negocio)
│  RegistroUsuarioImpl        │
└────────┬────────────────────┘
         │ notificarObservadores()
         ↓
         (El evento se propaga automáticamente)
         ↓
┌─────────────────┐
│  VistaLogin     │ → actualizar(tipoDeDato, datos)
│  VistaRegistro  │
└─────────────────┘
```

---

## 📝 Componentes Implementados

### 1. Interfaces Base (Infraestructura/Observador)

#### `IObservador`
```java
public interface IObservador {
    void actualizar(String tipoDeDato, Object datos);
}
```

#### `ISujeto`
```java
public interface ISujeto {
    void registrarObservador(IObservador observador);
    void removerObservador(IObservador observador);
    void notificarObservadores(String tipoDeDato, Object datos);
}
```

---

### 2. Gestores de Negocio (Implementan ISujeto)

#### `AutenticarUsuario`
- **Ubicación**: `Negocio/GestionUsuario/src/main/java/gestionUsuario/autenticacion/`
- **Eventos que notifica**:
  - `"AUTENTICACION_INICIADA"` - Cuando comienza el proceso
  - `"AUTENTICACION_EXITOSA"` - Cuando el servidor confirma las credenciales
  - `"USUARIO_LOGUEADO"` - Cuando el usuario se guarda en BD local y sesión
  - `"USUARIO_BANEADO"` - Cuando el usuario está suspendido
  - `"AUTENTICACION_ERROR"` - Cuando ocurre un error

#### `RegistroUsuarioImpl`
- **Ubicación**: `Negocio/GestionUsuario/src/main/java/gestionUsuario/registro/`
- **Eventos que notifica**:
  - `"REGISTRO_INICIADO"` - Cuando comienza el proceso
  - `"REGISTRO_EXITOSO"` - Cuando el usuario se registra correctamente
  - `"REGISTRO_ERROR"` - Cuando ocurre un error

---

### 3. Fachadas (Propagan el Registro de Observadores)

#### `FachadaAutenticacionUsuario`
- **Ubicación**: `Negocio/Fachada/src/main/java/fachada/gestionUsuarios/autenticacion/`
- **Métodos agregados**:
  ```java
  void registrarObservadorAutenticacion(IObservador observador);
  void registrarObservadorRegistro(IObservador observador);
  ```
- **Función**: Delega el registro a los gestores de negocio correspondientes

---

### 4. Servicios (Propagan el Registro de Observadores)

#### `ServicioAutenticacion`
- **Ubicación**: `Negocio/Servicio/src/main/java/servicio/autenticacion/`
- **Métodos agregados**:
  ```java
  void registrarObservadorAutenticacion(IObservador observador);
  void registrarObservadorRegistro(IObservador observador);
  ```
- **Función**: Delega el registro a la fachada

---

### 5. Controladores (Exponen el Registro a la Presentación)

#### `ControladorAutenticacion`
- **Ubicación**: `Presentacion/Controlador/src/main/java/controlador/autenticacion/`
- **Métodos agregados**:
  ```java
  void registrarObservadorAutenticacion(IObservador observador);
  void registrarObservadorRegistro(IObservador observador);
  ```
- **Función**: Punto de entrada para que las vistas se registren como observadores

---

### 6. Vistas (Implementan IObservador)

#### `VistaLogin`
- **Ubicación**: `Presentacion/InterfazEscritorio/src/main/java/interfazEscritorio/autenticacion/login/`
- **Implementa**: `IObservador`
- **Registro**: Se registra en el constructor:
  ```java
  controlador.registrarObservadorAutenticacion(this);
  ```
- **Reacciones**:
  - `AUTENTICACION_INICIADA`: Limpia mensajes de error
  - `AUTENTICACION_EXITOSA`: Muestra mensaje de éxito
  - `USUARIO_LOGUEADO`: **Navega al Lobby** (carga datos de BD local)
  - `USUARIO_BANEADO`: Muestra mensaje de suspensión
  - `AUTENTICACION_ERROR`: Muestra error y reactiva botón

#### `VistaRegistro`
- **Ubicación**: `Presentacion/InterfazEscritorio/src/main/java/interfazEscritorio/autenticacion/registro/`
- **Implementa**: `IObservador`
- **Registro**: Se registra en el constructor:
  ```java
  controlador.registrarObservadorRegistro(this);
  ```
- **Reacciones**:
  - `REGISTRO_INICIADO`: Limpia mensajes de error
  - `REGISTRO_EXITOSO`: Muestra mensaje de éxito y **navega al login** después de 1.5s
  - `REGISTRO_ERROR`: Muestra error y reactiva botón

---

## 🔄 Flujo Completo de Autenticación

### 1. Usuario hace clic en "Ingresar"
```
VistaLogin.btnLogin.onClick()
  → controlador.autenticar(datos)
    → servicio.autenticar(datos)
      → fachada.autenticarUsuario(datos)
        → gestorAutenticacion.autenticar(datos)
```

### 2. Gestor envía petición al servidor
```
AutenticarUsuario
  → notificarObservadores("AUTENTICACION_INICIADA", email)
  → enviadorPeticiones.enviar(peticion)
```

### 3. Servidor responde con datos del usuario
```
AutenticarUsuario.gestorRespuesta
  → Recibe DTOResponse
  → Guarda/actualiza usuario en BD local (H2)
  → Guarda usuario en sesión (GestorSesionUsuario)
  → notificarObservadores("AUTENTICACION_EXITOSA", usuario)
  → notificarObservadores("USUARIO_LOGUEADO", usuario)
```

### 4. Vista reacciona a las notificaciones
```
VistaLogin.actualizar("USUARIO_LOGUEADO", usuario)
  → Platform.runLater(() -> {
      onLoginExitoso.run(); // Navega al Lobby
  })
```

### 5. Lobby carga información desde BD local
```
VistaLobby.cargarInformacionUsuario()
  → controladorUsuario.cargarInformacionUsuarioLogueado()
    → servicioUsuario.cargarInformacionUsuarioLogueado()
      → fachadaLobby.cargarInformacionUsuarioLogueado()
        → especialistaUsuarios.obtenerUsuarioPorIdComoDTO(userId)
          → repositorioUsuario.obtenerPorId(userId) // Lee de H2
```

---

## 🔄 Flujo Completo de Registro

### 1. Usuario hace clic en "Registrarse"
```
VistaRegistro.btnRegistro.onClick()
  → controlador.registrar(datosFormulario)
    → servicioArchivos.subirArchivoParaRegistro(foto)
    → servicio.registrar(datosRegistro, fotoBytes)
      → fachada.registrarUsuario(datosRegistro, fotoBytes)
        → gestorRegistro.registrar(datosRegistro, fotoBytes)
```

### 2. Gestor envía petición al servidor
```
RegistroUsuarioImpl
  → notificarObservadores("REGISTRO_INICIADO", datosRegistro)
  → enviadorPeticiones.enviar(peticion)
```

### 3. Servidor responde con userId y photoId
```
RegistroUsuarioImpl.gestorRespuesta
  → Recibe DTOResponse
  → Crea entidad Usuario con los datos
  → Guarda usuario en BD local (H2) con fotoBytes
  → notificarObservadores("REGISTRO_EXITOSO", usuario)
```

### 4. Vista reacciona a la notificación
```
VistaRegistro.actualizar("REGISTRO_EXITOSO", usuario)
  → Muestra mensaje de éxito
  → Espera 1.5 segundos
  → onRegistroExitoso.run(); // Navega al Login
```

---

## 🗄️ Interacción con Base de Datos Local

### Después de Autenticación Exitosa
El usuario se guarda/actualiza en la BD local H2:

```java
// En AutenticarUsuario.java
Usuario usuarioExistente = especialistaUsuarios.obtenerUsuarioPorId(userId);
if (usuarioExistente != null) {
    // Actualizar usuario existente
    usuario = usuarioExistente;
    usuario.setNombre(nombre);
    usuario.setEmail(email);
    especialistaUsuarios.actualizarUsuario(usuario);
} else {
    // Crear nuevo usuario
    usuario = new Usuario();
    usuario.setIdUsuario(userId);
    usuario.setNombre(nombre);
    especialistaUsuarios.guardarUsuario(usuario);
}
```

### En el Lobby
La información se carga desde la BD local:


