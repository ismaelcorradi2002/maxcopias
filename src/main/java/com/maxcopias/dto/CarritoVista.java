package com.maxcopias.dto;

import com.maxcopias.model.MetodoEntregaPedidoTienda;
import java.math.BigDecimal;
import java.util.List;

public record CarritoVista(
    List<CarritoItemVista> items,
    boolean vacio,
    int totalItems,
    BigDecimal subtotal,
    String subtotalFormateado,
    BigDecimal gastosEnvio,
    String gastosEnvioFormateado,
    BigDecimal total,
    String totalFormateado,
    MetodoEntregaPedidoTienda metodoEntrega,
    boolean requiereLoginParaConfirmar
) {
}
