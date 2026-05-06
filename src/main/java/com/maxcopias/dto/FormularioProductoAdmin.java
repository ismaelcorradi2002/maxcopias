package com.maxcopias.dto;

import com.maxcopias.model.Producto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class FormularioProductoAdmin {

    @NotBlank(message = "Introduce un nombre para el producto.")
    @Size(max = 140, message = "El nombre no puede superar 140 caracteres.")
    private String nombre;

    @NotBlank(message = "Introduce una descripcion para el producto.")
    @Size(max = 1000, message = "La descripcion no puede superar 1000 caracteres.")
    private String descripcion;

    @NotNull(message = "Indica el stock.")
    @Min(value = 0, message = "El stock no puede ser negativo.")
    private Integer stock;

    @NotNull(message = "Indica el precio.")
    @DecimalMin(value = "0.0", inclusive = true, message = "El precio no puede ser negativo.")
    private BigDecimal precio;

    @NotNull(message = "Selecciona una categoria principal.")
    private Long categoriaId;

    private Long categoriaOpcionalId;

    private boolean eliminarImagen;

    public static FormularioProductoAdmin desdeProducto(Producto producto) {
        FormularioProductoAdmin formulario = new FormularioProductoAdmin();
        formulario.setNombre(producto.getNombre());
        formulario.setDescripcion(producto.getDescripcion());
        formulario.setStock(producto.getStock());
        formulario.setPrecio(producto.getPrecio());

        Long categoriaPrincipal = producto.getCategorias().stream()
            .findFirst()
            .map(categoria -> categoria.getId())
            .orElse(null);
        formulario.setCategoriaId(categoriaPrincipal);

        Long categoriaSecundaria = producto.getCategorias().stream()
            .skip(1)
            .findFirst()
            .map(categoria -> categoria.getId())
            .orElse(null);
        formulario.setCategoriaOpcionalId(categoriaSecundaria);
        return formulario;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public Long getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(Long categoriaId) {
        this.categoriaId = categoriaId;
    }

    public Long getCategoriaOpcionalId() {
        return categoriaOpcionalId;
    }

    public void setCategoriaOpcionalId(Long categoriaOpcionalId) {
        this.categoriaOpcionalId = categoriaOpcionalId;
    }

    public boolean isEliminarImagen() {
        return eliminarImagen;
    }

    public void setEliminarImagen(boolean eliminarImagen) {
        this.eliminarImagen = eliminarImagen;
    }
}
