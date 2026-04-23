package com.maxcopias.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad para pedidos de copisteria.
 */
@Entity
@Table(name = "pedidos_copisteria")
public class PedidoCopisteria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String customerName;

    @Column(name = "phone", length = 20, nullable = false)
    private String phone;

    @Column(nullable = false, length = 120)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "trabajo", nullable = false)
    private TipoTrabajo trabajo;

    @Column(nullable = false)
    private Integer copias;

    @Enumerated(EnumType.STRING)
    @Column(name = "color")
    private ModoColor color;

    @Enumerated(EnumType.STRING)
    @Column(name = "tamaño")
    private TamanoPapel tamano;

    @Enumerated(EnumType.STRING)
    @Column(name = "caras")
    private CaraImpresion caras;

    @Enumerated(EnumType.STRING)
    @Column(name = "papel")
    private TipoPapel papel;

    @Enumerated(EnumType.STRING)
    @Column(name = "encuadernacion")
    private TipoEncuadernacion encuadernacion;

    @Column(columnDefinition = "TEXT")
    private String extras;  // JSON or comma-separated: plastificado, urgente, etc.

    @Column(name = "ruta_archivo", length = 500)
    private String rutaArchivo;

    @Column(precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPedidoCopisteria estado = EstadoPedidoCopisteria.RECIBIDO;

    @Column(name = "codigo_recoger", length = 12, unique = true)
    private String codigoRecoger;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
        if (codigoRecoger == null) {
            codigoRecoger = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public TipoTrabajo getTrabajo() { return trabajo; }
    public void setTrabajo(TipoTrabajo trabajo) { this.trabajo = trabajo; }

    public Integer getCopias() { return copias; }
    public void setCopias(Integer copias) { this.copias = copias; }

    public ModoColor getColor() { return color; }
    public void setColor(ModoColor color) { this.color = color; }

    public TamanoPapel getTamano() { return tamano; }
    public void setTamano(TamanoPapel tamano) { this.tamano = tamano; }

    public CaraImpresion getCaras() { return caras; }
    public void setCaras(CaraImpresion caras) { this.caras = caras; }

    public TipoPapel getPapel() { return papel; }
    public void setPapel(TipoPapel papel) { this.papel = papel; }

    public TipoEncuadernacion getEncuadernacion() { return encuadernacion; }
    public void setEncuadernacion(TipoEncuadernacion encuadernacion) { this.encuadernacion = encuadernacion; }

    public String getExtras() { return extras; }
    public void setExtras(String extras) { this.extras = extras; }

    public String getRutaArchivo() { return rutaArchivo; }
    public void setRutaArchivo(String rutaArchivo) { this.rutaArchivo = rutaArchivo; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public EstadoPedidoCopisteria getEstado() { return estado; }
    public void setEstado(EstadoPedidoCopisteria estado) { this.estado = estado; }

    public String getCodigoRecoger() { return codigoRecoger; }
    public void setCodigoRecoger(String codigoRecoger) { this.codigoRecoger = codigoRecoger; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    // Helper aliases for Thymeleaf templates
    public EstadoPedidoCopisteria getStatus() { return estado; }
    public TipoTrabajo getJobType() { return trabajo; }
    public ModoColor getColorMode() { return color; }
    public TamanoPapel getPaperSize() { return tamano; }
    public CaraImpresion getPrintSide() { return caras; }
    public TipoPapel getPaperType() { return papel; }
    public TipoEncuadernacion getBindingType() { return encuadernacion; }
    public String getPickupCode() { return codigoRecoger; }

    public String getFormattedEstimatedPrice() {
        if (precio == null) {
            return "0,00 EUR";
        }
        return String.format(java.util.Locale.forLanguageTag("es-ES"), "%.2f EUR", precio);
    }

    public String getPriceBreakdownOrDefault() {
        StringBuilder sb = new StringBuilder();
        if (trabajo != null) {
            sb.append(trabajo.getLabel());
        }
        if (color != null) {
            sb.append(" • ").append(color.getLabel());
        }
        if (tamano != null) {
            sb.append(" • ").append(tamano.getLabel());
        }
        if (copias != null) {
            sb.append(" • ").append(copias).append(" copia(s)");
        }
        if (caras != null) {
            sb.append(" • ").append(caras.getLabel());
        }
        if (papel != null) {
            sb.append(" • ").append(papel.getLabel());
        }
        if (sb.length() == 0) {
            return "Pedido guardado";
        }
        return sb.toString();
    }

    public String getFormattedCreatedAt() {
        if (fechaCreacion == null) {
            return "";
        }
        return fechaCreacion.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    public int getFileCount() {
        if (rutaArchivo == null || rutaArchivo.isBlank()) {
            return 0;
        }
        return 1; // simplistic; could parse stored files if needed
    }

    public int getTotalPageCount() {
        return 0; // not tracked separately without file inspection service
    }

    public String getExtrasSummary() {
        if (extras == null || extras.isBlank()) {
            return "Sin extras";
        }
        return extras;
    }

    public String getObservationsOrDefault() {
        if (extras == null || extras.isBlank()) {
            return "Sin observaciones.";
        }
        return extras;
    }
}

