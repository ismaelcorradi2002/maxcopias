package com.maxcopias.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "ofertas")
public class Oferta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 140)
    private String titulo;

    @Column(nullable = false, length = 700)
    private String descripcion;

    @Column(name = "precio_descuento", length = 80)
    private String precioDescuento;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_oferta", nullable = false, length = 30)
    private TipoOferta tipoOferta = TipoOferta.GLOBAL;

    @Column(name = "porcentaje_descuento", nullable = false)
    private Integer porcentajeDescuento = 10;

    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private Boolean activa = true;

    @Column(name = "principal_home", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private Boolean principalHome = false;

    @ManyToOne
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @ManyToMany
    @JoinTable(
        name = "oferta_producto",
        joinColumns = @JoinColumn(name = "oferta_id"),
        inverseJoinColumns = @JoinColumn(name = "producto_id")
    )
    private Set<Producto> productos = new LinkedHashSet<>();

    @ManyToOne
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
        if (activa == null) {
            activa = true;
        }
        if (principalHome == null) {
            principalHome = false;
        }
        if (tipoOferta == null) {
            tipoOferta = TipoOferta.GLOBAL;
        }
        if (porcentajeDescuento == null) {
            porcentajeDescuento = 10;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getPrecioDescuento() {
        return precioDescuento;
    }

    public void setPrecioDescuento(String precioDescuento) {
        this.precioDescuento = precioDescuento;
    }

    public TipoOferta getTipoOferta() {
        return tipoOferta;
    }

    public void setTipoOferta(TipoOferta tipoOferta) {
        this.tipoOferta = tipoOferta;
    }

    public Integer getPorcentajeDescuento() {
        return porcentajeDescuento;
    }

    public void setPorcentajeDescuento(Integer porcentajeDescuento) {
        this.porcentajeDescuento = porcentajeDescuento;
    }

    public Boolean getActiva() {
        return activa;
    }

    public void setActiva(Boolean activa) {
        this.activa = activa;
    }

    public boolean isActiva() {
        return Boolean.TRUE.equals(activa);
    }

    public Boolean getPrincipal() {
        return principalHome;
    }

    public void setPrincipal(Boolean principal) {
        this.principalHome = principal;
    }

    public boolean isPrincipal() {
        return Boolean.TRUE.equals(principalHome);
    }

    public Boolean getPrincipalHome() {
        return principalHome;
    }

    public void setPrincipalHome(Boolean principalHome) {
        this.principalHome = principalHome;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public Set<Producto> getProductos() {
        return productos;
    }

    public void setProductos(Set<Producto> productos) {
        this.productos = productos == null ? new LinkedHashSet<>() : productos;
    }

    public void clearProductos() {
        productos.clear();
    }

    public void addProducto(Producto producto) {
        if (producto != null) {
            productos.add(producto);
        }
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }

    public String getImagenVisualUrl() {
        if (imagenUrl != null && !imagenUrl.isBlank()) {
            return imagenUrl;
        }

        if (producto != null && producto.tieneImagen()) {
            return producto.getImagenUrl();
        }

        return productos == null ? null : productos.stream()
            .filter(Producto::tieneImagen)
            .sorted(Comparator.comparing(Producto::getId, Comparator.nullsLast(Long::compareTo)))
            .map(Producto::getImagenUrl)
            .findFirst()
            .orElse(null);
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public boolean isVigente(LocalDate fecha) {
        if (!Boolean.TRUE.equals(activa)) {
            return false;
        }
        LocalDate hoy = fecha == null ? LocalDate.now() : fecha;
        boolean empieza = fechaInicio == null || !hoy.isBefore(fechaInicio);
        boolean noTermino = fechaFin == null || !hoy.isAfter(fechaFin);
        return empieza && noTermino;
    }
}
