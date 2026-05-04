package com.maxcopias.controller;

import com.maxcopias.service.ServicioCarrito;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class ControladorGlobalTienda {

    private final ServicioCarrito servicioCarrito;

    public ControladorGlobalTienda(ServicioCarrito servicioCarrito) {
        this.servicioCarrito = servicioCarrito;
    }

    @ModelAttribute("cartItemCount")
    public int cartItemCount(Authentication authentication, HttpSession session) {
        return servicioCarrito.contarItems(authentication, session);
    }
}
