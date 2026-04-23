package com.maxcopias.model;

public enum TipoOferta {
    PRODUCTO("Producto"),
    CATEGORIA("Categoria"),
    GLOBAL("Global");

    private final String label;

    TipoOferta(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
