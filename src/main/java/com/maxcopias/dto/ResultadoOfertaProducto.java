package com.maxcopias.dto;

import com.maxcopias.model.Oferta;
import java.math.BigDecimal;

public record ResultadoOfertaProducto(
    Oferta oferta,
    BigDecimal precioOriginal,
    BigDecimal precioFinal,
    Integer porcentajeDescuento,
    boolean aplicable
) {
}
