package com.maxcopias.model;

public enum EstadoPedidoTienda {
    PENDIENTE("Pendiente"),
    EN_PREPARACION("En preparacion"),
    LISTO_PARA_RECOGER("Listo para recoger"),
    ENTREGADO("Entregado"),
    CANCELADO("Cancelado");

    private final String label;

    EstadoPedidoTienda(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static EstadoPedidoTienda fromDatabaseValue(String value) {
        if (value == null || value.isBlank()) {
            return PENDIENTE;
        }

        return switch (value) {
            case "PENDIENTE" -> PENDIENTE;
            case "EN_PREPARACION", "PREPARANDO" -> EN_PREPARACION;
            case "LISTO_PARA_RECOGER", "LISTO" -> LISTO_PARA_RECOGER;
            case "ENTREGADO" -> ENTREGADO;
            case "CANCELADO" -> CANCELADO;
            default -> PENDIENTE;
        };
    }
}
