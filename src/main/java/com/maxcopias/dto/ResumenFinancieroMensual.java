package com.maxcopias.dto;

import java.math.BigDecimal;

public record ResumenFinancieroMensual(
    int mes,
    int anio,
    BigDecimal ingresosTotales,
    BigDecimal ingresosCopisteria,
    BigDecimal ingresosTienda,
    long pedidosEntregados,
    long pedidosCancelados,
    long pedidosPendientesOPreparacion,
    BigDecimal ticketMedio,
    String productoMasVendido,
    String servicioCopisteriaMasSolicitado,
    BigDecimal ingresosMesAnterior,
    BigDecimal diferenciaIngresos
) {
}
