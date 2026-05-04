package com.maxcopias.dto;

import java.util.List;

public record CheckoutVista(
    FormularioCheckoutTienda formulario,
    CarritoVista carrito,
    List<String> pasos
) {
}
