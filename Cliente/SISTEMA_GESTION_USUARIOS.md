    private JProgressBar progressBar;
    
    public VentanaRegistro() {
        registroUsuario = new RegistroUsuarioImpl();
        registroUsuario.registrarObservador(this);  // ¡Suscribirse!
        
        // Configurar UI...
    }
    
    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        SwingUtilities.invokeLater(() -> {
            switch (tipoDeDato) {
                case "REGISTRO_INICIADO":
                    btnRegistrar.setEnabled(false);
                    progressBar.setVisible(true);
                    break;
                    
                case "REGISTRO_EXITOSO":
                    Usuario usuario = (Usuario) datos;
                    JOptionPane.showMessageDialog(this, 
                        "¡Registro exitoso!\nBienvenido " + usuario.getNombre());
                    abrirVentanaLogin();
                    dispose();
                    break;
                    
                case "REGISTRO_ERROR":
                    String error = (String) datos;
                    btnRegistrar.setEnabled(true);
                    progressBar.setVisible(false);
                    JOptionPane.showMessageDialog(this, 
                        error, "Error", JOptionPane.ERROR_MESSAGE);
                    break;
            }
        });
    }
    
    private void btnRegistrarClick() {
        // 1. Subir foto primero
        File foto = seleccionarFoto();
        gestionArchivos.subirArchivoParaRegistro(foto)
            .thenAccept(photoId -> {
                // 2. Crear DTO con photoId del servidor
                DTORegistro dto = new DTORegistro();
                dto.setName(txtNombre.getText());
                dto.setEmail(txtEmail.getText());
                dto.setPassword(new String(txtPassword.getPassword()));
                dto.setPhotoId(photoId);  // Del servidor
                dto.setIp(obtenerIP());
                
                // 3. Registrar
                byte[] fotoBytes = leerFotoBytes(foto);
                registroUsuario.registrar(dto, fotoBytes);
                // El observador maneja el resto
            });
    }
}
```

### Ejemplo 2: Ventana de Login

```java
public class VentanaLogin extends JFrame implements IObservador {
    
    private IAutenticarUsuario autenticarUsuario;
    private JTextField txtEmail;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    
    public VentanaLogin() {
        autenticarUsuario = new AutenticarUsuario();
        autenticarUsuario.registrarObservador(this);
        
        // Configurar UI...
    }
    
    @Override
    public void actualizar(String tipoDeDato, Object datos) {
        SwingUtilities.invokeLater(() -> {
            switch (tipoDeDato) {
                case "AUTENTICACION_INICIADA":
                    btnLogin.setEnabled(false);
                    btnLogin.setText("Autenticando...");
                    break;
                    
                case "USUARIO_LOGUEADO":
                    Usuario usuario = (Usuario) datos;
                    System.out.println("Usuario logueado: " + usuario.getNombre());
                    System.out.println("Guardado en BD local: " + usuario.getIdUsuario());
                    abrirDashboard(usuario);
                    dispose();
                    break;
                    
                case "AUTENTICACION_ERROR":
                    String error = (String) datos;
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Iniciar Sesión");
                    txtPassword.setText("");
                    JOptionPane.showMessageDialog(this, 
                        error, "Error", JOptionPane.ERROR_MESSAGE);
                    break;
                    
                case "USUARIO_BANEADO":
                    String razon = (String) datos;
                    btnLogin.setEnabled(false);
                    JOptionPane.showMessageDialog(this,
                        "Cuenta suspendida.\n" + razon,
                        "Acceso Denegado",
                        JOptionPane.WARNING_MESSAGE);
                    break;
            }
        });
    }
    
    private void btnLoginClick() {
        String email = txtEmail.getText();
        String password = new String(txtPassword.getPassword());
        
        DTOAutenticacion dto = new DTOAutenticacion(email, password);
        autenticarUsuario.autenticar(dto);
        // El observador maneja todo automáticamente:
        // 1. Valida con servidor
        // 2. Recibe datos del usuario
        // 3. Guarda en BD local
        // 4. Guarda en sesión
        // 5. Notifica a la UI
    }
}
```

---

## 📊 FLUJO COMPLETO: REGISTRO → LOGIN

### 1. Usuario se Registra

```
Cliente:
1. Usuario llena formulario
2. Cliente sube foto → photoId
3. Cliente envía registerUser con photoId
   ↓
Servidor:
4. Genera userId (UUID)
5. Hashea password
6. Guarda en BD del servidor
7. Responde: userId, fechaRegistro, photoId
   ↓
Cliente:
8. Recibe respuesta del servidor
9. Crea entidad Usuario con datos del servidor
10. Guarda en BD local H2
    INSERT INTO usuarios VALUES (
      userId,     ← Del servidor
      nombre,
      email,
      'activo',
      fotoBytes,
      photoId,    ← Del servidor
      fechaRegistro ← Del servidor
    )
11. Notifica observador: "REGISTRO_EXITOSO"
    ↓
UI:
12. Cierra ventana de registro
13. Abre ventana de login (opcional: auto-login)
```

### 2. Usuario hace Login

```
Cliente:
1. Usuario ingresa email y password
2. Cliente envía authenticateUser
   ↓
Servidor:
3. Valida credenciales
4. Verifica estado != 'baneado'
5. Responde con datos completos del usuario
   ↓
Cliente:
6. Recibe respuesta con userId del servidor
7. Busca en BD local: SELECT * FROM usuarios WHERE id_usuario = ?
8. Si existe:
     UPDATE usuarios SET nombre=?, estado=?, photoId=?...
   Si no existe:
     INSERT INTO usuarios VALUES (...)
9. Guarda en GestorSesionUsuario
10. Notifica observador: "USUARIO_LOGUEADO"
    ↓
UI:
11. Cierra ventana de login
12. Abre dashboard con datos del usuario
13. Carga foto de perfil (desde BD o descarga)
14. Muestra información del usuario logueado
```

---

## 🗄️ ESPECIALISTA DE USUARIOS

### Acceso al Usuario Logueado

```java
// Desde cualquier parte de la aplicación:
IEspecialistaUsuarios especialista = new EspecialistaUsuariosImpl();

// Obtener usuario logueado desde sesión
String userId = GestorSesionUsuario.getInstancia().getUserId();
UUID userUUID = UUID.fromString(userId);

// Obtener desde BD local
Usuario usuarioActual = especialista.obtenerUsuarioPorId(userUUID);

System.out.println("Usuario actual: " + usuarioActual.getNombre());
System.out.println("Email: " + usuarioActual.getEmail());
System.out.println("Estado: " + usuarioActual.getEstado());
System.out.println("Photo ID: " + usuarioActual.getPhotoIdServidor());
```

### Operaciones Disponibles

```java
// Obtener usuario por ID
Usuario usuario = especialista.obtenerUsuarioPorId(uuid);

// Obtener usuario por email
Usuario usuario = especialista.obtenerUsuarioPorEmail("email@example.com");

// Actualizar usuario
usuario.setNombre("Nuevo Nombre");
especialista.actualizarUsuario(usuario);

// Actualizar solo el estado
especialista.actualizarEstadoUsuario(uuid, "inactivo");

// Verificar si email existe
boolean existe = especialista.existeUsuarioPorEmail("email@example.com");

// Obtener todos los usuarios
List<Usuario> usuarios = especialista.obtenerTodosUsuarios();
```

---

## 📁 ESTRUCTURA DE ARCHIVOS

### Creados/Actualizados:

#### Interfaces:
- ✅ `IAutenticarUsuario` - Extiende `ISujeto`
- ✅ `IRegistroUsuario` - Extiende `ISujeto`
- ✅ `IEspecialistaUsuarios` - Métodos de BD

#### Implementaciones:
- ✅ `AutenticarUsuario` - Con Observador y persistencia
- ✅ `RegistroUsuarioImpl` - Con Observador y persistencia
- ✅ `EspecialistaUsuariosImpl` - CRUD completo

#### Documentación:
- ✅ `PROTOCOLO_JSON_USUARIOS.md` - Protocolo completo
- ✅ `EjemploObservadorGestionUsuario.java` - Ejemplos de uso
- ✅ Este documento de resumen

---

## 🔐 SEGURIDAD Y VALIDACIONES

### Validaciones del Especialista:

```java
// Email obligatorio y único
if (usuario.getEmail() == null || usuario.getEmail().trim().isEmpty()) {
    throw new IllegalArgumentException("El email es obligatorio");
}
if (repositorioUsuario.existePorEmail(usuario.getEmail())) {
    throw new IllegalStateException("Email ya registrado");
}

// Nombre obligatorio
if (usuario.getNombre() == null || usuario.getNombre().trim().isEmpty()) {
    throw new IllegalArgumentException("El nombre es obligatorio");
}

// Estado válido
if (!estado.equals("activo") && !estado.equals("inactivo") && !estado.equals("baneado")) {
    throw new IllegalArgumentException("Estado inválido");
}
```

---

## ✨ VENTAJAS DE ESTA IMPLEMENTACIÓN

### 1. **Persistencia Automática**
- ✅ Usuario guardado en BD local al registrar
- ✅ Usuario actualizado en BD local al autenticar
- ✅ Sincronización automática con servidor

### 2. **Patrón Observador**
- ✅ UI se actualiza automáticamente
- ✅ Múltiples componentes pueden escuchar
- ✅ Desacoplamiento total entre capas

### 3. **IDs del Servidor**
- ✅ Sin colisiones
- ✅ Servidor tiene control total
- ✅ Sincronización confiable

### 4. **Sesión Global**
- ✅ Usuario accesible desde cualquier parte
- ✅ Datos siempre actualizados
- ✅ Fácil verificación de autenticación

### 5. **Validaciones de Negocio**
- ✅ Email único
- ✅ Estados válidos
- ✅ Datos obligatorios

---

## 🚀 PRÓXIMOS PASOS

### Para Producción:

1. **Seguridad**:
   - Hashear passwords antes de enviar
   - Implementar tokens JWT
   - HTTPS obligatorio

2. **Fotos de Perfil**:
   - Descargar foto al autenticar si photoId cambió
   - Cache de fotos en BD local (tabla archivos)
   - Lazy loading de fotos

3. **Sincronización**:
   - Sincronizar datos al login
   - Detectar cambios en servidor
   - Resolver conflictos

4. **Offline**:
   - Permitir ver perfil sin conexión
   - Cola de cambios pendientes
   - Sincronizar al reconectar

---

## 📝 EJEMPLO COMPLETO DE USO

```java
// ========== EN TU VENTANA DE LOGIN ==========
public class VentanaLogin extends JFrame implements IObservador {
    
    private IAutenticarUsuario autenticar;
    
    public VentanaLogin() {
        // 1. Crear servicio
        autenticar = new AutenticarUsuario();
        
        // 2. Suscribirse
        autenticar.registrarObservador(this);
    }
    
    @Override
    public void actualizar(String tipo, Object datos) {
        SwingUtilities.invokeLater(() -> {
            if (tipo.equals("USUARIO_LOGUEADO")) {
                Usuario usuario = (Usuario) datos;
                // Usuario YA está en BD local
                // Usuario YA está en sesión
                abrirDashboard(usuario);
            }
        });
    }
    
    private void login() {
        DTOAutenticacion dto = new DTOAutenticacion(email, password);
        autenticar.autenticar(dto);
        // ¡Eso es todo! El resto es automático
    }
}

// ========== EN CUALQUIER OTRA PARTE ==========
public class PanelPerfil extends JPanel {
    
    public void cargarDatosUsuario() {
        // Obtener usuario logueado
        String userId = GestorSesionUsuario.getInstancia().getUserId();
        
        // Obtener desde BD local
        IEspecialistaUsuarios especialista = new EspecialistaUsuariosImpl();
        Usuario usuario = especialista.obtenerUsuarioPorId(
            UUID.fromString(userId)
        );
        
        // Mostrar en UI
        lblNombre.setText(usuario.getNombre());
        lblEmail.setText(usuario.getEmail());
        lblEstado.setText(usuario.getEstado());
        
        // Cargar foto de perfil
        if (usuario.getFoto() != null) {
            mostrarFoto(usuario.getFoto());
        } else if (usuario.getPhotoIdServidor() != null) {
            descargarYMostrarFoto(usuario.getPhotoIdServidor());
        }
    }
}
```

---

## ✅ SISTEMA COMPLETAMENTE FUNCIONAL

El sistema está listo para:
- ✅ Registrar usuarios con foto de perfil
- ✅ Autenticar usuarios
- ✅ Guardar automáticamente en BD local
- ✅ Mantener sesión del usuario logueado
- ✅ Notificar a la UI en tiempo real
- ✅ Usar IDs del servidor (nunca generar localmente)
- ✅ Validar datos de negocio
- ✅ Sincronizar con servidor al login

**¡TODO IMPLEMENTADO Y DOCUMENTADO!** 🎉
# ✅ SISTEMA COMPLETO DE GESTIÓN DE USUARIOS - IMPLEMENTADO

## 📋 Resumen de la Implementación

Se ha implementado un sistema completo de gestión de usuarios que incluye:
1. ✅ **Protocolo JSON completo** documentado para registro y autenticación
2. ✅ **Patrón Observador** integrado en Registro y Autenticación
3. ✅ **Persistencia en BD local** automática
4. ✅ **Gestión de sesión** con usuario logueado
5. ✅ **IDs del servidor** - El cliente NUNCA genera IDs de usuario

---

## 📄 PROTOCOLO JSON DOCUMENTADO

### Archivo creado: `PROTOCOLO_JSON_USUARIOS.md`

Contiene la documentación completa de:

#### 📝 REGISTRO (registerUser)
**Petición:**
```json
{
  "action": "registerUser",
  "data": {
    "name": "Juan Pérez",
    "email": "juan.perez@example.com",
    "password": "SecurePass123!",
    "ip": "192.168.1.100",
    "photoId": "file-abc123-foto-perfil.jpg"
  }
}
```

**Respuesta del Servidor:**
```json
{
  "success": true,
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "fechaRegistro": "2025-10-16T14:30:00.000Z",
    "photoId": "file-abc123-foto-perfil.jpg"
  }
}
```

#### 🔐 AUTENTICACIÓN (authenticateUser)
**Petición:**
```json
{
  "action": "authenticateUser",
  "data": {
    "email": "juan.perez@example.com",
    "password": "SecurePass123!"
  }
}
```

**Respuesta del Servidor:**
```json
{
  "success": true,
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "nombre": "Juan Pérez",
    "email": "juan.perez@example.com",
    "photoId": "file-abc123-foto-perfil.jpg",
    "estado": "activo",
    "fechaRegistro": "2025-10-16T14:30:00.000Z"
  }
}
```

---

## 🔔 PATRÓN OBSERVADOR IMPLEMENTADO

### Eventos de Registro:

| Evento | Datos | Cuándo se Dispara |
|--------|-------|-------------------|
| `REGISTRO_INICIADO` | `DTORegistro` | Al comenzar proceso de registro |
| `REGISTRO_EXITOSO` | `Usuario` | Usuario registrado y guardado en BD |
| `REGISTRO_ERROR` | `String mensaje` | Error en el proceso |

### Eventos de Autenticación:

| Evento | Datos | Cuándo se Dispara |
|--------|-------|-------------------|
| `AUTENTICACION_INICIADA` | `String email` | Al comenzar login |
| `AUTENTICACION_EXITOSA` | `Usuario` | Credenciales válidas, datos recibidos |
| `USUARIO_LOGUEADO` | `Usuario` | Usuario guardado en sesión y BD |
| `AUTENTICACION_ERROR` | `String mensaje` | Credenciales incorrectas |
| `USUARIO_BANEADO` | `String razon` | Usuario no puede acceder |

---

## 💾 PERSISTENCIA EN BD LOCAL

### Lo que se guarda automáticamente:

#### Al Registrar:
```java
Usuario usuario = new Usuario();
usuario.setIdUsuario(userId);           // Del servidor
usuario.setNombre(nombre);
usuario.setEmail(email);
usuario.setEstado("activo");
usuario.setFoto(fotoBytes);
usuario.setPhotoIdServidor(photoId);    // Del servidor
usuario.setIp(ip);
usuario.setFechaRegistro(fecha);        // Del servidor

especialistaUsuarios.guardarUsuario(usuario);
// → INSERT en tabla usuarios
```

#### Al Autenticar:
```java
// 1. Buscar usuario en BD local
Usuario usuarioExistente = especialistaUsuarios.obtenerUsuarioPorId(userId);

if (usuarioExistente != null) {
    // Usuario existe → ACTUALIZAR
    usuarioExistente.setNombre(nombre);
    usuarioExistente.setEstado(estado);
    usuarioExistente.setPhotoIdServidor(photoId);
    especialistaUsuarios.actualizarUsuario(usuarioExistente);
} else {
    // Usuario nuevo → INSERTAR
    Usuario nuevoUsuario = new Usuario();
    // ... setear datos del servidor
    especialistaUsuarios.guardarUsuario(nuevoUsuario);
}

// 2. Guardar en sesión
GestorSesionUsuario.getInstancia().setUserId(userId);
GestorSesionUsuario.getInstancia().setUsuarioLogueado(usuario);
```

---

## 🔑 IDs DEL SERVIDOR

### El servidor GENERA:
- ✅ `userId` (UUID del usuario)
- ✅ `photoId` (ID del archivo de foto)
- ✅ `fechaRegistro` (Timestamp)

### El cliente RECIBE y ALMACENA:
- ✅ Guarda `userId` en BD local como PRIMARY KEY
- ✅ Guarda `photoId` para descargar foto después
- ✅ Usa estos IDs para todas las operaciones

### El cliente NUNCA GENERA:
- ❌ UUIDs de usuario
- ❌ Timestamps del servidor
- ❌ IDs de archivos

---

## 🎯 CÓMO USAR EN LA UI

### Ejemplo 1: Ventana de Registro

```java
public class VentanaRegistro extends JFrame implements IObservador {
    
    private IRegistroUsuario registroUsuario;
    private JButton btnRegistrar;

