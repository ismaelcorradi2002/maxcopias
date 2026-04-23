package com.maxcopias.model;

public enum EstadoPedidoTienda {
    PENDIENTE("Pendiente"),
    PREPARANDO("Preparando"),
    LISTO("Listo"),
    ENTREGADO("Entregado"),
    CANCELADO("Cancelado");

    private final String label;

    EstadoPedidoTienda(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
