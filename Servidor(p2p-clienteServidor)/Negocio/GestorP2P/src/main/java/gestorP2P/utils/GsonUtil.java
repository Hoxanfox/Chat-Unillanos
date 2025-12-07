package gestorP2P.utils;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.time.Instant;

public class GsonUtil {

    /**
     * Crea una instancia de Gson configurada para manejar Java Time (Instant)
     * y evitar errores de reflexión en Java 17+.
     */
    public static Gson crearGson() {
        return new GsonBuilder()
                // Adaptador para serializar (Instant -> String)
                .registerTypeAdapter(Instant.class, new JsonSerializer<Instant>() {
                    @Override
                    public JsonElement serialize(Instant src, Type typeOfSrc, JsonSerializationContext context) {
                        return new JsonPrimitive(src.toString());
                    }
                })
                // Adaptador para deserializar (String -> Instant)
                .registerTypeAdapter(Instant.class, new JsonDeserializer<Instant>() {
                    @Override
                    public Instant deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                        return Instant.parse(json.getAsString());
                    }
                })
                .serializeNulls() // Opcional: para incluir nulos si es necesario
                .create();
    }
}
