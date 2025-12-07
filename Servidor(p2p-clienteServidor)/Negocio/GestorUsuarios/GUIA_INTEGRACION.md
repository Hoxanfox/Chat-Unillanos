# Guía de Integración - Gestión de Usuarios con Sincronización P2P

## Arquitectura Implementada

Se ha implementado la arquitectura completa siguiendo el patrón **Vista → Controlador → Servicio → Gestor → Repositorio** para la gestión de usuarios.

### Flujo de Capas

```
┌─────────────────────────────────────────────────────────────────┐
│                         CAPA DE VISTA                           │
│  PanelUsuarios, DialogoUsuario, BarraHerramientas, TablaUsuarios│
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ↓
┌─────────────────────────────────────────────────────────────────┐
│                      CAPA DE CONTROLADOR                        │
│                    ControladorUsuarios                          │
│  (Maneja eventos de UI, validaciones y mensajes al usuario)    │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ↓
┌─────────────────────────────────────────────────────────────────┐
│                      CAPA DE SERVICIO                           │
│                 ServicioGestionUsuarios                         │
│      (Orquesta transacciones y operaciones complejas)           │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ↓
┌─────────────────────────────────────────────────────────────────┐
│                      CAPA DE GESTOR                             │
│                      GestorUsuarios                             │
│  (Lógica de negocio, validaciones y notificación a observadores)│
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ↓
┌─────────────────────────────────────────────────────────────────┐
│                    CAPA DE REPOSITORIO                          │
│                    UsuarioRepositorio                           │
│              (Acceso a datos y persistencia)                    │
└─────────────────────────────────────────────────────────────────┘
```

## Componentes Creados

### 1. DTOs (Data Transfer Objects)

#### `DTOCrearUsuario`
```java
// Ubicación: Infraestructura/DTO/src/main/java/dto/usuario/
```
- Contiene: nombre, email, contraseña, foto, peerPadreId
- Usado para crear nuevos usuarios

#### `DTOActualizarUsuario`
```java
// Ubicación: Infraestructura/DTO/src/main/java/dto/usuario/
```
- Contiene: id, nombre, email, foto, contraseña, estado
- Usado para actualizar usuarios existentes

### 2. Gestor de Usuarios

#### `GestorUsuarios`
```java
// Ubicación: Negocio/GestorUsuarios/src/main/java/gestorUsuarios/
```

**Responsabilidades:**
- Lógica de negocio para usuarios
- Validaciones de datos
- Notificación a observadores cuando se crean/actualizan/eliminan usuarios
- Conversión de entidades de dominio a DTOs

**Métodos principales:**
- `crearUsuario(DTOCrearUsuario dto)` → DTOUsuarioVista
- `actualizarUsuario(DTOActualizarUsuario dto)` → DTOUsuarioVista
- `listarUsuarios()` → List<DTOUsuarioVista>
- `buscarPorId(String id)` → DTOUsuarioVista
- `buscarPorEmail(String email)` → DTOUsuarioVista
- `cambiarEstado(String id, Usuario.Estado estado)` → boolean

### 3. Servicio de Gestión

#### `ServicioGestionUsuarios`
```java
// Ubicación: Negocio/GestorUsuarios/src/main/java/gestorUsuarios/servicios/
```

**Responsabilidades:**
- Orquestación de operaciones
- Manejo de transacciones
- Logging de operaciones
- Capa intermedia entre controlador y gestor

### 4. Controlador

#### `ControladorUsuarios`
```java
// Ubicación: Presentacion/Controlador/src/main/java/controlador/usuarios/
```

**Responsabilidades:**
- Coordinar entre vista y servicio
- Validaciones previas
- Mostrar mensajes al usuario (éxito, error, advertencia)
- Manejo de excepciones

### 5. Observador de Sincronización

#### `ObservadorSincronizacionUsuarios`
```java
// Ubicación: Negocio/GestorUsuarios/src/main/java/gestorUsuarios/observadores/
```

**Responsabilidades:**
- Escuchar eventos del GestorUsuarios
- Activar sincronización P2P cuando se crean/actualizan/eliminan usuarios
- Conectar el módulo de usuarios con el ServicioSincronizacionDatos

## Integración con Sincronización P2P

### Flujo de Sincronización

```
1. Usuario crea/actualiza/elimina un usuario en la interfaz
   ↓
2. PanelUsuarios → ControladorUsuarios → ServicioGestionUsuarios
   ↓
3. GestorUsuarios.crearUsuario(dto)
   ↓
4. UsuarioRepositorio.guardar(usuario) ← Persistencia local
   ↓
5. GestorUsuarios notifica: "USUARIO_CREADO"
   ↓
6. ObservadorSincronizacionUsuarios.actualizar("USUARIO_CREADO", usuario)
   ↓
7. ServicioSincronizacionDatos.forzarSincronizacion()
   ↓
8. Sincronización P2P activa → Otros peers reciben el usuario
```

## Cómo Integrar en tu Aplicación

### Paso 1: Compilar el módulo GestorUsuarios

```bash
cd Negocio/GestorUsuarios
mvn clean install
```

### Paso 2: Instanciar los componentes

```java
// En tu clase Main o donde inicialices la aplicación

// 1. Obtener la instancia del ServicioSincronizacionDatos
ServicioSincronizacionDatos servicioSync = /* tu instancia existente */;

// 2. Crear el GestorUsuarios
GestorUsuarios gestorUsuarios = new GestorUsuarios();

// 3. Registrar el observador de sincronización
ObservadorSincronizacionUsuarios observador = 
    new ObservadorSincronizacionUsuarios(servicioSync);
gestorUsuarios.registrarObservador(observador);

// 4. Crear el Servicio
ServicioGestionUsuarios servicioUsuarios = 
    new ServicioGestionUsuarios(gestorUsuarios);

// 5. Crear el Controlador
ControladorUsuarios controladorUsuarios = 
    new ControladorUsuarios(servicioUsuarios);

// 6. Crear la vista (panel)
PanelUsuarios panelUsuarios = new PanelUsuarios(controladorUsuarios);

// 7. Agregar el panel a tu interfaz gráfica
tuFrame.add(panelUsuarios);
```

### Paso 3: Configurar dependencias en pom.xml

Si tu módulo Main necesita GestorUsuarios, agrega:

```xml
<dependency>
    <groupId>org.example</groupId>
    <artifactId>GestorUsuarios</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

## Eventos de Sincronización

El GestorUsuarios emite los siguientes eventos que activan la sincronización:

- **USUARIO_CREADO**: Cuando se crea un nuevo usuario
- **USUARIO_ACTUALIZADO**: Cuando se actualiza un usuario existente
- **USUARIO_ELIMINADO**: Cuando se elimina (desactiva) un usuario

Estos eventos son capturados por el `ObservadorSincronizacionUsuarios`, que llama a:

```java
servicioSincronizacionDatos.forzarSincronizacion();
```

Esto fuerza una sincronización inmediata con todos los peers conectados.

## Validaciones Implementadas

### En la creación:
- ✅ Nombre obligatorio
- ✅ Email obligatorio y válido (contiene @ y .)
- ✅ Contraseña obligatoria (mínimo 6 caracteres)
- ✅ Email único (no duplicados)

### En la actualización:
- ✅ ID obligatorio
- ✅ Usuario debe existir
- ✅ Email válido si se modifica
- ✅ Estado válido (ONLINE/OFFLINE)

## Características Implementadas

✅ **Crear usuario** con contraseña
✅ **Actualizar usuario** (nombre, email, estado)
✅ **Eliminar usuario** (cambio a estado OFFLINE)
✅ **Listar usuarios** desde la base de datos
✅ **Sincronización P2P automática** cuando se crea/actualiza/elimina
✅ **Validaciones robustas** en todas las capas
✅ **Manejo de errores** con mensajes al usuario
✅ **Logging completo** para debugging
✅ **Patrón Observador** para notificaciones

## Notas Importantes

### Contraseñas
⚠️ **TODO**: Implementar encriptación de contraseñas antes de producción
- Actualmente se guardan en texto plano
- Usar BCrypt o similar para hash de contraseñas

### PeerPadreId
⚠️ **TODO**: Obtener el ID del peer actual automáticamente
- Actualmente se establece como `null`
- Debe obtenerse del servidor P2P activo

### Estado por defecto
- Los nuevos usuarios se crean con estado `OFFLINE`
- Cambiar a `ONLINE` cuando el usuario se conecte

## Testing

Para probar la integración:

1. **Crear un usuario**:
   - Click en "Add"
   - Llenar formulario
   - Verificar que aparece en la tabla
   - Revisar logs: debe aparecer "Usuario creado" y "Activando sincronización P2P"

2. **Verificar sincronización**:
   - Si hay otro peer conectado, el usuario debe aparecer en ambos
   - Revisar logs de sincronización

3. **Actualizar usuario**:
   - Seleccionar fila
   - Click en "Edit"
   - Modificar datos
   - Verificar actualización en tabla y logs

4. **Eliminar usuario**:
   - Seleccionar fila
   - Click en "Delete"
   - Confirmar
   - Verificar que desaparece de la tabla

## Troubleshooting

### Error: No se puede crear usuario
- Verificar conexión a base de datos
- Revisar logs en `logs/app.log`
- Verificar que el email no esté duplicado

### No se sincroniza con otros peers
- Verificar que hay peers conectados (`repoPeer.listarPeersOnline()`)
- Revisar logs del ServicioSincronizacionDatos
- Verificar que el observador está registrado

### Error de compilación
- Ejecutar `mvn clean install` en el módulo raíz
- Verificar que todas las dependencias estén instaladas
- Revisar versiones de Java (debe ser Java 23)

## Próximos Pasos

1. ✅ Implementar encriptación de contraseñas
2. ✅ Obtener peerPadreId automáticamente
3. ✅ Agregar campo de foto de perfil funcional
4. ✅ Implementar cambio de contraseña
5. ✅ Agregar búsqueda y filtrado en la tabla
6. ✅ Implementar paginación para muchos usuarios

