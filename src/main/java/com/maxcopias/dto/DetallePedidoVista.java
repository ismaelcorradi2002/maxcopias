package com.maxcopias.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.List;

public record DetallePedidoVista(
    Long id,
    String tipo,
    String cliente,
    String email,
    String telefono,
    String trabajo,
    String tamano,
    String color,
    String caras,
    Integer copias,
    String papel,
    String encuadernacion,
    List<String> extras,
    String observaciones,
    String archivoNombre,
    String archivoVerUrl,
    String archivoDescargaUrl,
    String archivoImprimirUrl,
    Integer paginas,
    String tamanoArchivo,
    BigDecimal precioBase,
    BigDecimal precioExtras,
    BigDecimal precioTotal,
    boolean precioDesglosado,
    List<LineaResumenEconomico> resumenEconomico,
    String estado,
    String estadoLabel,
    String codigoRecogida,
    LocalDateTime fechaCreacion,
    LocalDateTime fechaActualizacion,
    String resumenProductos,
    String codigoPedido,
    String metodoEntrega,
    boolean pagado,
    String metodoPago,
    LocalDateTime fechaPago,
    BigDecimal subtotalPedido,
    BigDecimal gastosEnvio,
    List<LineaPedidoTiendaVista> lineasTienda,
    boolean eliminado,
    String eliminadoPor
) {
    public boolean tieneArchivo() {
        return archivoNombre != null && !archivoNombre.isBlank();
    }

    public boolean tieneConfiguracionImpresion() {
        return trabajo != null && !trabajo.isBlank();
    }

    public boolean tieneExtras() {
        return extras != null && !extras.isEmpty();
    }

    public String getArchivoExtension() {
        if (!tieneArchivo() || !archivoNombre.contains(".")) {
            return "FILE";
        }
        return archivoNombre.substring(archivoNombre.lastIndexOf('.') + 1).toUpperCase(Locale.ROOT);
    }

    public String getArchivoTipoLabel() {
        return switch (getArchivoExtension()) {
            case "PDF" -> "Documento PDF";
            case "DOC", "DOCX" -> "Documento Word";
            case "JPG", "JPEG", "PNG" -> "Imagen";
            default -> "Archivo adjunto";
        };
    }

    public String getArchivoFechaLabel() {
        if (fechaCreacion == null) {
            return null;
        }
        return fechaCreacion.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    public String getFechaPagoLabel() {
        if (fechaPago == null) {
            return null;
        }
        return fechaPago.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    public boolean tieneLineasTienda() {
        return lineasTienda != null && !lineasTienda.isEmpty();
    }

    public String getSubtotalPedidoFormateado() {
        return subtotalPedido == null ? null : NumberFormat.getCurrencyInstance(new Locale("es", "ES")).format(subtotalPedido);
    }

    public String getGastosEnvioFormateado() {
        return gastosEnvio == null ? null : NumberFormat.getCurrencyInstance(new Locale("es", "ES")).format(gastosEnvio);
    }
}
