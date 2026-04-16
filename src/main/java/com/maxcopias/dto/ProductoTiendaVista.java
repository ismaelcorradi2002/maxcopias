package com.maxcopias.dto;

import java.util.List;

public record ProductoTiendaVista(
    Long id,
    String nombre,
    String descripcion,
    String precioFormateado,
    String imagenUrl,
    String alt,
    String categoriasTexto,
    String categoriasSlug,
    List<String> categorias,
    Integer stock,
    String detalleTitulo,
    String detalleDescripcion,
    List<String> detallePuntos
) {
}
