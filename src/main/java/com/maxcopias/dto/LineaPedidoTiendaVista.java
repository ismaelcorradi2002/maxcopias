package com.maxcopias.dto;

import java.math.BigDecimal;

public record LineaPedidoTiendaVista(
    String nombre,
    String imagenUrl,
    Integer cantidad,
    BigDecimal precioUnitario,
    BigDecimal subtotal,
    String precioUnitarioFormateado,
    String subtotalFormateado
) {
}
