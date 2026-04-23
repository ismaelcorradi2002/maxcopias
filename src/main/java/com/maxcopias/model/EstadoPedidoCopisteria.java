package com.maxcopias.model;

public enum EstadoPedidoCopisteria {

    RECIBIDO("Pendiente"),
    REVISANDO_ARCHIVO("Revisando archivo"),
    EN_PREPARACION("Preparando"),
    LISTO_PARA_RECOGER("Listo para recoger"),
    ENTREGADO("Entregado"),
    CANCELADO("Cancelado");

    private final String label;

    EstadoPedidoCopisteria(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

