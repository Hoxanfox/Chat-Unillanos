# Base de Datos H2 Embebida - Sistema de Usuarios Local

## 📋 Resumen

Se ha implementado una **base de datos H2 embebida** en el proyecto para almacenar información de usuarios localmente. La base de datos se crea automáticamente al iniciar la aplicación y se guarda en el directorio `./data/`.

---

## 🗄️ Configuración de la Base de Datos

### Ubicación del Archivo
```
./data/chat_unillanos.mv.db
```

### Configuración de Conexión
```java
DB_URL = "jdbc:h2:./data/chat_unillanos;AUTO_SERVER=TRUE"
DB_USER = "sa"
DB_PASSWORD = ""
```

### Esquema de la Tabla `usuarios`
```sql
CREATE TABLE usuarios (
    user_id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    foto BLOB,
    photo_id VARCHAR(255),
    ip VARCHAR(50),
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)
```

---

## 🏗️ Arquitectura Implementada

### Flujo Completo por Capas

```
┌─────────────────────────────────────────────────────────────────┐
│                       CONTROLADOR                                │
│  ControladorUsuario                                             │
│  - cargarInformacionUsuario(userId)                             │
│  - actualizarInformacionUsuario(dtoUsuario)                     │
│  - obtenerUsuarioActual()                                       │
└────────────────────────┬────────────────────────────────────────┘
                         │ DTOUsuario
                         ↓
┌─────────────────────────────────────────────────────────────────┐
│                       SERVICIO                                   │
│  ServicioUsuarioImpl                                            │
│  - obtenerInformacionUsuario(userId): CompletableFuture         │
│  - actualizarInformacionUsuario(dtoUsuario): CompletableFuture  │
│  - guardarUsuario(dtoUsuario): CompletableFuture                │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ↓
┌─────────────────────────────────────────────────────────────────┐
│                       FACHADA                                    │
│  FachadaUsuariosImpl                                            │
│  - obtenerUsuarioPorId(userId): CompletableFuture               │
│  - obtenerUsuarioPorEmail(email): CompletableFuture             │
│  - guardarUsuario(dtoUsuario): CompletableFuture                │
│  - actualizarUsuario(dtoUsuario): CompletableFuture             │
└────────────────────────┬────────────────────────────────────────┘
                         │ Conversión DTOUsuario ↔ DTOUsuarioRepositorio
                         ↓
┌─────────────────────────────────────────────────────────────────┐
│                     REPOSITORIO                                  │
│  RepositorioUsuarioImpl                                         │
│  - guardarUsuario(dtoRepo)                                      │
│  - obtenerUsuarioPorId(userId): DTOUsuarioRepositorio           │
│  - obtenerUsuarioPorEmail(email): DTOUsuarioRepositorio         │
│  - actualizarUsuario(dtoRepo)                                   │
│  - eliminarUsuario(userId)                                      │
│  - existeUsuarioPorEmail(email): boolean                        │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ↓
┌─────────────────────────────────────────────────────────────────┐
│                    GESTOR CONEXIÓN H2                            │
│  GestorConexionH2 (Singleton)                                   │
│  - getConexion(): Connection                                    │
│  - crearTablas()                                                │
│  - cerrarConexion()                                             │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ↓
                    Base de Datos H2
```

---

## 📦 Componentes Implementados

### 1. GestorConexionH2 (Singleton)
```
Ubicación: Persistencia/Repositorio/src/main/java/repositorio/conexion/
Responsabilidad: Gestionar la conexión única a la BD H2
```

**Características:**
- ✅ Patrón Singleton (una sola instancia)
- ✅ Crea la base de datos automáticamente
- ✅ Crea las tablas si no existen
- ✅ Reconexión automática si se cierra la conexión
- ✅ Archivo embebido en `./data/`

### 2. IRepositorioUsuario + RepositorioUsuarioImpl
```
Ubicación: Persistencia/Repositorio/src/main/java/repositorio/usuario/
Responsabilidad: Operaciones CRUD sobre la tabla usuarios
```

**Operaciones:**
- `guardarUsuario()` - INSERT
- `obtenerUsuarioPorId()` - SELECT por ID
- `obtenerUsuarioPorEmail()` - SELECT por email
- `actualizarUsuario()` - UPDATE
- `eliminarUsuario()` - DELETE
- `existeUsuarioPorEmail()` - Verificación de existencia

### 3. IFachadaUsuarios + FachadaUsuariosImpl
```
Ubicación: Negocio/Fachada/src/main/java/fachada/gestionUsuarios/
Responsabilidad: Coordinar operaciones asíncronas y conversión de DTOs
```

**Conversiones:**
- `DTOUsuario` (vista) ↔ `DTOUsuarioRepositorio` (persistencia)

### 4. IServicioUsuario + ServicioUsuarioImpl
```
Ubicación: Negocio/Servicio/src/main/java/servicio/usuario/
Responsabilidad: Lógica de negocio relacionada con usuarios
```

### 5. IControladorUsuario + ControladorUsuario
```
Ubicación: Presentacion/Controlador/src/main/java/controlador/usuario/
Responsabilidad: Coordinar operaciones desde la vista
```

**Caché en memoria:**
```java
private DTOUsuario usuarioActual;
```

---

## 🔄 Flujo de Uso: Login Exitoso

```
1. Usuario ingresa credenciales
   ↓
2. ControladorAutenticacion.autenticar() → Servidor valida
   ↓
3. Servidor retorna DTOUsuario básico (id, nombre, email, avatarUrl)
   ↓
4. VistaLogin → onLoginExitoso.accept(dtoUsuario)
   ↓
5. VistaAutenticacion → Crea VistaLobby(dtoUsuario)
   ↓
6. VistaLobby → controladorUsuario.cargarInformacionUsuario(userId)
   ↓
7. ServicioUsuario → FachadaUsuarios → RepositorioUsuario
   ↓
8. SELECT * FROM usuarios WHERE user_id = ?
   ↓
9. Si existe en BD local → Retorna información completa
   ↓
10. Si NO existe → Retorna null (se usará el DTOUsuario del servidor)
   ↓
11. VistaLobby actualiza la interfaz con la información completa
```

---

## 💾 Persistencia de Usuarios

### Cuándo se Guarda un Usuario en la BD Local

1. **Después del Registro Exitoso:**
```java
// En el flujo de registro
controladorUsuario.guardarUsuario(dtoUsuario).thenRun(() -> {
    System.out.println("Usuario guardado en BD local");
});
```

2. **Después del Login Exitoso (Primera vez):**
```java
// Si el usuario no existe en la BD local
if (usuarioLocal == null) {
    controladorUsuario.guardarUsuario(dtoUsuarioServidor);
}
```

3. **Cuando se Actualiza el Perfil:**
```java
controladorUsuario.actualizarInformacionUsuario(dtoUsuarioActualizado);
```

---

## 🎯 DTOs Utilizados

### DTOUsuario (Vista - Lobby)
```java
package dto.vistaLobby;

public class DTOUsuario {
    private final String id;
    private final String nombre;
    private final String email;
    private final String avatarUrl;
}
```
**Uso:** Transporte de datos entre Controlador ↔ Vista

### DTOUsuarioRepositorio (Persistencia)
```java
package dto.repositorio;

public class DTOUsuarioRepositorio {
    private final String userId;
    private final String name;
    private final String email;
    private final String password;
    private final byte[] fotoBytes;
    private final String photoId;
    private final String ip;
    private final LocalDateTime fechaRegistro;
}
```
**Uso:** Transporte de datos entre Repositorio ↔ Dominio

---

## 🔧 Inicialización Automática

La base de datos se inicializa automáticamente cuando se crea la primera instancia de `GestorConexionH2`:

```java
// Primera llamada al repositorio
IRepositorioUsuario repositorio = new RepositorioUsuarioImpl();
// ↓ Esto ejecuta:
GestorConexionH2 gestor = GestorConexionH2.getInstancia();
// ↓ Que a su vez:
// 1. Carga el driver H2
// 2. Crea la conexión
// 3. Crea la tabla 'usuarios' si no existe
```

**No se necesita inicialización manual en Main.java**

---

## 📂 Estructura de Archivos Generados

```
Cliente/
├── data/
│   ├── chat_unillanos.mv.db        ← Base de datos H2
│   └── chat_unillanos.trace.db     ← Logs de H2 (si hay errores)
```

**Estos archivos se crean automáticamente al ejecutar la aplicación.**

---

## 🛠️ Operaciones de Ejemplo

### Guardar Usuario
```java
DTOUsuario usuario = new DTOUsuario("123", "Juan", "juan@mail.com", "avatar.png");
controladorUsuario.guardarUsuario(usuario)
    .thenRun(() -> System.out.println("Usuario guardado"))
    .exceptionally(ex -> {
        System.err.println("Error: " + ex.getMessage());
        return null;
    });
```

### Cargar Usuario
```java
controladorUsuario.cargarInformacionUsuario("123")
    .thenAccept(usuario -> {
        if (usuario != null) {
            System.out.println("Usuario: " + usuario.getNombre());
        }
    });
```

### Actualizar Usuario
```java
DTOUsuario usuarioActualizado = new DTOUsuario("123", "Juan Pérez", "juan@mail.com", "nuevo_avatar.png");
controladorUsuario.actualizarInformacionUsuario(usuarioActualizado);
```

---

## ✅ Ventajas de H2 Embebida

1. **Sin Configuración Externa:**
   - No requiere instalar un servidor de BD
   - Se distribuye como archivo `.jar`

2. **Portabilidad:**
   - La BD viaja con la aplicación
   - Funciona en cualquier sistema con Java

3. **Rendimiento:**
   - Acceso directo al archivo
   - Sin latencia de red

4. **Modo AUTO_SERVER:**
   - Permite múltiples conexiones si es necesario
   - Útil para debugging

5. **Compatible con SQL Estándar:**
   - Fácil migración a otras BDs si es necesario

---

## 🔍 Verificación de Errores

Para verificar que la BD funciona correctamente, revisa los logs:

```
✅ [GestorConexionH2]: Conexión establecida con la base de datos.
✅ [GestorConexionH2]: Tabla 'usuarios' verificada/creada.
✅ [RepositorioUsuario]: Usuario guardado exitosamente en H2.
```

---

## 🚀 Próximos Pasos

1. **Sincronización con Servidor:**
   - Guardar usuarios automáticamente después del login
   - Actualizar información periódicamente

2. **Caché Offline:**
   - Permitir login offline si hay usuario en BD local
   - Sincronizar cuando se recupere la conexión

3. **Tablas Adicionales:**
   - Mensajes (para caché de conversaciones)
   - Contactos (para acceso rápido)
   - Configuración local del usuario

---

## 📝 Resumen

✅ Base de datos H2 embebida configurada
✅ Repositorio con operaciones CRUD completas
✅ Arquitectura por capas respetada
✅ DTOs para mover datos entre capas
✅ Operaciones asíncronas con CompletableFuture
✅ Singleton para gestión de conexión
✅ Inicialización automática de tablas
✅ Sin configuración manual requerida

La base de datos está lista para usar. Solo necesitas llamar al `ControladorUsuario` desde tu `VistaLobby` después de un login exitoso. 🎯

