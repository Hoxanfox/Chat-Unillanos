package com.unillanos.server;

import org.springframework.context.ApplicationContext;

/**
 * Proveedor estático del ApplicationContext para acceso desde GUI.
 */
public final class GuiContext {

    private static ApplicationContext context;

    private GuiContext() { }

    public static void setContext(ApplicationContext ctx) {
        context = ctx;
    }

    public static ApplicationContext getContext() {
        return context;
    }
}


