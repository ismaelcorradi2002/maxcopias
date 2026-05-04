package com.maxcopias.dto;

import java.math.BigDecimal;

public record CarritoItemVista(
    Long productoId,
    String nombre,
    String descripcion,
    String imagenUrl,
    BigDecimal precioUnitario,
    String precioUnitarioFormateado,
    int cantidad,
    int stockDisponible,
    BigDecimal subtotal,
    String subtotalFormateado
) {
}
