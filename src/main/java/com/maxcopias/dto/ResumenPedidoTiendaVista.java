package com.maxcopias.dto;

import com.maxcopias.model.EstadoPedidoTienda;
import com.maxcopias.model.MetodoEntregaPedidoTienda;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ResumenPedidoTiendaVista(
    Long id,
    String codigoPedido,
    LocalDateTime fechaCreacion,
    List<CarritoItemVista> items,
    BigDecimal subtotal,
    String subtotalFormateado,
    BigDecimal gastosEnvio,
    String gastosEnvioFormateado,
    BigDecimal total,
    String totalFormateado,
    MetodoEntregaPedidoTienda metodoEntrega,
    String metodoEntregaLabel,
    EstadoPedidoTienda estado,
    String estadoLabel,
    String mensajeEstado,
    boolean pagado
) {
}
