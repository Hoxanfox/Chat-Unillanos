package com.arquitectura.utils.logs;

import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class LogService implements ILogService{

    private static final String LOG_FILE_PATH = "logs/server.log";
    @Override
    public String getLogContents() throws IOException {
        try {
            return Files.readString(Paths.get(LOG_FILE_PATH), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "No se pudo leer el archivo de log. ¿El servidor ya ha generado alguno?\n\nError: " + e.getMessage();
        }
    }


}