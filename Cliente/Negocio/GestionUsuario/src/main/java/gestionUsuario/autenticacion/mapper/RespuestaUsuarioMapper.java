package gestionUsuario.autenticacion.mapper;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dominio.Usuario;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Utilidad para mapear la sección "data" de la respuesta del servidor a objetos de dominio.
 */
public class RespuestaUsuarioMapper {

    private RespuestaUsuarioMapper() {}

    public static Map<String, Object> toMap(Object data, Gson gson) {
        if (data == null) return null;
        Type type = new TypeToken<Map<String, Object>>() {}.getType();
        return gson.fromJson(gson.toJson(data), type);
    }

    @SuppressWarnings("unused")
    public static Usuario buildUsuarioFromMap(Map<String, Object> datos) {
        if (datos == null) return null;

        String userIdStr = firstString(datos, "userId", "id");
        if (userIdStr == null || userIdStr.isEmpty()) return null;

        Usuario usuario = new Usuario();
        usuario.setIdUsuario(UUID.fromString(userIdStr));
        usuario.setNombre(firstString(datos, "nombre", "username"));
        usuario.setEmail(firstString(datos, "email"));
        usuario.setEstado("activo");

        String fileId = firstString(datos, "fileId", "photoAddress", "photoId", "imagenBase64");
        if (fileId != null) usuario.setPhotoIdServidor(fileId);

        usuario.setFechaRegistro(LocalDateTime.now());
        return usuario;
    }

    // Helper público reutilizable para extraer el primer valor no nulo/no vacío de varias claves
    public static String firstString(Map<String, Object> map, String... keys) {
        if (map == null) return null;
        for (String k : keys) {
            if (map.containsKey(k)) {
                Object v = map.get(k);
                if (v != null) {
                    String s = String.valueOf(v);
                    if (!s.isEmpty() && !"null".equalsIgnoreCase(s)) {
                        return s;
                    }
                }
            }
        }
        return null;
    }
}
