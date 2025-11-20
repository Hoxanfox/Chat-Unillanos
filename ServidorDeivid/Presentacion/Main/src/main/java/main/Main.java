package main;

import interfazConsola.VistaConsola;

public class MainApp {
    public static void main(String[] args) {
        // Main solo sabe que existe una Vista y que debe arrancarla.
        VistaConsola vista = new VistaConsola();
        vista.mostrarInterfaz();
    }
}
