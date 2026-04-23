package com.maxcopias.controller;

import com.maxcopias.dto.CategoriaTiendaVista;
import com.maxcopias.dto.ProductoTiendaVista;
import com.maxcopias.model.Producto;
import com.maxcopias.service.ServicioCatalogoTiendaVisual;
import com.maxcopias.service.ServicioOferta;
import com.maxcopias.service.ServicioTienda;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class ControladorInicio {

    private final ServicioTienda servicioTienda;
    private final ServicioCatalogoTiendaVisual servicioCatalogoTiendaVisual;
    private final ServicioOferta servicioOferta;

    public ControladorInicio(
        ServicioTienda servicioTienda,
        ServicioCatalogoTiendaVisual servicioCatalogoTiendaVisual,
        ServicioOferta servicioOferta
    ) {
        this.servicioTienda = servicioTienda;
        this.servicioCatalogoTiendaVisual = servicioCatalogoTiendaVisual;
        this.servicioOferta = servicioOferta;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("ofertaPrincipal", servicioOferta.obtenerOfertaPrincipalActiva().orElse(null));
        return "inicio/inicio";
    }

    @GetMapping("/contacto")
    public String contacto() {
        return "contacto/contacto";
    }

    @GetMapping("/tienda")
    public String tienda(Model model) {
        List<Producto> productos = servicioTienda.obtenerProductosActivos();
        List<CategoriaTiendaVista> categorias = servicioCatalogoTiendaVisual.obtenerCategoriasVisibles(productos);
        List<ProductoTiendaVista> productosVista = servicioCatalogoTiendaVisual.mapearProductos(productos);

        model.addAttribute("categoriasTienda", categorias);
        model.addAttribute("productosTienda", productosVista);
        return "tienda/tienda";
    }

    @GetMapping("/detalles-producto/{id}")
    public String detallesProducto(@PathVariable Long id, Model model) {
        try {
            Producto producto = servicioTienda.obtenerProductoPorId(id);
            ProductoTiendaVista productoVista = servicioCatalogoTiendaVisual.mapearProducto(producto);
            model.addAttribute("productoTienda", productoVista);
            return "tienda/detalles-producto";
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado.");
        }
    }
}
