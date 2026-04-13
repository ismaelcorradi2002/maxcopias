package com.maxcopias.model;

public enum TipoEncuadernacion {

    SIN_ENCUADERNACION("No", "El pedido no necesita encuadernacion."),
    ESPIRAL("Espiral", "La opcion mas habitual para apuntes y trabajos."),
    TAPA_DURA("Tapa dura", "Acabado mas solido para memorias y entregas formales."),
    GRAPADO("Grapado", "Solucion rapida para juegos cortos de hojas.");

    private final String label;
    private final String description;

    TipoEncuadernacion(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public boolean isNone() {
        return this == SIN_ENCUADERNACION;
    }
}
