package com.maxcopias.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "productos", uniqueConstraints = {
    @UniqueConstraint(name = "uk_productos_nombre", columnNames = "nombre")
})
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 140)
    private String nombre;

    @Column(nullable = false, length = 1000)
    private String descripcion;

    @Column(nullable = false)
    private Integer stock;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "producto_categoria",
        joinColumns = @JoinColumn(name = "producto_id", nullable = false),
        inverseJoinColumns = @JoinColumn(name = "categoria_id", nullable = false)
    )
    @JsonIgnoreProperties("productos")
    private Set<Categoria> categorias = new LinkedHashSet<>();

    public void addCategoria(Categoria categoria) {
        if (categoria == null) {
            return;
        }
        categorias.add(categoria);
        categoria.getProductos().add(this);
    }

    public void clearCategorias() {
        for (Categoria categoria : categorias) {
            categoria.getProductos().remove(this);
        }
        categorias.clear();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Set<Categoria> getCategorias() {
        return categorias;
    }

    public void setCategorias(Set<Categoria> categorias) {
        this.categorias = categorias;
    }
}
