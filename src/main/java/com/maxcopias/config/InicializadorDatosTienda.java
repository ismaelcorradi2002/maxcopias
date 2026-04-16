package com.maxcopias.config;

import com.maxcopias.model.Categoria;
import com.maxcopias.model.Producto;
import com.maxcopias.service.ServicioTienda;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class InicializadorDatosTienda implements CommandLineRunner {

    private final ServicioTienda servicioTienda;

    public InicializadorDatosTienda(ServicioTienda servicioTienda) {
        this.servicioTienda = servicioTienda;
    }

    @Override
    public void run(String... args) {
        if (!servicioTienda.obtenerTodasCategorias().isEmpty() || !servicioTienda.obtenerTodosProductos().isEmpty()) {
            return;
        }

        Map<String, Long> categorias = new LinkedHashMap<>();
        categorias.put("Escolar", crearCategoria("Escolar", "Material habitual para clase, mochila y estudio."));
        categorias.put("Oficina", crearCategoria("Oficina", "Productos de escritorio y trabajo diario."));
        categorias.put("Arte", crearCategoria("Arte", "Material creativo para dibujo, ilustracion y manualidades."));
        categorias.put("Organizacion", crearCategoria("Organizacion", "Soluciones para archivar, ordenar y planificar."));
        categorias.put("Tecnologia", crearCategoria("Tecnologia", "Accesorios utiles para equipos y escritorio digital."));

        crearProducto("Mochila escolar", "Capacidad 25L, multiples compartimentos.", 18, new BigDecimal("29.99"), List.of(categorias.get("Escolar")));
        crearProducto("Estuche doble", "Plastico resistente, 2 cremalleras.", 30, new BigDecimal("8.50"), List.of(categorias.get("Escolar")));
        crearProducto("Separadores A4", "12 pestanas, colores variados.", 45, new BigDecimal("5.99"), List.of(categorias.get("Oficina"), categorias.get("Organizacion")));
        crearProducto("Grapadora pesada", "Capacidad 100 hojas, metalica.", 12, new BigDecimal("19.95"), List.of(categorias.get("Oficina")));
        crearProducto("Lapices acuarela", "Set profesional 24 colores.", 16, new BigDecimal("22.50"), List.of(categorias.get("Arte"), categorias.get("Escolar")));
        crearProducto("Bloc dibujo", "Formato A4, 50h 200gr.", 22, new BigDecimal("12.99"), List.of(categorias.get("Arte"), categorias.get("Escolar")));
        crearProducto("Caja archivador", "Transfer resistente, 50 documentos.", 20, new BigDecimal("6.75"), List.of(categorias.get("Organizacion"), categorias.get("Oficina")));
        crearProducto("Tablero corcho", "60x40cm con marcos aluminio.", 9, new BigDecimal("18.90"), List.of(categorias.get("Organizacion")));
        crearProducto("Protector teclado", "Transparente, funda silicona.", 28, new BigDecimal("9.99"), List.of(categorias.get("Tecnologia")));
        crearProducto("Cable organizador", "Kit 10 brazaletes velcro.", 40, new BigDecimal("4.25"), List.of(categorias.get("Tecnologia"), categorias.get("Oficina")));
    }

    private Long crearCategoria(String nombre, String descripcion) {
        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        categoria.setDescripcion(descripcion);
        return servicioTienda.guardarCategoria(categoria).getId();
    }

    private void crearProducto(String nombre, String descripcion, int stock, BigDecimal precio, List<Long> categoriaIds) {
        Producto producto = new Producto();
        producto.setNombre(nombre);
        producto.setDescripcion(descripcion);
        producto.setStock(stock);
        producto.setPrecio(precio);
        Producto productoGuardado = servicioTienda.guardarProducto(producto);
        servicioTienda.asignarCategoriasAProducto(productoGuardado.getId(), categoriaIds);
    }
}
