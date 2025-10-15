# Épica 6: Interfaz de Administración (JavaFX) - Plan Detallado

## Objetivo General

Implementar una interfaz gráfica de administración (JavaFX 21) para monitorear y gestionar en tiempo real el servidor Chat-Unillanos: estado del servidor, conexiones activas, usuarios, canales y logs. La GUI seguirá la arquitectura modular (4 capas), patrones MVC y Observer, y no contendrá lógica de negocio ni acceso directo a datos.

## Contexto Actual

✅ Ya Implementado (Épicas 1-5):
- ✅ Infraestructura de excepciones y manejo global de errores
- ✅ LoggerService con persistencia asíncrona y LogRepository
- ✅ ConnectionManager para gestión de conexiones en tiempo real
- ✅ Netty server funcional y ActionDispatcher enrutando acciones
- ✅ Gestión de usuarios completa (registro, login, perfil, estados)
- ✅ Gestión de canales completa (crear, unirse, listar, gestionar miembros)
- ✅ Mensajería en tiempo real (directos y canales) e historial
- ✅ Gestión de archivos (subir, descargar, listar, deduplicación)

⚠️ Pendiente de Implementar:
- ❌ Estructura de vistas y controladores JavaFX (Dashboard, Usuarios, Canales, Logs)
- ❌ Integración GUI ↔ Servicios (invocación a servicios, DTOs/DTOResponse)
- ❌ Suscripción a eventos (Observer) para logs/conexiones en tiempo real
- ❌ Paginación y filtros en tablas (usuarios, canales, logs)
- ❌ Manejo de errores y estados de carga (loaders, toasts)

---

## Componentes a Implementar

### 1. Arquitectura de la GUI (JavaFX 21)

**Ubicación:** `Presentacion/GUI/src/main/java/com/unillanos/server/gui/`

- Estructura MVC por vista:
  - `view/` (Java/FXML opcional) y `controller/` (Java)
  - Modelos de presentación (POJOs) desacoplados de entidades de BD
- Ventana principal (`MainWindow`): barra lateral (Dashboard, Usuarios, Canales, Logs) y panel central con `StackPane` para navegación
- Tema visual unificado (claro), espaciado consistente, tipografías legibles

### 2. Vistas y Funcionalidad

1) **DashboardView/DashboardController**
   - Estado del servidor: activo, puerto, uptime
   - Conexiones activas (contador y lista compacta opcional)
   - Uso del pool de conexiones (si está disponible desde config/metricas)
   - Eventos recientes (logs en vivo, auto-scroll, pausa/continuar)

2) **UsersView/UsersController**
   - Tabla paginada: nombre, email, estado, IP, fecha registro
   - Búsqueda (nombre/email) con debounce
   - Filtros por estado (ONLINE/OFFLINE/AWAY)
   - Acciones: ver detalle, forzar logout, cambiar estado

3) **ChannelsView/ChannelsController**
   - Tabla: nombre, activo, creador, fecha, cantidad de miembros
   - Panel lateral con miembros del canal seleccionado (rol y fecha unión)
   - Acciones: crear canal, activar/desactivar, invitar, expulsar, cambiar rol

4) **LogsView/LogsController**
   - Stream en vivo (INFO/ERROR/SYSTEM/LOGIN/LOGOUT) con auto-scroll
   - Filtros por tipo, usuario, rango de fechas y búsqueda de texto
   - Pausar/continuar stream, limpiar vista, exportar selección a CSV

### 3. Integración con Servicios

**Ubicación Servicios:** `LogicaNegocio/Servicios/src/main/java/com/unillanos/server/service/impl/`

- Los controladores invocan servicios (Spring) a través de métodos sincronizados/asincrónicos, nunca JDBC desde la GUI
- Logs en vivo:
  - Opción A (preferida): patrón Observer/Listener en `LoggerService` para push de eventos
  - Opción B (fallback): polling incremental de `ILogRepository.findRecent(limit)` con delta
- Conexiones activas: snapshot desde `ConnectionManager.getAllConnections()` + eventos de conexión/desconexión
- Paginación: usar parámetros `limit/offset` de repositorios/servicios existentes

### 4. Concurrencia y Rendimiento

- Operaciones largas fuera del hilo de UI: `Executors.newVirtualThreadPerTaskExecutor()` o `Task` de JavaFX
- Toda actualización de UI vía `Platform.runLater`
- Back-pressure en Logs (limitar a ~2000 líneas visibles)
- Debounce (≈300 ms) en filtros/búsquedas

### 5. Manejo de Errores y Seguridad

- Mostrar errores como toasts/banners no intrusivos (mensajes de `DTOResponse.error`)
- Confirmaciones para acciones destructivas (expulsar usuario, desactivar canal)
- Modo administrador: restringir acciones administrativas (config de GUI)
- Sanitización de entradas de búsqueda/filtros (UI)

---

## Orden de Implementación

### Fase 1: Skeleton y Navegación
1. Crear `MainWindow` con barra lateral y `StackPane` central
2. Registrar rutas/vistas: Dashboard, Usuarios, Canales, Logs
3. Cargar vistas y controladores (inyectar servicios via Spring)

### Fase 2: Dashboard y Logs Recientes
4. Dashboard: tarjetas de estado (servidor, conexiones, uptime)
5. LogsView (polling simple, limit 100 recientes) con filtros básicos
6. Auto-scroll, botón Pausar/Continuar

### Fase 3: Usuarios
7. Tabla de usuarios con paginación, búsqueda y filtros de estado
8. Acciones: ver detalle, forzar logout, cambiar estado
9. Mensajería de confirmación y manejo de errores

### Fase 4: Canales
10. Tabla de canales con paginación, creación/activación
11. Panel de miembros del canal seleccionado
12. Acciones: invitar, expulsar, cambiar rol (validaciones de permisos)

### Fase 5: Logs en Vivo y Observers
13. Integración Observer con `LoggerService` para stream en vivo
14. Buffer circular y exportación de selección a CSV

### Fase 6: Pulido y Hardening
15. Loaders/estado de carga, debouncing, manejo de timeouts
16. Revisión de permisos en UI, mensajes de error consistentes

---

## Criterios de Aceptación

1. La GUI inicia desde `Presentacion/Main` y muestra `MainWindow` con navegación completa
2. Dashboard muestra en ≤2 s: estado, conexiones activas y logs recientes
3. LogsView soporta stream en vivo con filtros y pausa/continuar
4. UsersView lista usuarios con paginación, búsqueda y acciones (logout/cambiar estado)
5. ChannelsView lista canales, muestra miembros y permite acciones administrativas
6. Acciones que requieran permisos solo visibles/habilitadas en modo admin
7. Todas las interacciones de red se realizan vía servicios; sin JDBC en GUI
8. La UI no se bloquea: operaciones en hilos virtuales; actualizaciones via `Platform.runLater`
9. Errores se muestran de forma clara (toasts/banners) y no rompen la UI
10. Compila `mvn clean install` sin errores

---

## Verificación Final

### Prueba 1: Inicio de la GUI
```bash
mvn -pl Presentacion/Main spring-boot:run
```
Esperado: Se abre la ventana principal con navegación a Dashboard/Usuarios/Canales/Logs.

### Prueba 2: Dashboard (estado y conexiones)
- Ver estado del servidor (activo, puerto) y conexiones activas > 0 al conectar clientes

### Prueba 3: Logs (polling y en vivo)
- Ver últimos 100 logs y aplicar filtros por tipo
- Activar stream en vivo y observar entradas nuevas al generar eventos (login, mensajes)

### Prueba 4: Usuarios
- Buscar por nombre/email y ver resultados paginados
- Forzar logout de un usuario y validar cambio de estado/registro de log

### Prueba 5: Canales
- Listar canales, ver miembros, invitar y cambiar rol; verificar logs correspondientes

### Prueba 6: Robustez
- Apagar/encender servidor y verificar recuperación de la GUI (mensajes de error y reintentos/polling)

---

## Dependencias Nuevas Requeridas

No se requieren nuevas dependencias externas. Se utilizarán:
- JavaFX 21 (ya configurado en `Presentacion/GUI/pom.xml`)
- Spring Boot para inyección de dependencias en controladores
- Ejecutor de hilos virtuales (Java 21) para tareas en background

---

## Estimación de Tiempo

- Fase 1 (Skeleton + navegación): ~40-50 min
- Fase 2 (Dashboard + logs recientes): ~40-50 min
- Fase 3 (Usuarios): ~60-75 min
- Fase 4 (Canales): ~60-75 min
- Fase 5 (Logs en vivo + Observer): ~40-50 min
- Fase 6 (Pulido/Hardening): ~30-40 min

Total Estimado: 270-340 minutos (4.5 - 5.7 horas)

---

## Notas Importantes

🔐 Seguridad:
- Modo administrador: ocultar/inhabilitar acciones restrigidas si no está activo
- Sanitización de entradas UI (búsqueda, filtros)

⚡ Performance:
- Evitar bloqueos en UI; todas las llamadas a servicios en hilos virtuales
- Limitar registros visibles en logs y paginar tablas

📊 Observabilidad:
- Registrar acciones de GUI relevantes como SYSTEM/INFO para auditoría

🎯 Reglas de Arquitectura:
- La GUI no debe acceder a repositorios ni a JDBC; solo a servicios
- Mantener MVC estricto y uso de Observer para tiempo real

---

¿Listo para comenzar la implementación de la Épica 6 (Fase 1: Skeleton y Navegación)?


