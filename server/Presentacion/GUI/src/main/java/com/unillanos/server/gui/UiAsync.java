package com.unillanos.server.gui;

import javafx.application.Platform;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Utilidad para ejecutar tareas en hilos virtuales y publicar en UI.
 */
public final class UiAsync {
    private static final ExecutorService EXEC = Executors.newVirtualThreadPerTaskExecutor();

    private UiAsync() {}

    public static <T> void run(Supplier<T> supplier, java.util.function.Consumer<T> uiConsumer, java.util.function.Consumer<Throwable> errorHandler) {
        CompletableFuture.supplyAsync(supplier, EXEC)
                .whenComplete((res, err) -> {
                    if (err != null) {
                        if (errorHandler != null) errorHandler.accept(err);
                    } else {
                        Platform.runLater(() -> uiConsumer.accept(res));
                    }
                });
    }
}


