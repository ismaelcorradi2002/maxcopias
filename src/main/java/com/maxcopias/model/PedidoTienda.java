package com.maxcopias.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedidos_tienda")
public class PedidoTienda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_nombre", nullable = false, length = 160)
    private String clienteNombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "codigo_pedido", length = 32)
    private String codigoPedido;

    @Column(nullable = false, length = 140)
    private String email;

    @Column(length = 20)
    private String telefono;

    @Column(name = "resumen_productos", columnDefinition = "TEXT")
    private String resumenProductos;

    @Column(precision = 10, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "gastos_envio", precision = 10, scale = 2)
    private BigDecimal gastosEnvio = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Convert(converter = EstadoPedidoTiendaConverter.class)
    @Column(nullable = false, length = 30)
    private EstadoPedidoTienda estado = EstadoPedidoTienda.PENDIENTE;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_entrega", length = 30)
    private MetodoEntregaPedidoTienda metodoEntrega = MetodoEntregaPedidoTienda.RECOGIDA_TIENDA;

    @Column(nullable = false)
    private boolean pagado = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", length = 30)
    private MetodoPagoPedidoTienda metodoPago;

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    @Column(nullable = false)
    private boolean eliminado = false;

    @Column(name = "fecha_eliminacion")
    private LocalDateTime fechaEliminacion;

    @Column(name = "eliminado_por", length = 160)
    private String eliminadoPor;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PedidoItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
        if (estado == null) {
            estado = EstadoPedidoTienda.PENDIENTE;
        }
        if (total == null) {
            total = BigDecimal.ZERO;
        }
        if (subtotal == null) {
            subtotal = total == null ? BigDecimal.ZERO : total;
        }
        if (gastosEnvio == null) {
            gastosEnvio = BigDecimal.ZERO;
        }
        if (metodoEntrega == null) {
            metodoEntrega = MetodoEntregaPedidoTienda.RECOGIDA_TIENDA;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getClienteNombre() {
        return clienteNombre;
    }

    public void setClienteNombre(String clienteNombre) {
        this.clienteNombre = clienteNombre;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getCodigoPedido() {
        return codigoPedido;
    }

    public void setCodigoPedido(String codigoPedido) {
        this.codigoPedido = codigoPedido;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getResumenProductos() {
        return resumenProductos;
    }

    public void setResumenProductos(String resumenProductos) {
        this.resumenProductos = resumenProductos;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getGastosEnvio() {
        return gastosEnvio;
    }

    public void setGastosEnvio(BigDecimal gastosEnvio) {
        this.gastosEnvio = gastosEnvio;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public EstadoPedidoTienda getEstado() {
        return estado;
    }

    public void setEstado(EstadoPedidoTienda estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public MetodoEntregaPedidoTienda getMetodoEntrega() {
        return metodoEntrega;
    }

    public void setMetodoEntrega(MetodoEntregaPedidoTienda metodoEntrega) {
        this.metodoEntrega = metodoEntrega;
    }

    public boolean isPagado() {
        return pagado;
    }

    public void setPagado(boolean pagado) {
        this.pagado = pagado;
    }

    public MetodoPagoPedidoTienda getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(MetodoPagoPedidoTienda metodoPago) {
        this.metodoPago = metodoPago;
    }

    public LocalDateTime getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDateTime fechaPago) {
        this.fechaPago = fechaPago;
    }

    public boolean isEliminado() {
        return eliminado;
    }

    public void setEliminado(boolean eliminado) {
        this.eliminado = eliminado;
    }

    public LocalDateTime getFechaEliminacion() {
        return fechaEliminacion;
    }

    public void setFechaEliminacion(LocalDateTime fechaEliminacion) {
        this.fechaEliminacion = fechaEliminacion;
    }

    public String getEliminadoPor() {
        return eliminadoPor;
    }

    public void setEliminadoPor(String eliminadoPor) {
        this.eliminadoPor = eliminadoPor;
    }

    public List<PedidoItem> getItems() {
        return items;
    }

    public void setItems(List<PedidoItem> items) {
        this.items = items;
    }
}
