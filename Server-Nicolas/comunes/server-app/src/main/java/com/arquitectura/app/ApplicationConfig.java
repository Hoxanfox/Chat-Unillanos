package com.arquitectura.app;

import com.arquitectura.utils.json.LocalDateTimeAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
@ComponentScan(basePackages = "com.arquitectura")
public class ApplicationConfig {
    @Bean
    public Gson gson() {
        return new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .create();
    }
}