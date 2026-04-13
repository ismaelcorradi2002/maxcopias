package com.maxcopias.controller;

/**
 * Controlador de la página principal (home).
 */
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ControladorInicio {

    /**
     * Ruta principal de la app (página de inicio).
     */
    @GetMapping("/")
    public String home() {
        return "inicio/inicio";
    }

    /**
     * Página de la tienda de papelería.
     */
    @GetMapping("/tienda")
    public String tienda() {
        return "tienda/tienda";
    }

    /**
     * Detalles de producto tienda.
     */
    @GetMapping("/detalles-producto/{id}")
    public String detallesProducto(@PathVariable Long id) {
        return "tienda/detalles-producto";
    }
}

