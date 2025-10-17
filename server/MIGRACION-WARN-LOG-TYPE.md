# Migración: Agregar tipo 'WARN' a logs_sistema

**Fecha**: 17 de octubre, 2025  
**Versión**: 1.0.1

## Problema Detectado

Al implementar el manejo mejorado de excepciones que distingue entre validaciones de negocio y errores reales del sistema, se introdujo un nuevo tipo de log: `WARN`.

Sin embargo, la tabla `logs_sistema` en la base de datos solo permitía los siguientes tipos en su columna ENUM:
- `LOGIN`
- `LOGOUT`
- `ERROR`
- `INFO`
- `SYSTEM`

Esto causaba el siguiente error al intentar guardar logs de advertencia:

```
java.sql.SQLException: Data truncated for column 'tipo' at row 1
```

## Solución Aplicada

### 1. Script de Migración

Se creó el archivo `migration-add-warn-log-type.sql` con el siguiente contenido:

```sql
USE chat_unillanos;

ALTER TABLE logs_sistema 
MODIFY COLUMN tipo ENUM('LOGIN', 'LOGOUT', 'ERROR', 'WARN', 'INFO', 'SYSTEM') NOT NULL;
```

### 2. Actualización del Script de Inicialización

Se actualizó `init-db.sql` para incluir `WARN` en las nuevas instalaciones:

```sql
CREATE TABLE IF NOT EXISTS logs_sistema (
    -- ...
    tipo ENUM('LOGIN', 'LOGOUT', 'ERROR', 'WARN', 'INFO', 'SYSTEM') NOT NULL,
    -- ...
);
```

### 3. Ejecución de la Migración

La migración se ejecutó exitosamente en la base de datos existente sin pérdida de datos.

## Resultado

✅ **Migración exitosa**: La columna `tipo` ahora acepta el valor 'WARN'  
✅ **Compatibilidad**: Los logs existentes permanecen intactos  
✅ **Funcionalidad completa**: El sistema ahora puede registrar advertencias de validación de negocio correctamente

## Tipos de Log Disponibles

Después de la migración, el sistema soporta los siguientes tipos de log:

| Tipo | Descripción | Uso |
|------|-------------|-----|
| `LOGIN` | Inicio de sesión de usuario | Autenticación exitosa |
| `LOGOUT` | Cierre de sesión de usuario | Usuario se desconecta |
| `ERROR` | Error del sistema | Excepciones inesperadas, fallos críticos |
| `WARN` | **NUEVO** Advertencia | Validaciones de negocio, situaciones esperadas pero inusuales |
| `INFO` | Información general | Acciones normales del sistema |
| `SYSTEM` | Evento del sistema | Inicio/detención de servicios, configuración |

## Uso en el Código

```java
// Registrar una advertencia
loggerService.logWarning("gestionarMiembro", "El usuario ya es miembro del canal");

// Registrar un error
loggerService.logError("conexionDB", "No se pudo conectar a la base de datos");

// Registrar información
loggerService.logInfo("enviarMensaje", "Mensaje enviado exitosamente");
```

## Comandos Ejecutados

```bash
# Ejecutar migración
docker exec -i chat-unillanos-mysql mysql -uchatuser -pchatpassword chat_unillanos < migration-add-warn-log-type.sql

# Verificar cambio
docker exec chat-unillanos-mysql mysql -uchatuser -pchatpassword chat_unillanos -e "SHOW COLUMNS FROM logs_sistema WHERE Field = 'tipo';"
```

## Compatibilidad hacia Atrás

✅ Esta migración es **100% compatible hacia atrás**:
- No elimina ningún valor existente del ENUM
- Solo agrega un nuevo valor válido
- Los logs existentes no se ven afectados
- No requiere actualizar datos existentes

## Notas Importantes

- **Sin downtime**: La migración se puede ejecutar sin detener el servidor
- **Reversible**: Si es necesario revertir, simplemente eliminar 'WARN' del ENUM (si no hay registros usando ese valor)
- **Idempotente**: El script puede ejecutarse múltiples veces sin causar problemas

## Testing Recomendado

1. Intentar agregar un usuario duplicado a un canal
2. Verificar que el log se guarda correctamente con tipo 'WARN'
3. Confirmar que no aparece el error "Data truncated for column 'tipo'"
4. Verificar en la GUI de administración que las advertencias aparecen correctamente

