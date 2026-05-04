package com.maxcopias.model;

public enum MetodoPagoPedidoTienda {
    PAGO_EN_TIENDA("Pago en tienda"),
    ENVIO_SIMULADO("Envio simulado");

    private final String label;

    MetodoPagoPedidoTienda(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
