# Documentación: Flujo de Gestión de Canales

## Índice
1. [Visión General](#visión-general)
2. [Arquitectura del Componente](#arquitectura-del-componente)
3. [Flujo de Creación de Canal](#flujo-de-creación-de-canal)
4. [Componentes Involucrados](#componentes-involucrados)
5. [Flujo Detallado Paso a Paso](#flujo-detallado-paso-a-paso)
6. [Diagrama de Secuencia](#diagrama-de-secuencia)
7. [Manejo de Errores](#manejo-de-errores)
8. [Ejemplos de Uso](#ejemplos-de-uso)

---

## Visión General

El componente **GestionCanales** implementa la lógica de negocio para la gestión de canales en el sistema de chat. Su responsabilidad principal es **orquestar** la comunicación entre el cliente y el servidor, y sincronizar los datos locales con el servidor remoto.

### Características Principales:
- ✅ **Asincronía**: Todas las operaciones son asíncronas usando `CompletableFuture`
- ✅ **Patrón Observer**: Usa callbacks para procesar respuestas del servidor
- ✅ **Persistencia Dual**: Guarda datos tanto en el servidor como localmente
- ✅ **Desacoplamiento**: Usa interfaces y DTOs para la comunicación

---

## Arquitectura del Componente

```
┌─────────────────────────────────────────────────────────────┐
│                    PRESENTACIÓN (UI)                        │
│              (Controlador/InterfazEscritorio)               │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                   NEGOCIO (Lógica)                          │
│  ┌──────────────────────────────────────────────────────┐   │
│  │         GestionCanales.CreadorCanal                  │   │
│  │  - Orquesta el flujo de creación                     │   │
│  │  - Coordina comunicación y persistencia              │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────┬────────────────────────────────┬──────────────┘
              │                                │
              ▼                                ▼
┌─────────────────────────┐      ┌────────────────────────────┐
│   PERSISTENCIA/         │      │   PERSISTENCIA/            │
│   Comunicacion          │      │   Repositorio              │
│                         │      │                            │
│  ┌──────────────────┐   │      │  ┌──────────────────────┐  │
│  │EnviadorPeticiones│   │      │  │RepositorioCanalImpl  │  │
│  │  - Envía DTOs    │   │      │  │  - Guarda en H2      │  │
│  │  - Serializa JSON│   │      │  │  - CRUD local        │  │
│  └──────────────────┘   │      │  └──────────────────────┘  │
│                         │      │                            │
│  ┌──────────────────┐   │      │  ┌──────────────────────┐  │
│  │ GestorRespuesta  │   │      │  │  GestorConexionH2    │  │
│  │  - Escucha hilo  │   │      │  │  - Pool conexiones   │  │
│  │  - Parsea JSON   │   │      │  │  - Singleton         │  │
│  │  - Invoca callbacks  │      │  └──────────────────────┘  │
│  └──────────────────┘   │      │                            │
└─────────────────────────┘      └────────────────────────────┘
              │                                │
              ▼                                ▼
┌─────────────────────────┐      ┌────────────────────────────┐
│   SERVIDOR (TCP/IP)     │      │   BASE DE DATOS H2         │
│   - Procesa peticiones  │      │   - Almacenamiento local   │
│   - Devuelve respuestas │      │   - Tablas SQL             │
└─────────────────────────┘      └────────────────────────────┘
```

---

## Flujo de Creación de Canal

### 1. **Punto de Entrada**: `CreadorCanal.crearCanal(String nombre, String descripcion)`

El método principal que inicia todo el proceso:

```java
public CompletableFuture<Canal> crearCanal(String nombre, String descripcion)
```

**Entradas:**
- `nombre`: Nombre del canal a crear
- `descripcion`: Descripción opcional del canal

**Salida:**
- `CompletableFuture<Canal>`: Promesa que se completará con el objeto Canal del dominio

---

## Componentes Involucrados

### 1. **CreadorCanal** (Negocio/GestionCanales)
**Responsabilidad:** Orquestador principal del flujo de creación

**Dependencias:**
```java
private final IRepositorioCanal repositorioCanal;        // Para persistencia local
private final GestorSesionUsuario gestorSesion;          // Para obtener userId
private final IEnviadorPeticiones enviadorPeticiones;    // Para enviar al servidor
private final IGestorRespuesta gestorRespuesta;          // Para recibir respuestas
```

### 2. **EnviadorPeticiones** (Persistencia/Comunicacion)
**Responsabilidad:** Enviar peticiones serializadas al servidor

**Funcionamiento:**
```java
public void enviar(DTORequest request) {
    // 1. Obtiene la sesión activa (socket + streams)
    DTOSesion sesion = gestorConexion.getSesion();
    
    // 2. Serializa el request a JSON
    String jsonRequest = gson.toJson(request);
    
    // 3. Envía por el PrintWriter del socket
    PrintWriter out = sesion.getOut();
    out.println(jsonRequest);
}
```

### 3. **GestorRespuesta** (Persistencia/Comunicacion)
**Responsabilidad:** Escuchar respuestas del servidor en un hilo separado

**Funcionamiento:**
```java
// Patrón Singleton
private static GestorRespuesta instancia;

// Mapa de callbacks por tipo de acción
private final Map<String, Consumer<DTOResponse>> manejadores;

// Hilo que escucha continuamente
public void iniciarEscucha() {
    hiloEscucha = new Thread(() -> {
        BufferedReader in = sesion.getIn();
        while ((respuestaServidor = in.readLine()) != null) {
            procesarRespuesta(respuestaServidor);
        }
    });
}

// Invoca el callback correspondiente
private void procesarRespuesta(String jsonResponse) {
    DTOResponse response = gson.fromJson(jsonResponse, DTOResponse.class);
    Consumer<DTOResponse> manejador = manejadores.get(response.getAction());
    if (manejador != null) {
        manejador.accept(response);
    }
}
```

### 4. **RepositorioCanalImpl** (Persistencia/Repositorio)
**Responsabilidad:** Persistir canales en la base de datos H2 local

**Funcionamiento:**
```java
public CompletableFuture<Boolean> guardar(Canal canal) {
    return CompletableFuture.supplyAsync(() -> {
        // 1. Obtiene conexión JDBC de GestorConexionH2
        Connection conn = gestorConexion.getConexion();
        
        // 2. Prepara statement SQL
        String sql = "INSERT INTO canales (id, nombre, id_administrador) VALUES (?, ?, ?)";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        
        // 3. Mapea objeto dominio a SQL
        pstmt.setString(1, canal.getIdCanal().toString());
        pstmt.setString(2, canal.getNombre());
        pstmt.setString(3, canal.getIdAdministrador().toString());
        
        // 4. Ejecuta y retorna resultado
        int filasAfectadas = pstmt.executeUpdate();
        return filasAfectadas > 0;
    });
}
```

### 5. **GestorConexionH2** (Persistencia/Repositorio)
**Responsabilidad:** Gestionar conexión única (Singleton) a la base de datos H2

**Características:**
- Base de datos embebida en `./data/chat_unillanos`
- Usuario: `sa`, sin contraseña
- Modo `AUTO_SERVER=TRUE` para permitir múltiples conexiones
- Crea automáticamente todas las tablas al iniciar

**Método principal:**
```java
public Connection getConexion() {
    // Verifica si la conexión está cerrada y reconecta si es necesario
    if (conexion == null || conexion.isClosed()) {
        conexion = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
    return conexion;
}
```

---

## Flujo Detallado Paso a Paso

### **Fase 1: Preparación y Validación**

```java
public CompletableFuture<Canal> crearCanal(String nombre, String descripcion) {
    // PASO 1: Crear el CompletableFuture que se retornará
    CompletableFuture<Canal> future = new CompletableFuture<>();
    
    // PASO 2: Validar que el usuario esté autenticado
    String creadorId = gestorSesion.getUserId();
    if (creadorId == null) {
        future.completeExceptionally(
            new IllegalStateException("El usuario no ha iniciado sesión.")
        );
        return future;
    }
    
    // Continúa...
}
```

**¿Qué pasa aquí?**
1. Se crea un `CompletableFuture<Canal>` que actuará como "promesa"
2. Se obtiene el ID del usuario de la sesión activa
3. Si no hay usuario logueado, se completa el future con excepción y se retorna inmediatamente

---

### **Fase 2: Preparación de la Petición**

```java
// PASO 3: Crear el DTO de la petición
DTORequest request = new DTORequest(
    "crearCanal",                                    // action
    new DTOCrearCanal(creadorId, nombre, descripcion) // payload
);
```

**Estructura del DTORequest:**
```json
{
  "action": "crearCanal",
  "requestId": "uuid-generado-automaticamente",
  "payload": {
    "creadorId": "uuid-del-usuario",
    "nombre": "Mi Canal",
    "descripcion": "Descripción del canal"
  }
}
```

---

### **Fase 3: Registro del Callback (Patrón Observer)**

```java
// PASO 4: Registrar el manejador que procesará la respuesta
gestorRespuesta.registrarManejador(request.getAction(), (respuesta) -> {
    
    // PASO 5: Verificar el estado de la respuesta
    if (!"success".equals(respuesta.getStatus())) {
        future.completeExceptionally(
            new RuntimeException("Error del servidor: " + respuesta.getMessage())
        );
        return;
    }
    
    try {
        // PASO 6: Extraer los datos de la respuesta
        Map<String, Object> data = (Map<String, Object>) respuesta.getData();
        
        // PASO 7: Construir el objeto del dominio
        Canal canalDeDominio = new Canal(
            UUID.fromString((String) data.get("id")),
            (String) data.get("nombre"),
            UUID.fromString((String) data.get("creadorId"))
        );
        
        // PASO 8: Guardar localmente en la base de datos H2
        repositorioCanal.guardar(canalDeDominio)
            .thenAccept(guardado -> {
                if (guardado) {
                    // PASO 9: Completar el future con éxito
                    future.complete(canalDeDominio);
                } else {
                    future.completeExceptionally(
                        new RuntimeException("Falló al guardarse localmente.")
                    );
                }
            })
            .exceptionally(ex -> {
                future.completeExceptionally(ex);
                return null;
            });
            
    } catch (Exception e) {
        future.completeExceptionally(
            new RuntimeException("Error al procesar la respuesta del servidor.", e)
        );
    }
});
```

**¿Qué pasa aquí?**
1. Se registra un callback (Consumer) en el GestorRespuesta con la clave "crearCanal"
2. Este callback se ejecutará cuando llegue una respuesta del servidor con `action: "crearCanal"`
3. El callback valida el estado, extrae los datos, crea el objeto dominio y lo persiste localmente
4. Finalmente completa el `CompletableFuture` original

---

### **Fase 4: Envío de la Petición**

```java
// PASO 10: Enviar la petición al servidor
enviadorPeticiones.enviar(request);

// PASO 11: Retornar el future inmediatamente (no bloqueante)
return future;
```

**Internamente en EnviadorPeticiones:**
```java
public void enviar(DTORequest request) {
    DTOSesion sesion = gestorConexion.getSesion(); // Socket + streams
    
    if (sesion != null && sesion.estaActiva()) {
        PrintWriter out = sesion.getOut();
        String jsonRequest = gson.toJson(request);
        out.println(jsonRequest);  // Envía por el socket TCP
        System.out.println(">> Petición enviada: " + jsonRequest);
    }
}
```

---

### **Fase 5: Recepción de la Respuesta (Hilo Separado)**

El **GestorRespuesta** está escuchando continuamente en un hilo aparte:

```java
// Hilo de escucha (iniciado al conectar)
hiloEscucha = new Thread(() -> {
    BufferedReader in = sesion.getIn();
    while ((respuestaServidor = in.readLine()) != null) {
        System.out.println("<< Respuesta recibida: " + respuestaServidor);
        procesarRespuesta(respuestaServidor);
    }
});

private void procesarRespuesta(String jsonResponse) {
    // Deserializar JSON a DTOResponse
    DTOResponse response = gson.fromJson(jsonResponse, DTOResponse.class);
    
    // Buscar el callback registrado para esta acción
    Consumer<DTOResponse> manejador = manejadores.get(response.getAction());
    
    // Invocar el callback (el que registramos en Fase 3)
    if (manejador != null) {
        manejador.accept(response);
    }
}
```

**Estructura de DTOResponse esperada:**
```json
{
  "action": "crearCanal",
  "status": "success",
  "message": "Canal creado exitosamente",
  "data": {
    "id": "uuid-del-canal-generado",
    "nombre": "Mi Canal",
    "creadorId": "uuid-del-usuario"
  }
}
```

---

### **Fase 6: Persistencia Local**

Una vez que el callback procesa la respuesta exitosa, se guarda en H2:

```java
repositorioCanal.guardar(canalDeDominio)
    .thenAccept(guardado -> {
        if (guardado) {
            future.complete(canalDeDominio); // ✅ ¡Éxito total!
        }
    });
```

**Internamente en RepositorioCanalImpl:**
```java
public CompletableFuture<Boolean> guardar(Canal canal) {
    return CompletableFuture.supplyAsync(() -> {
        // Obtiene conexión del pool singleton
        Connection conn = gestorConexion.getConexion();
        
        // Inserta en la tabla 'canales'
        String sql = "INSERT INTO canales (id, nombre, id_administrador) VALUES (?, ?, ?)";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        
        pstmt.setString(1, canal.getIdCanal().toString());
        pstmt.setString(2, canal.getNombre());
        pstmt.setString(3, canal.getIdAdministrador().toString());
        
        int filasAfectadas = pstmt.executeUpdate();
        return filasAfectadas > 0;
    });
}
```

**Tabla en H2:**
```sql
CREATE TABLE IF NOT EXISTS canales (
    id_canal UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL UNIQUE,
    id_administrador UUID,
    FOREIGN KEY (id_administrador) REFERENCES usuarios(id_usuario) ON DELETE SET NULL
)
```

---

## Diagrama de Secuencia

```
Usuario          CreadorCanal      EnviadorPeticiones    SERVIDOR       GestorRespuesta    RepositorioCanalImpl    H2
  │                   │                     │                │                 │                    │              │
  │ crearCanal()      │                     │                │                 │                    │              │
  ├──────────────────>│                     │                │                 │                    │              │
  │                   │                     │                │                 │                    │              │
  │                   │ registrarManejador()│                │                 │                    │              │
  │                   ├─────────────────────┼────────────────┼────────────────>│                    │              │
  │                   │                     │                │                 │ [Registra callback]│              │
  │                   │                     │                │                 │                    │              │
  │                   │ enviar(DTORequest)  │                │                 │                    │              │
  │                   ├────────────────────>│                │                 │                    │              │
  │                   │                     │  JSON/TCP      │                 │                    │              │
  │                   │                     ├───────────────>│                 │                    │              │
  │<─ CompletableFuture                    │                │                 │                    │              │
  │   (no bloqueante) │                     │                │                 │                    │              │
  │                   │                     │                │                 │                    │              │
  │                   │                     │                │ [Procesa]       │                    │              │
  │                   │                     │                │                 │                    │              │
  │                   │                     │  JSON/TCP      │                 │                    │              │
  │                   │                     │                ├────────────────>│                    │              │
  │                   │                     │                │                 │ [Hilo de escucha]  │              │
  │                   │                     │                │                 │                    │              │
  │                   │                     │                │                 │ accept(DTOResponse)│              │
  │                   │<────────────────────┼────────────────┼─────────────────┤                    │              │
  │                   │ [Ejecuta callback]  │                │                 │                    │              │
  │                   │                     │                │                 │                    │              │
  │                   │ guardar(Canal)      │                │                 │                    │              │
  │                   ├─────────────────────┼────────────────┼─────────────────┼───────────────────>│              │
  │                   │                     │                │                 │                    │              │
  │                   │                     │                │                 │                    │ INSERT INTO  │
  │                   │                     │                │                 │                    ├─────────────>│
  │                   │                     │                │                 │                    │              │
  │                   │                     │                │                 │                    │ OK (1 fila)  │
  │                   │                     │                │                 │                    │<─────────────┤
  │                   │                     │                │                 │                    │              │
  │                   │ CompletableFuture<Boolean>          │                 │                    │              │
  │                   │<────────────────────┼────────────────┼─────────────────┼────────────────────┤              │
  │                   │                     │                │                 │                    │              │
  │                   │ future.complete(Canal)              │                 │                    │              │
  │                   │                     │                │                 │                    │              │
  │<─ Canal (dominio) │                     │                │                 │                    │              │
  │   ¡Éxito!         │                     │                │                 │                    │              │
```

---

## Manejo de Errores

El sistema implementa manejo de errores en múltiples capas:

### 1. **Validación Inicial**
```java
String creadorId = gestorSesion.getUserId();
if (creadorId == null) {
    future.completeExceptionally(
        new IllegalStateException("El usuario no ha iniciado sesión.")
    );
    return future;
}
```

### 2. **Error del Servidor**
```java
if (!"success".equals(respuesta.getStatus())) {
    future.completeExceptionally(
        new RuntimeException("Error del servidor: " + respuesta.getMessage())
    );
    return;
}
```

### 3. **Error de Persistencia Local**
```java
repositorioCanal.guardar(canalDeDominio)
    .thenAccept(guardado -> {
        if (!guardado) {
            future.completeExceptionally(
                new RuntimeException("El canal se creó en el servidor, pero falló al guardarse localmente.")
            );
        }
    })
    .exceptionally(ex -> {
        future.completeExceptionally(ex);
        return null;
    });
```

### 4. **Error de Parsing**
```java
try {
    Map<String, Object> data = (Map<String, Object>) respuesta.getData();
    Canal canalDeDominio = new Canal(...);
} catch (Exception e) {
    future.completeExceptionally(
        new RuntimeException("Error al procesar la respuesta del servidor.", e)
    );
}
```

### 5. **Error de Comunicación**
```java
// En EnviadorPeticiones
try {
    out.println(jsonRequest);
} catch (Exception e) {
    if (sesion.getSocket().isClosed()) {
        System.err.println("Error: la conexión parece estar cerrada.");
    } else {
        System.err.println("Error al enviar la petición: " + e.getMessage());
    }
}
```

### 6. **Error de Base de Datos**
```java
// En RepositorioCanalImpl
try (Connection conn = gestorConexion.getConexion();
     PreparedStatement pstmt = conn.prepareStatement(sql)) {
    // ...
} catch (SQLException e) {
    System.err.println("Error al guardar el canal: " + e.getMessage());
    throw new RuntimeException("Fallo en la operación de base de datos al guardar", e);
}
```

---

## Ejemplos de Uso

### Ejemplo 1: Uso Básico desde un Controlador

```java
public class ControladorCanales {
    
    private final ICreadorCanal creadorCanal;
    
    public ControladorCanales() {
        IRepositorioCanal repositorioCanal = new RepositorioCanalImpl();
        this.creadorCanal = new CreadorCanal(repositorioCanal);
    }
    
    public void manejarCreacionCanal(String nombre, String descripcion) {
        // Llamada asíncrona
        creadorCanal.crearCanal(nombre, descripcion)
            .thenAccept(canal -> {
                // Éxito: actualizar UI
                System.out.println("✅ Canal creado: " + canal.getNombre());
                System.out.println("   ID: " + canal.getIdCanal());
                actualizarListaCanales(canal);
            })
            .exceptionally(ex -> {
                // Error: mostrar mensaje
                System.err.println("❌ Error al crear canal: " + ex.getMessage());
                mostrarMensajeError(ex.getMessage());
                return null;
            });
            
        // El método retorna inmediatamente (no bloqueante)
        System.out.println("Petición enviada, esperando respuesta...");
    }
}
```

### Ejemplo 2: Uso con Timeout

```java
import java.util.concurrent.TimeUnit;

public void crearCanalConTimeout(String nombre, String descripcion) {
    creadorCanal.crearCanal(nombre, descripcion)
        .orTimeout(10, TimeUnit.SECONDS)  // Timeout de 10 segundos
        .thenAccept(canal -> {
            System.out.println("Canal creado: " + canal.getNombre());
        })
        .exceptionally(ex -> {
            if (ex instanceof TimeoutException) {
                System.err.println("❌ Timeout: El servidor no respondió");
            } else {
                System.err.println("❌ Error: " + ex.getMessage());
            }
            return null;
        });
}
```

### Ejemplo 3: Uso con Loading UI

```java
public void crearCanalConLoading(String nombre, String descripcion) {
    // Mostrar indicador de carga
    mostrarLoading(true);
    
    creadorCanal.crearCanal(nombre, descripcion)
        .whenComplete((canal, ex) -> {
            // Ocultar indicador de carga (éxito o error)
            mostrarLoading(false);
            
            if (ex != null) {
                mostrarError("No se pudo crear el canal: " + ex.getMessage());
            } else {
                mostrarExito("Canal '" + canal.getNombre() + "' creado exitosamente");
                navegarAlCanal(canal);
            }
        });
}
```

### Ejemplo 4: Crear Múltiples Canales

```java
public void crearVariosCanales(List<String> nombres) {
    List<CompletableFuture<Canal>> futures = nombres.stream()
        .map(nombre -> creadorCanal.crearCanal(nombre, ""))
        .collect(Collectors.toList());
    
    // Esperar a que todos se completen
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .thenRun(() -> {
            System.out.println("✅ Todos los canales creados");
            // Obtener los resultados
            List<Canal> canales = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
            
            actualizarListaCanales(canales);
        })
        .exceptionally(ex -> {
            System.err.println("❌ Error en creación masiva: " + ex.getMessage());
            return null;
        });
}
```

---

## Ventajas de Esta Arquitectura

### ✅ **Asincronía No Bloqueante**
- La UI no se congela esperando respuestas del servidor
- Operaciones múltiples pueden ejecutarse en paralelo

### ✅ **Separación de Responsabilidades**
- **GestionCanales**: Lógica de negocio
- **Comunicacion**: Transporte de datos
- **Repositorio**: Persistencia local

### ✅ **Patrón Observer para Respuestas**
- Desacopla el envío de la recepción
- Permite múltiples peticiones simultáneas
- Callbacks se ejecutan automáticamente al recibir respuesta

### ✅ **Persistencia Dual**
- **Servidor**: Fuente de verdad
- **Local (H2)**: Cache para acceso rápido y modo offline

### ✅ **Manejo de Errores Robusto**
- Validación en múltiples capas
- Propagación clara de excepciones
- Mensajes descriptivos

### ✅ **Extensibilidad**
- Fácil agregar nuevas operaciones (eliminar, actualizar, etc.)
- Solo requiere:
  1. Crear nuevo DTO
  2. Implementar método en ICreadorCanal
  3. Registrar callback con gestorRespuesta

---

## Otros Métodos del Repositorio

El `RepositorioCanalImpl` implementa operaciones CRUD completas:

### **Buscar por ID**
```java
CompletableFuture<Canal> buscarPorId(String id)
```
```sql
SELECT id, nombre, id_administrador FROM canales WHERE id = ?
```

### **Obtener Todos**
```java
CompletableFuture<List<Canal>> obtenerTodos()
```
```sql
SELECT id, nombre, id_administrador FROM canales
```

### **Actualizar**
```java
CompletableFuture<Boolean> actualizar(Canal canal)
```
```sql
UPDATE canales SET nombre = ?, id_administrador = ? WHERE id = ?
```

### **Eliminar**
```java
CompletableFuture<Boolean> eliminar(String id)
```
```sql
DELETE FROM canales WHERE id = ?
```

**Todos estos métodos:**
- Son asíncronos (retornan `CompletableFuture`)
- Usan la conexión del `GestorConexionH2`
- Manejan excepciones SQL adecuadamente

---

## Conclusión

El componente **GestionCanales** implementa un **flujo asíncrono completo** que:

1. **Valida** la sesión del usuario
2. **Envía** peticiones serializadas al servidor vía TCP/IP
3. **Registra callbacks** que se ejecutarán al recibir respuestas
4. **Procesa** las respuestas del servidor en un hilo separado
5. **Persiste** los datos localmente en H2
6. **Completa** el `CompletableFuture` para notificar a la UI

Todo esto manteniendo la UI responsiva, manejando errores en múltiples capas, y sincronizando datos entre el servidor y la base de datos local.

---

**Documentación generada para el proyecto Chat-Unillanos**  
**Fecha:** Octubre 2025  
**Autor:** Sistema de Documentación Automática

