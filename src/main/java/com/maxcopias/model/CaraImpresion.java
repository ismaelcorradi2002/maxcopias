package com.maxcopias.model;

public enum CaraImpresion {

    ONE_SIDED("Una cara", "Cada pagina se imprime por una sola cara."),
    DOUBLE_SIDED("Doble cara", "Reduce papel y deja el documento mas compacto.");

    private final String label;
    private final String description;

    CaraImpresion(String label, String description) {
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

