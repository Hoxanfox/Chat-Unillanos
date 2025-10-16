# Documentación de Controladores

## Visión General

Los controladores forman parte de la **capa de Presentación** y actúan como intermediarios entre las vistas (interfaz gráfica) y los servicios (lógica de negocio).

### Responsabilidades

1. **Recibir peticiones** desde las vistas JavaFX
2. **Delegar operaciones** a la capa de servicios
3. **Convertir objetos de dominio a DTOs** para la presentación
4. **Gestionar el registro de observadores** para comunicación asíncrona
5. **Mantener el desacoplamiento** arquitectónico

---

## ControladorCanalesImpl

**Ubicación**: `Presentacion/Controlador/src/main/java/controlador/canales/ControladorCanalesImpl.java`

**Interfaz**: `IControladorCanales`

### Descripción

Controlador que gestiona todas las operaciones relacionadas con canales de comunicación grupal.

### Dependencias

- `IServicioCanales`: Servicio de negocio para operaciones de canales
- `DTOCanalCreado`: DTO para transferir información de canales creados

### Métodos Principales

#### `crearCanal(String nombre, String descripcion)`
- **Propósito**: Crea un nuevo canal de comunicación
- **Parámetros**:
  - `nombre`: Nombre del canal
  - `descripcion`: Descripción del canal
- **Retorno**: `CompletableFuture<DTOCanalCreado>` con los datos del canal creado
- **Flujo**: Delega al servicio y convierte el objeto de dominio `Canal` a `DTOCanalCreado`

#### `solicitarCanalesUsuario()`
- **Propósito**: Solicita al servidor la lista de canales del usuario
- **Comunicación**: Asíncrona a través del patrón Observer
- **Respuesta**: Los datos llegan mediante observadores registrados

#### `obtenerCanalesCache()`
- **Propósito**: Obtiene la lista de canales desde la caché local
- **Retorno**: `List<DTOCanalCreado>` con los canales disponibles
- **Uso**: Para mostrar datos inmediatamente sin esperar al servidor

#### `enviarMensajeTexto(String canalId, String contenido)`
- **Propósito**: Envía un mensaje de texto a un canal específico
- **Parámetros**:
  - `canalId`: ID del canal destino
  - `contenido`: Texto del mensaje
- **Retorno**: `CompletableFuture<Void>` que se completa al enviar

#### `invitarMiembro(String canalId, String contactoId)`
- **Propósito**: Invita a un contacto a unirse a un canal
- **Parámetros**:
  - `canalId`: ID del canal
  - `contactoId`: ID del usuario a invitar
- **Retorno**: `CompletableFuture<Void>` confirmando la invitación

#### `solicitarMiembrosCanal(String canalId)`
- **Propósito**: Solicita la lista de miembros de un canal
- **Parámetro**: `canalId` - ID del canal
- **Comunicación**: Los miembros se reciben mediante observadores

### Patrón Observer

El controlador gestiona múltiples tipos de observadores:

1. **Observador de Creación**: Notifica cuando se crea un canal
2. **Observador de Listado**: Notifica cambios en la lista de canales
3. **Observador de Mensajes**: Notifica nuevos mensajes en canales
4. **Observador de Miembros**: Notifica cambios en los miembros de canales

### Ejemplo de Uso

```java
// Crear el controlador
IControladorCanales controlador = new ControladorCanalesImpl();

// Registrar observador para mensajes
controlador.registrarObservadorMensajes(new IObservador() {
    @Override
    public void actualizar(String tipo, Object datos) {
        if ("MENSAJE_RECIBIDO".equals(tipo)) {
            // Actualizar la UI con el nuevo mensaje
        }
    }
});

// Crear un canal
controlador.crearCanal("General", "Canal de discusión general")
    .thenAccept(canal -> {
        System.out.println("Canal creado: " + canal.getNombre());
    });

// Enviar mensaje
controlador.enviarMensajeTexto(canalId, "Hola a todos!");
```

### Principios Arquitectónicos

✅ **Separación de Responsabilidades**: Solo coordina, no implementa lógica de negocio  
✅ **Desacoplamiento**: No expone objetos de dominio, solo DTOs  
✅ **Delegación**: Todas las operaciones se delegan al servicio  
✅ **Asincronía**: Usa `CompletableFuture` y patrón Observer  

---

## Notas de Implementación

### Conversión Dominio → DTO

El controlador realiza conversiones explícitas:

```java
// Convertir Canal (dominio) a DTOCanalCreado (DTO)
.thenApply(canal -> new DTOCanalCreado(
    canal.getIdCanal().toString(), 
    canal.getNombre()
))
```

### Inicialización

El controlador se auto-inicializa con sus dependencias:

```java
public ControladorCanalesImpl() {
    this.servicioCanales = new ServicioCanalesImpl();
}
```

### Logging

Incluye mensajes de depuración para trazabilidad:

```java
System.out.println("🎮 [ControladorCanales]: Solicitando miembros del canal: " + canalId);
```

---

## Diagrama de Flujo

```
Vista (JavaFX)
    ↓
ControladorCanalesImpl
    ↓ (delega)
ServicioCanalesImpl
    ↓ (delega)
FachadaCanalesImpl
    ↓ (ejecuta)
Gestores de Negocio
    ↓ (persiste)
Repositorios
    ↓ (comunica)
Servidor
```

---

## Mejoras Futuras

- [ ] Inyección de dependencias mediante framework (Spring/CDI)
- [ ] Manejo de errores más robusto con tipos `Either` o `Result`
- [ ] Métricas y monitoreo de operaciones
- [ ] Cache local más sofisticado con TTL

