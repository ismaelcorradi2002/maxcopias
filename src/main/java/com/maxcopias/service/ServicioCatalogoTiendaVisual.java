package com.maxcopias.service;

import com.maxcopias.dto.CategoriaTiendaVista;
import com.maxcopias.dto.ProductoTiendaVista;
import com.maxcopias.dto.ResultadoOfertaProducto;
import com.maxcopias.model.Categoria;
import com.maxcopias.model.Producto;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ServicioCatalogoTiendaVisual {

    private static final Locale LOCALE_ES = new Locale("es", "ES");

    private final Map<String, MetadatosProductoTienda> catalogoVisual = crearCatalogoVisual();
    private final ServicioOferta servicioOferta;

    public ServicioCatalogoTiendaVisual(ServicioOferta servicioOferta) {
        this.servicioOferta = servicioOferta;
    }

    public List<CategoriaTiendaVista> mapearCategorias(List<Categoria> categorias) {
        return categorias.stream()
            .map(categoria -> new CategoriaTiendaVista(
                categoria.getId(),
                categoria.getNombre(),
                generarSlug(categoria.getNombre())
            ))
            .toList();
    }

    public List<ProductoTiendaVista> mapearProductos(List<Producto> productos) {
        return productos.stream()
            .map(this::mapearProducto)
            .toList();
    }

    public List<CategoriaTiendaVista> obtenerCategoriasVisibles(List<Producto> productos) {
        Set<Categoria> categoriasVisibles = new LinkedHashSet<>();
        for (Producto producto : productos) {
            categoriasVisibles.addAll(producto.getCategorias());
        }
        return mapearCategorias(categoriasVisibles.stream().toList());
    }

    public ProductoTiendaVista mapearProducto(Producto producto) {
        MetadatosProductoTienda metadatos = catalogoVisual.getOrDefault(
            producto.getNombre(),
            MetadatosProductoTienda.porDefecto(producto.getNombre(), producto.getDescripcion())
        );

        List<String> categorias = producto.getCategorias().stream()
            .map(Categoria::getNombre)
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();

        String categoriasSlug = producto.getCategorias().stream()
            .map(Categoria::getNombre)
            .map(this::generarSlug)
            .sorted()
            .collect(Collectors.joining(","));

        ResultadoOfertaProducto resultadoOferta = servicioOferta.calcularOfertaParaProducto(producto);

        return new ProductoTiendaVista(
            producto.getId(),
            producto.getNombre(),
            producto.getDescripcion(),
            resultadoOferta.aplicable() ? formatearPrecio(resultadoOferta.precioFinal()) : formatearPrecio(producto),
            resolverImagenProducto(producto, metadatos),
            producto.tieneImagen() ? producto.getNombre() : metadatos.alt(),
            categorias.isEmpty() ? "Sin categoria" : String.join(" · ", categorias),
            categoriasSlug,
            categorias,
            producto.getStock(),
            metadatos.detalleTitulo(),
            metadatos.detalleDescripcion(),
            metadatos.detallePuntos(),
            resultadoOferta.aplicable(),
            formatearPrecio(resultadoOferta.precioOriginal()),
            formatearPrecio(resultadoOferta.precioFinal()),
            resultadoOferta.aplicable() ? resultadoOferta.porcentajeDescuento() + "% OFF" : ""
        );
    }

    public String generarSlug(String valor) {
        if (valor == null) {
            return "";
        }

        return valor
            .toLowerCase(LOCALE_ES)
            .replace("á", "a")
            .replace("é", "e")
            .replace("í", "i")
            .replace("ó", "o")
            .replace("ú", "u")
            .replace("ñ", "n")
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-|-$)", "");
    }

    private String formatearPrecio(Producto producto) {
        return NumberFormat.getCurrencyInstance(LOCALE_ES).format(producto.getPrecio());
    }

    private String formatearPrecio(BigDecimal precio) {
        return NumberFormat.getCurrencyInstance(LOCALE_ES).format(precio);
    }

    private String resolverImagenProducto(Producto producto, MetadatosProductoTienda metadatos) {
        return producto.tieneImagen() ? producto.getImagenUrl() : metadatos.imagenUrl();
    }

    private Map<String, MetadatosProductoTienda> crearCatalogoVisual() {
        Map<String, MetadatosProductoTienda> datos = new LinkedHashMap<>();

        datos.put("Mochila escolar", new MetadatosProductoTienda(
            "https://images.unsplash.com/photo-1581605405669-fcdf81165afa?q=80&w=774&auto=format&fit=crop",
            "Mochila escolar reforzada",
            "Mochila escolar reforzada",
            "La mochila perfecta para el dia a dia escolar.",
            List.of(
                "Capacidad 25L con multiples compartimentos",
                "Material resistente al agua",
                "Asas ergonomicas acolchadas",
                "Disponible para recogida en tienda"
            )
        ));
        datos.put("Estuche doble", new MetadatosProductoTienda(
            "https://images.unsplash.com/photo-1567634088512-20ec1da1e1a5?q=80&w=1548&auto=format&fit=crop",
            "Estuche doble para material escolar",
            "Estuche doble con doble cremallera",
            "Solucion compacta para llevar boligrafos, rotuladores y pequenos accesorios.",
            List.of(
                "Dos compartimentos independientes",
                "Cremalleras resistentes",
                "Formato practico para mochila o escritorio"
            )
        ));
        datos.put("Separadores A4", new MetadatosProductoTienda(
            "https://multimedia.dideco.es/img/papeleria/EAN_8422951051238-5.jpg",
            "Separadores A4 de colores",
            "Separadores A4 de colores",
            "Ideales para clasificar apuntes, documentos y archivadores por materias.",
            List.of(
                "Pack de 12 pestanas",
                "Compatibles con archivadores A4",
                "Uso practico para estudio y oficina"
            )
        ));
        datos.put("Grapadora pesada", new MetadatosProductoTienda(
            "https://images.unsplash.com/photo-1559743341-7fef133c7c6a?q=80&w=1570&auto=format&fit=crop",
            "Grapadora metalica de gran capacidad",
            "Grapadora metalica de alta capacidad",
            "Pensada para oficina y tareas que requieren grapado intensivo.",
            List.of(
                "Capacidad hasta 100 hojas",
                "Estructura metalica resistente",
                "Apta para uso frecuente"
            )
        ));
        datos.put("Lapices acuarela", new MetadatosProductoTienda(
            "https://m.media-amazon.com/images/I/61xNnFt-2QL.jpg",
            "Lapices acuarelables de 24 colores",
            "Set de lapices acuarelables",
            "Set versatil para dibujo, ilustracion y trabajos creativos.",
            List.of(
                "24 colores intensos",
                "Trazo suave y facil de difuminar",
                "Perfectos para arte y manualidades"
            )
        ));
        datos.put("Bloc dibujo", new MetadatosProductoTienda(
            "https://plus.unsplash.com/premium_photo-1683309559481-f5b07f07774e?q=80&w=774&auto=format&fit=crop",
            "Bloc de dibujo A4 de alto gramaje",
            "Bloc de dibujo A4",
            "Bloc pensado para bocetos, laminas y tecnicas secas o mixtas.",
            List.of(
                "Formato A4",
                "50 hojas de 200 g",
                "Buena rigidez para trabajos escolares y artisticos"
            )
        ));
        datos.put("Caja archivador", new MetadatosProductoTienda(
            "https://lasuperpapeleria.com//imagenes_grandes/3130631/313063189560.JPG",
            "Caja archivador para organizacion",
            "Caja archivador resistente",
            "Una opcion practica para guardar documentos, apuntes o proyectos completos.",
            List.of(
                "Gran capacidad de almacenaje",
                "Material resistente",
                "Pensada para oficina y estudio"
            )
        ));
        datos.put("Tablero corcho", new MetadatosProductoTienda(
            "https://moldiber.com/2658-thickbox_default/perfil-auxiliar-de-aluminio-a-medida-modelo-20.webp",
            "Tablero de corcho con marco",
            "Tablero de corcho 60x40",
            "Ideal para notas, recordatorios y organizacion visual de tareas.",
            List.of(
                "Medida 60 x 40 cm",
                "Marco resistente",
                "Muy util para estudio y despacho"
            )
        ));
        datos.put("Protector teclado", new MetadatosProductoTienda(
            "https://m.media-amazon.com/images/I/717phNvKCVS._AC_UF1000,1000_QL80_.jpg",
            "Protector de teclado transparente",
            "Protector de teclado de silicona",
            "Protege el teclado del polvo y del uso diario manteniendo una escritura comoda.",
            List.of(
                "Silicona flexible",
                "Acabado transparente",
                "Facil de limpiar"
            )
        ));
        datos.put("Cable organizador", new MetadatosProductoTienda(
            "https://m.media-amazon.com/images/I/71YTA1pw9CL.jpg",
            "Kit de organizacion de cables",
            "Kit organizador de cables",
            "Pack sencillo para mantener cables ordenados en casa, oficina o zona de estudio.",
            List.of(
                "Incluye 10 piezas",
                "Cierre de velcro reutilizable",
                "Muy util para escritorio y tecnologia"
            )
        ));

        return datos;
    }

    private record MetadatosProductoTienda(
        String imagenUrl,
        String alt,
        String detalleTitulo,
        String detalleDescripcion,
        List<String> detallePuntos
    ) {
        private static MetadatosProductoTienda porDefecto(String nombre, String descripcion) {
            return new MetadatosProductoTienda(
                "/images/placeholders/product-placeholder-card.svg",
                nombre,
                nombre,
                descripcion,
                List.of(
                    "Disponible para recogida en tienda",
                    "Consulta stock y precio actualizado en Maxcopias"
                )
            );
        }
    }
}
