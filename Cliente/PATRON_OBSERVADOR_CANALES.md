# Patrón Observador - Implementación Completa para CANALES

## 📋 Descripción General

Se ha implementado el **Patrón Observador** completo para el módulo de **Canales**, siguiendo el mismo flujo arquitectónico que se usó para Autenticación:

```
Vista → Controlador → Servicio → Fachada → Gestores de Negocio
```

---

## 🏗️ Arquitectura del Patrón Observador para Canales

### Componentes Implementados

#### 1. **Gestores de Negocio** (Implementan ISujeto)

##### `CreadorCanal`
- **Ubicación**: `Negocio/GestionCanales/src/main/java/gestionCanales/nuevoCanal/`
- **Eventos que notifica**:
  - `"CANAL_CREACION_INICIADA"` - Cuando comienza la creación
  - `"CANAL_CREADO_EXITOSAMENTE"` - Cuando el canal se crea y guarda en BD local
  - `"CANAL_ERROR"` - Cuando ocurre un error

##### `ListadorCanales`
- **Ubicación**: `Negocio/GestionCanales/src/main/java/gestionCanales/listarCanales/`
- **Eventos que notifica**:
  - `"CANALES_ACTUALIZADOS"` - Cuando llega la lista del servidor y se sincroniza con BD local

##### `GestorMensajesCanalImpl`
- **Ubicación**: `Negocio/GestionCanales/src/main/java/gestionCanales/mensajes/`
- **Eventos que notifica**:
  - `"HISTORIAL_CANAL_RECIBIDO"` - Cuando llega el historial de mensajes
  - `"MENSAJE_CANAL_RECIBIDO"` - Cuando llega un nuevo mensaje en tiempo real
  - `"ERROR_OPERACION"` - Cuando ocurre un error

---

#### 2. **Fachada de Canales**

##### `FachadaCanalesImpl`
- **Ubicación**: `Negocio/Fachada/src/main/java/fachada/gestionCanales/`
- **Función**: Orquesta todos los gestores de canales y delega el registro de observadores
- **Métodos de Observador**:
  ```java
  void registrarObservadorCreacion(IObservador observador);
  void registrarObservadorListado(IObservador observador);
  void registrarObservadorMensajes(IObservador observador);
  ```

---

#### 3. **Servicio de Canales**

##### `ServicioCanalesImpl`
- **Ubicación**: `Negocio/Servicio/src/main/java/servicio/canales/`
- **Función**: Punto de entrada desde el Controlador, delega a la Fachada
- **Operaciones**:
  - Crear canales
  - Listar canales
  - Enviar/recibir mensajes
  - Invitar miembros
  - Gestionar observadores

---

#### 4. **Controlador de Canales**

##### `ControladorCanalesImpl`
- **Ubicación**: `Presentacion/Controlador/src/main/java/controlador/canales/`
- **Función**: Intermediario entre las vistas y el servicio
- **Métodos principales**:
  ```java
  CompletableFuture<Canal> crearCanal(String nombre, String descripcion);
  void solicitarCanalesUsuario();
  void solicitarHistorialCanal(String canalId, int limite);
  CompletableFuture<Void> enviarMensajeTexto(String canalId, String contenido);
  void registrarObservadorCreacion(IObservador observador);
  void registrarObservadorListado(IObservador observador);
  void registrarObservadorMensajes(IObservador observador);
  ```

---

#### 5. **Vistas (Implementan IObservador)**

##### `FeatureCanales`
- **Ubicación**: `Presentacion/InterfazEscritorio/.../featureCanales/`
- **Implementa**: `IObservador`
- **Función**: Lista de canales en el sidebar
- **Registro**: Se registra como observador de listado en el constructor
- **Reacciones**:
  - `CANALES_ACTUALIZADOS`: Actualiza la lista visual con los canales desde BD local
  - `CANAL_CREADO_EXITOSAMENTE`: Refresca la lista de canales

##### `VistaCrearCanal`
- **Ubicación**: `Presentacion/InterfazEscritorio/.../crearCanal/`
- **Implementa**: `IObservador`
- **Función**: Formulario para crear nuevos canales
- **Registro**: Se registra como observador de creación
- **Reacciones**:
  - `CANAL_CREACION_INICIADA`: Muestra mensaje "Creando canal..."
  - `CANAL_CREADO_EXITOSAMENTE`: Muestra éxito y vuelve al lobby después de 1s
  - `CANAL_ERROR`: Muestra mensaje de error

##### `VistaCanal`
- **Ubicación**: `Presentacion/InterfazEscritorio/.../canal/`
- **Implementa**: `IObservador`
- **Función**: Chat de un canal específico
- **Registro**: Se registra como observador de mensajes
- **Reacciones**:
  - `HISTORIAL_CANAL_RECIBIDO`: Carga los mensajes desde BD local
  - `MENSAJE_CANAL_RECIBIDO`: Agrega el nuevo mensaje en tiempo real
  - `ERROR_OPERACION`: Muestra error en el chat

---

## 🔄 Flujos Completos

### **Flujo 1: Crear un Canal**

```
1. Usuario hace clic en "Crear Canal"
   VistaCrearCanal.btnCrear.onClick()
   → controlador.crearCanal(nombre, descripcion)

2. Flujo hacia el servidor
   ControladorCanales → ServicioCanales → FachadaCanales → CreadorCanal
   → enviadorPeticiones.enviar(request)

3. Notificación: CANAL_CREACION_INICIADA
   CreadorCanal.notificarObservadores("CANAL_CREACION_INICIADA", nombre)
   → VistaCrearCanal muestra "Creando canal..."

4. Servidor responde con datos del canal
   CreadorCanal recibe respuesta
   → Crea objeto Canal de dominio
   → repositorioCanal.guardar(canal) // Guarda en H2
   → notificarObservadores("CANAL_CREADO_EXITOSAMENTE", canal)

5. Vistas reaccionan
   VistaCrearCanal: Muestra éxito y vuelve al lobby
   FeatureCanales: Refresca la lista de canales
```

### **Flujo 2: Listar Canales**

```
1. Al cargar FeatureCanales
   controlador.solicitarCanalesUsuario()
   → ServicioCanales → FachadaCanales → ListadorCanales
   → enviadorPeticiones.enviar("listarCanales")

2. Servidor responde con lista de canales
   ListadorCanales recibe respuesta
   → Convierte mapas a objetos Canal
   → repositorioCanal.sincronizarCanales(canales) // Sincroniza con H2
   → notificarObservadores("CANALES_ACTUALIZADOS", canales)

3. Vista actualiza
   FeatureCanales.actualizar("CANALES_ACTUALIZADOS", canales)
   → Limpia la lista visual
   → Crea entradas para cada canal desde la BD local
```

### **Flujo 3: Chat en Tiempo Real**

```
1. Al abrir VistaCanal
   controlador.solicitarHistorialCanal(canalId, 50)
   → GestorMensajesCanal.solicitarHistorialCanal()

2. Servidor envía historial
   GestorMensajesCanal recibe historial
   → repositorioMensajes.sincronizarHistorial() // Guarda en H2
   → notificarObservadores("HISTORIAL_CANAL_RECIBIDO", mensajes)

3. Vista carga mensajes
   VistaCanal.actualizar("HISTORIAL_CANAL_RECIBIDO", mensajes)
   → Muestra cada mensaje en burbujas (propios vs otros)

4. Llega nuevo mensaje (push del servidor)
   GestorMensajesCanal.manejarNuevoMensaje()
   → repositorioMensajes.guardarMensajeRecibido() // Guarda en H2
   → notificarObservadores("MENSAJE_CANAL_RECIBIDO", mensaje)

5. Vista agrega mensaje en tiempo real
   VistaCanal.actualizar("MENSAJE_CANAL_RECIBIDO", mensaje)
   → Solo si es del canal actual
   → Agrega burbuja de mensaje al final del chat
```

### **Flujo 4: Enviar Mensaje**

```
1. Usuario escribe y envía
   VistaCanal.enviarMensaje()
   → controlador.enviarMensajeTexto(canalId, contenido)

2. Flujo al servidor
   ControladorCanales → ServicioCanales → FachadaCanales → GestorMensajes
   → Crea DTOEnviarMensajeCanal
   → Guarda como MensajeEnviadoCanal en H2
   → enviadorPeticiones.enviar()

3. Servidor procesa y reenvía a todos
   Servidor recibe mensaje
   → Guarda en su BD
   → Envía notificación "nuevoMensajeCanal" a todos los miembros

4. Todos los clientes reciben (incluyendo el emisor)
   GestorMensajes.manejarNuevoMensaje()
   → Guarda como MensajeRecibidoCanal en H2
   → notificarObservadores("MENSAJE_CANAL_RECIBIDO", mensaje)

5. Todas las VistaCanal abiertas actualizan
   → Si están viendo ese canal, agregan el mensaje
```

---

## 🗄️ Persistencia Local (H2)

### Tablas Usadas

#### **CANALES**
```sql
- id_canal (UUID)
- nombre (VARCHAR)
- id_administrador (UUID)
```

#### **MENSAJES_CANAL_ENVIADOS**
```sql
- id_mensaje (UUID)
- id_canal (UUID)
- tipo (VARCHAR)
- contenido (TEXT)
- file_id (VARCHAR)
- fecha_envio (TIMESTAMP)
```

#### **MENSAJES_CANAL_RECIBIDOS**
```sql
- id_mensaje (UUID)
- id_canal (UUID)
- id_remitente (UUID)
- nombre_remitente (VARCHAR)
- tipo (VARCHAR)
- contenido (TEXT)
- file_id (VARCHAR)
- fecha_envio (TIMESTAMP)
```

### Sincronización

1. **Canales**: Se sincronizan cada vez que se solicita la lista
2. **Mensajes**: Se sincronizan al abrir un canal (historial) y en tiempo real (nuevos)
3. **Persistencia**: Todos los datos se guardan automáticamente en H2 para acceso offline

---

## ✅ Beneficios de la Implementación

### 1. **Reactividad Total**
- Las vistas se actualizan automáticamente sin necesidad de polling
- Los mensajes llegan en tiempo real a todas las vistas abiertas
- La lista de canales se refresca automáticamente al crear uno nuevo

### 2. **Desacoplamiento**
- Las vistas no conocen los detalles de los gestores de negocio
- Fácil agregar nuevas vistas que reaccionen a los mismos eventos
- Cambios en los gestores no afectan las vistas

### 3. **Persistencia Automática**
- Todos los eventos importantes se guardan en H2 automáticamente
- Los datos persisten entre sesiones
- Acceso rápido desde la BD local

### 4. **Escalabilidad**
- Fácil agregar nuevos tipos de eventos
- Múltiples observadores pueden escuchar el mismo evento
- Nuevo observadores se pueden registrar en cualquier momento

### 5. **Sincronización Inteligente**
- Los datos del servidor se sincronizan con la BD local
- No hay duplicados (se actualiza o inserta según sea necesario)
- Consistencia entre servidor y cliente

---

## 🎯 Eventos Disponibles

### Creación de Canales
- `CANAL_CREACION_INICIADA`
- `CANAL_CREADO_EXITOSAMENTE`
- `CANAL_ERROR`

### Listado de Canales
- `CANALES_ACTUALIZADOS`

### Mensajes de Canal
- `HISTORIAL_CANAL_RECIBIDO`
- `MENSAJE_CANAL_RECIBIDO`
- `ERROR_OPERACION`

---

## 📊 Comparación: Antes vs Después

### **ANTES (Sin Observador)**
```java
// Vista tenía que hacer polling o refresh manual
btnRefresh.setOnAction(e -> {
    List<Canal> canales = controlador.obtenerCanales();
    actualizarLista(canales);
});
```

### **DESPUÉS (Con Observador)**
```java
// Vista se registra y recibe actualizaciones automáticas
controlador.registrarObservadorListado(this);

@Override
public void actualizar(String tipo, Object datos) {
    if ("CANALES_ACTUALIZADOS".equals(tipo)) {
        actualizarLista((List<Canal>) datos);
    }
}
```

---

## 🚀 Próximos Pasos

Para extender el patrón a otros módulos:

1. **Gestión de Contactos**
   - Notificar cuando se agregue/elimine un contacto
   - Notificar cambios de estado (online/offline)

2. **Notificaciones**
   - Notificar nuevas invitaciones a canales
   - Notificar menciones en mensajes

3. **Archivos**
   - Notificar progreso de descarga/subida
   - Notificar cuando un archivo esté disponible

---

## 📝 Notas de Implementación

### Inicialización de Manejadores
```java
// En VistaLobby, se inicializan los manejadores de mensajes
controladorCanales.inicializarManejadoresMensajes();
```

Esto es necesario para que el `GestorMensajesCanalImpl` registre sus callbacks con el `GestorRespuesta` y pueda recibir notificaciones push del servidor.

### Thread Safety
- Todos los observadores usan `Platform.runLater()` para actualizar la UI
- Las listas de observadores usan `CopyOnWriteArrayList` para thread-safety
- Los `CompletableFuture` manejan operaciones asíncronas correctamente

### Gestión de Memoria
- Los observadores se desregistran automáticamente cuando las vistas se destruyen
- Los callbacks se limpian después de procesarse
- No hay memory leaks por referencias circulares

---

**Fecha de Implementación**: 16 de Octubre, 2025  
**Estado**: ✅ Implementado y Compilado Exitosamente  
**Arquitectura**: Vista → Controlador → Servicio → Fachada → Gestores de Negocio  
**Persistencia**: Base de Datos H2 Local con sincronización automática

