package main;

import interfazConsola.VistaConsola;

public class Main {
    public static void main(String[] args) {
        // Main solo sabe que existe una Vista y que debe arrancarla.
        VistaConsola vista = new VistaConsola();
        vista.mostrarInterfaz();
    }
}
