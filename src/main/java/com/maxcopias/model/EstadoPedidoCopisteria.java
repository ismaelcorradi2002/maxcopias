package com.maxcopias.model;

public enum EstadoPedidoCopisteria {

    PENDIENTE("Pendiente"),
    EN_PREPARACION("En preparacion"),
    LISTO_PARA_RECOGER("Preparado para envio"),
    ENTREGADO("Entregado"),
    CANCELADO("Cancelado");

    private final String label;

    EstadoPedidoCopisteria(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static EstadoPedidoCopisteria fromDatabaseValue(String value) {
        if (value == null || value.isBlank()) {
            return PENDIENTE;
        }

        return switch (value) {
            case "RECIBIDO", "REVISANDO_ARCHIVO", "PENDIENTE" -> PENDIENTE;
            case "EN_PREPARACION", "PREPARANDO" -> EN_PREPARACION;
            case "LISTO_PARA_RECOGER", "LISTO", "PREPARADO_PARA_ENVIO", "ENVIADO" -> LISTO_PARA_RECOGER;
            case "ENTREGADO" -> ENTREGADO;
            case "CANCELADO" -> CANCELADO;
            default -> PENDIENTE;
        };
    }
}
