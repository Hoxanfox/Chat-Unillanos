package gestionUsuario.sesion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración para GestorSesionUsuario.
 * Verifica el comportamiento del Singleton y la gestión de sesiones.
 */
class GestorSesionUsuarioTest {

    private GestorSesionUsuario gestorSesion;

    @BeforeEach
    void setUp() throws Exception {
        gestorSesion = GestorSesionUsuario.getInstancia();
        
        // Limpiar estado del singleton
        Field userIdField = GestorSesionUsuario.class.getDeclaredField("userId");
        userIdField.setAccessible(true);
        userIdField.set(gestorSesion, null);
        
        Field usuarioField = GestorSesionUsuario.class.getDeclaredField("usuarioLogueado");
        usuarioField.setAccessible(true);
        usuarioField.set(gestorSesion, null);
    }

    @Test
    void testSingletonInstance() {
        GestorSesionUsuario instancia1 = GestorSesionUsuario.getInstancia();
        GestorSesionUsuario instancia2 = GestorSesionUsuario.getInstancia();
        
        assertSame(instancia1, instancia2, "Debe retornar la misma instancia (Singleton)");
        System.out.println("✅ Test Singleton - PASÓ");
    }

    @Test
    void testEstablecerYObtenerUserId() {
        String userId = UUID.randomUUID().toString();
        
        gestorSesion.setUserId(userId);
        
        assertEquals(userId, gestorSesion.getUserId(), "El userId debe coincidir");
        assertTrue(gestorSesion.haySesionActiva(), "Debe haber sesión activa");
        System.out.println("✅ Test establecer userId - PASÓ");
    }

    @Test
    void testObtenerUserIdSinSesion() {
        assertThrows(IllegalStateException.class, () -> {
            gestorSesion.getUserId();
        }, "Debe lanzar excepción cuando no hay sesión activa");
        System.out.println("✅ Test userId sin sesión - PASÓ");
    }

    @Test
    void testCerrarSesion() {
        String userId = UUID.randomUUID().toString();
        gestorSesion.setUserId(userId);
        
        assertTrue(gestorSesion.haySesionActiva(), "Debe haber sesión activa antes de cerrar");
        
        gestorSesion.cerrarSesion();
        
        assertFalse(gestorSesion.haySesionActiva(), "No debe haber sesión activa después de cerrar");
        System.out.println("✅ Test cerrar sesión - PASÓ");
    }

    @Test
    void testHaySesionActiva() {
        assertFalse(gestorSesion.haySesionActiva(), "No debe haber sesión activa inicialmente");
        
        gestorSesion.setUserId(UUID.randomUUID().toString());
        
        assertTrue(gestorSesion.haySesionActiva(), "Debe haber sesión activa después de setUserId");
        System.out.println("✅ Test verificar sesión activa - PASÓ");
    }

    @Test
    void testCerrarSesionLimpiaCompletamente() {
        String userId = UUID.randomUUID().toString();
        gestorSesion.setUserId(userId);
        
        gestorSesion.cerrarSesion();
        
        assertFalse(gestorSesion.haySesionActiva(), "Sesión debe estar inactiva");
        
        assertThrows(IllegalStateException.class, () -> {
            gestorSesion.getUserId();
        }, "No debe poder obtener userId después de cerrar sesión");
        
        System.out.println("✅ Test limpieza completa - PASÓ");
    }
}

