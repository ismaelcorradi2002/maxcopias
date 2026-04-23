package com.maxcopias.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para mostrar pedidos unificados (copistería y tienda) en el panel de administrador.
 * Incluye todos los campos posibles de ambas entidades; los que no apliquen serán null.
 */
public record PedidoAdminVista(
    Long id,
    String tipo,
    String cliente,
    String email,
    String telefono,
    String estado,
    LocalDateTime fechaCreacion,
    BigDecimal total,
    String trabajo,
    Integer copias,
    String color,
    String tamano,
    String caras,
    String papel,
    String encuadernacion,
    String extras,
    String rutaArchivo,
    String codigoRecoger,
    String resumenProductos,
    String usuarioNombre
) {
}

