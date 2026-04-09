package com.maxcopias.model;

public enum ModoColor {

    COLOR("Color", "Ideal para presentaciones, portadas y materiales visuales."),
    BLACK_AND_WHITE("Blanco y negro", "La opcion mas practica para apuntes y documentos diarios.");

    private final String label;
    private final String description;

    ModoColor(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }
}

