package com.maxcopias.controller;

import com.maxcopias.model.Categoria;
import com.maxcopias.model.Producto;
import com.maxcopias.service.ServicioTienda;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tienda")
public class ControladorApiTienda {

    private final ServicioTienda servicioTienda;

    public ControladorApiTienda(ServicioTienda servicioTienda) {
        this.servicioTienda = servicioTienda;
    }

    @GetMapping("/productos")
    public List<Producto> obtenerProductos() {
        return servicioTienda.obtenerTodosProductos();
    }

    @GetMapping("/categorias")
    public List<Categoria> obtenerCategorias() {
        return servicioTienda.obtenerTodasCategorias();
    }

    @GetMapping("/categorias/{categoriaId}/productos")
    public List<Producto> obtenerProductosPorCategoria(@PathVariable Long categoriaId) {
        return servicioTienda.obtenerProductosPorCategoria(categoriaId);
    }
}
