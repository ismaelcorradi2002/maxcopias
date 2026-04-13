package com.maxcopias.model;

public enum TipoPapel {

    NORMAL("Normal", "Papel estandar para apuntes y documentos diarios."),
    SATINADO("Satinado", "Acabado mas fino para presentaciones y materiales visuales."),
    CARTULINA("Cartulina", "Mayor grosor para portadas, fichas y piezas resistentes.");

    private final String label;
    private final String description;

    TipoPapel(String label, String description) {
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
