package com.maxcopias.dto;

public record ConfirmacionPedidoTiendaVista(
    ResumenPedidoTiendaVista pedido,
    String mensajePrincipal
) {
}
