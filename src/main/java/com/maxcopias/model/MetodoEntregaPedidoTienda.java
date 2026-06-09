package com.maxcopias.model;

public enum MetodoEntregaPedidoTienda {
    RECOGIDA_TIENDA("Recogida legacy"),
    ENVIO_DOMICILIO("Envio a domicilio");

    private final String label;

    MetodoEntregaPedidoTienda(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
