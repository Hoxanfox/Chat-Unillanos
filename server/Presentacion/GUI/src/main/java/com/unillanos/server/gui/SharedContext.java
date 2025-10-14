package com.unillanos.server.gui;

import org.springframework.context.ApplicationContext;

/**
 * Contexto compartido para acceder a Spring ApplicationContext desde la GUI.
 */
public final class SharedContext {

    private static ApplicationContext context;

    private SharedContext() { }

    public static void set(ApplicationContext ctx) {
        context = ctx;
    }

    public static ApplicationContext get() {
        return context;
    }
}


