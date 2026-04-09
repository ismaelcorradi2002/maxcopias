package com.maxcopias.model;

public enum EstadoPedidoCopisteria {

    RECIBIDO("Recibido"),
    EN_PREPARACION("En preparacion"),
    LISTO_PARA_RECOGER("Listo para recoger"),
    ENTREGADO("Entregado");

    private final String label;

    EstadoPedidoCopisteria(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

