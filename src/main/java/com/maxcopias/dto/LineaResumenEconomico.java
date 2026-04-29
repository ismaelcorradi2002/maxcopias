package com.maxcopias.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record LineaResumenEconomico(
    String concepto,
    String detalle,
    String cantidad,
    BigDecimal precioUnitario,
    BigDecimal subtotal,
    boolean subtotalRow,
    boolean totalRow
) {
    public boolean hasDetalle() {
        return detalle != null && !detalle.isBlank();
    }

    public boolean hasCantidad() {
        return cantidad != null && !cantidad.isBlank();
    }

    public boolean hasPrecioUnitario() {
        return precioUnitario != null;
    }

    public boolean hasSubtotal() {
        return subtotal != null;
    }

    public boolean isInformativa() {
        return !hasPrecioUnitario() && !hasSubtotal();
    }

    public boolean showsZeroLabel() {
        return hasSubtotal()
            && subtotal.setScale(2, RoundingMode.HALF_UP).compareTo(BigDecimal.ZERO) == 0
            && !totalRow;
    }

    public String getPrecioUnitarioFormateado() {
        if (!hasPrecioUnitario()) {
            return "—";
        }
        return precioUnitario.setScale(2, RoundingMode.HALF_UP).toPlainString() + " €";
    }

    public String getSubtotalFormateado() {
        if (!hasSubtotal()) {
            return "—";
        }
        return subtotal.setScale(2, RoundingMode.HALF_UP).toPlainString() + " €";
    }

    public String getCantidadFormateada() {
        return hasCantidad() ? cantidad : "—";
    }

    public String getRowCssClass() {
        if (totalRow) {
            return " is-total";
        }
        if (subtotalRow) {
            return " is-subtotal";
        }
        if (isInformativa()) {
            return " is-info";
        }
        return "";
    }
}
