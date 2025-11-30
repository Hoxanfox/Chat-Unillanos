# Sistema de Sincronización Global - SIGNAL_UPDATE

## 📋 Descripción General

El cliente ahora cuenta con un **sistema de sincronización automática** que responde a señales enviadas por el servidor. Cuando el servidor envía una señal `SIGNAL_UPDATE`, el cliente actualiza automáticamente todos los componentes de la interfaz.

## 🔔 Señal del Servidor

### Formato de la Señal
```json
{
  "type": "SIGNAL_UPDATE",
  "resource": "NUEVO_MENSAJE"
}
```

Esta señal puede indicar diferentes tipos de actualizaciones:
- `NUEVO_MENSAJE`: Indica que hay nuevos mensajes (privados o de canales)
- Otros recursos pueden agregarse en el futuro

## 🏗️ Arquitectura del Sistema

El sistema está compuesto por tres componentes principales:

### 1. **GestorSincronizacionGlobal**
- **Ubicación**: `Negocio/GestionNotificaciones/src/main/java/gestionNotificaciones/`
- **Responsabilidad**: 
  - Escucha la señal `SIGNAL_UPDATE` del `GestorRespuesta`
  - Notifica a sus observadores cuando detecta cambios
  - Actúa como hub central de notificaciones
- **Patrón**: Singleton + Observer

```java
// Se registra automáticamente en el GestorRespuesta
GestorSincronizacionGlobal.getInstancia().inicializar();
```

### 2. **CoordinadorActualizaciones**
- **Ubicación**: `Negocio/Fachada/src/main/java/fachada/`
- **Responsabilidad**: 
  - Se registra como observador del `GestorSincronizacionGlobal`
  - Traduce las señales en acciones concretas
  - Dispara las actualizaciones en las fachadas correspondientes
- **Patrón**: Singleton + Observer

```java
// Se inicializa automáticamente con la FachadaGeneral
CoordinadorActualizaciones.getInstancia(fachadaGeneral).inicializar();
```

### 3. **FachadaGeneralImpl**
- **Ubicación**: `Negocio/Fachada/src/main/java/fachada/`
- **Responsabilidad**: 
  - Inicializa el sistema completo de sincronización
  - Coordina todas las fachadas específicas

## 🔄 Flujo de Actualización

```
Servidor
   ↓
   📡 Envía: {"type":"SIGNAL_UPDATE","resource":"NUEVO_MENSAJE"}
   ↓
GestorRespuesta (Escucha socket)
   ↓
GestorSincronizacionGlobal (Maneja SIGNAL_UPDATE)
   ↓
   📢 Notifica: ACTUALIZAR_CONTACTOS
   📢 Notifica: ACTUALIZAR_CANALES
   📢 Notifica: ACTUALIZAR_MENSAJES_PRIVADOS
   📢 Notifica: ACTUALIZAR_MENSAJES_CANALES
   📢 Notifica: ACTUALIZAR_NOTIFICACIONES
   ↓
CoordinadorActualizaciones (Observador)
   ↓
   📇 FachadaContactos.solicitarActualizacionContactos()
   📢 FachadaCanales.solicitarCanalesUsuario()
   🔔 FachadaNotificaciones.obtenerNotificaciones()
   ↓
Componentes de negocio hacen peticiones al servidor
   ↓
Datos actualizados → Repositorios → UI
```

## 🚀 Eventos Disparados

Cuando se recibe `SIGNAL_UPDATE`, el sistema notifica los siguientes eventos:

| Evento | Descripción | Acción |
|--------|-------------|--------|
| `ACTUALIZAR_CONTACTOS` | Actualiza lista de contactos | Solicita contactos al servidor |
| `ACTUALIZAR_CANALES` | Actualiza lista de canales | Solicita canales al servidor |
| `ACTUALIZAR_MENSAJES_PRIVADOS` | Actualiza mensajes privados | Se maneja por push automático |
| `ACTUALIZAR_MENSAJES_CANALES` | Actualiza mensajes de canales | Se maneja por push automático |
| `ACTUALIZAR_NOTIFICACIONES` | Actualiza notificaciones | Solicita notificaciones al servidor |
| `SINCRONIZACION_GLOBAL` | Actualización completa | Dispara todos los anteriores |

## 💻 Integración con la UI

Las vistas de la interfaz ya están configuradas como observadores de las fachadas. Cuando las fachadas reciben datos actualizados, automáticamente notifican a la UI.

### Ejemplo de flujo completo:

1. **Usuario A envía un mensaje a Usuario B**
2. **Servidor procesa el mensaje**
3. **Servidor envía SIGNAL_UPDATE a Usuario B**
4. **Cliente de Usuario B recibe la señal**
5. **GestorSincronizacionGlobal dispara actualizaciones**
6. **CoordinadorActualizaciones solicita datos frescos**
7. **Las fachadas obtienen datos del servidor**
8. **UI se actualiza automáticamente**

## 🔧 Inicialización

El sistema se inicializa automáticamente cuando se crea la `FachadaGeneralImpl`:

```java
public class FachadaGeneralImpl implements IFachadaGeneral {
    private FachadaGeneralImpl() {
        // ... inicialización de fachadas ...
        
        // Inicializar el Gestor de Sincronización Global
        GestorSincronizacionGlobal.getInstancia().inicializar();
        
        // Inicializar el Coordinador que conecta el gestor con las fachadas
        CoordinadorActualizaciones.getInstancia(this).inicializar();
    }
}
```

**No se requiere ninguna configuración adicional**. El sistema se activa automáticamente al iniciar la aplicación.

## 🎯 Ventajas del Sistema

1. **Sincronización en tiempo real**: Los usuarios ven los cambios inmediatamente
2. **Desacoplamiento**: Los componentes están separados y comunicados por observadores
3. **Escalabilidad**: Fácil agregar nuevos tipos de actualizaciones
4. **Mantenibilidad**: Lógica centralizada y clara
5. **Sin intervención manual**: Todo funciona automáticamente

## 🔍 Debugging y Logs

El sistema genera logs detallados para facilitar el debugging:

```
🔔 [GestorSincronizacionGlobal]: SIGNAL_UPDATE recibida
📡 [GestorSincronizacionGlobal]: Recurso actualizado: NUEVO_MENSAJE
💬 [GestorSincronizacionGlobal]: Detectado nuevo mensaje - Disparando actualización global
🔄 [GestorSincronizacionGlobal]: Iniciando actualización global de la aplicación
   📇 Solicitando actualización de contactos...
   📢 Solicitando actualización de canales...
   💬 Solicitando actualización de mensajes privados...
   📨 Solicitando actualización de mensajes de canales...
   🔔 Solicitando actualización de notificaciones...
✅ [GestorSincronizacionGlobal]: Actualización global completada
```

## 🧪 Testing

Para probar el sistema manualmente:

```java
// Forzar una actualización global desde cualquier parte del código
GestorSincronizacionGlobal.getInstancia().forzarActualizacion();
```

## 📝 Notas Importantes

1. **Los mensajes privados y de canales ya tienen push automático**: No necesitan ser solicitados explícitamente, pero se incluyen en la sincronización global por seguridad.

2. **El sistema es resistente a errores**: Si una actualización falla, no afecta a las demás.

3. **Thread-safe**: Utiliza `ConcurrentHashMap` y estructuras seguras para hilos.

4. **Singleton pattern**: Garantiza una única instancia de cada componente crítico.

## 🚦 Estado Actual

✅ **Sistema implementado y funcional**
✅ **Compilación exitosa**
✅ **Integración completa con fachadas**
✅ **Documentación completa**

El cliente está listo para recibir y procesar señales `SIGNAL_UPDATE` del servidor.

