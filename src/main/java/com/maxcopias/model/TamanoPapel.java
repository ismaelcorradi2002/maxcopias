package com.maxcopias.model;

public enum TamanoPapel {

    A5("A5", "Formato reducido para fichas, flyers pequenos y material compacto."),
    A4("A4", "Formato estandar para apuntes, informes y documentos."),
    A3("A3", "Formato amplio para carteles, planos y material visual.");

    private final String label;
    private final String description;

    TamanoPapel(String label, String description) {
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

