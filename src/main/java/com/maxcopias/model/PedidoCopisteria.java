package com.maxcopias.model;

/**
 * Entidad principal de pedido de copistería.
 */
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Entity
@Table(name = "copisteria_orders", uniqueConstraints = {
    @UniqueConstraint(name = "uk_copisteria_orders_pickup_code", columnNames = "pickup_code")
})
public class PedidoCopisteria {

    private static final DateTimeFormatter CREATED_AT_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pickup_code", nullable = false, length = 20)
    private String pickupCode;

    @Column(name = "customer_name", nullable = false, length = 80)
    private String customerName;

    @Column(nullable = false, length = 25)
    private String phone;

    @Column(nullable = false, length = 120)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 40)
    private TipoTrabajo jobType;

    @Column
    private Integer copies;

    @Enumerated(EnumType.STRING)
    @Column(name = "color_mode", length = 30)
    private ModoColor colorMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "print_side", length = 30)
    private CaraImpresion printSide;

    @Enumerated(EnumType.STRING)
    @Column(name = "paper_size", length = 20)
    private TamanoPapel paperSize;

    @Column(length = 600)
    private String observations;

    @Column(name = "estimated_price", precision = 10, scale = 2)
    private BigDecimal estimatedPrice;

    @Column(name = "price_breakdown", length = 240)
    private String priceBreakdown;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoPedidoCopisteria status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Usuario user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ArchivoPedidoCopisteria> files = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = EstadoPedidoCopisteria.RECIBIDO;
        }
    }

    public void addFile(ArchivoPedidoCopisteria file) {
        files.add(file);
        file.setOrder(this);
    }

    public boolean hasObservations() {
        return observations != null && !observations.isBlank();
    }

    public String getObservationsOrDefault() {
        return hasObservations() ? observations : "Sin observaciones.";
    }

    public String getPriceBreakdownOrDefault() {
        return priceBreakdown != null && !priceBreakdown.isBlank()
            ? priceBreakdown
            : "Precio pendiente de calcular.";
    }

    public int getFileCount() {
        return files.size();
    }

    public int getTotalPageCount() {
        return files.stream()
            .mapToInt(ArchivoPedidoCopisteria::getPageCount)
            .sum();
    }

    public boolean hasPrintConfiguration() {
        return jobType != null && jobType.isRequiresPrintConfiguration();
    }

    public String getFormattedCreatedAt() {
        return createdAt.format(CREATED_AT_FORMAT);
    }

    public String getFormattedEstimatedPrice() {
        if (estimatedPrice == null) {
            return "Pendiente";
        }

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "ES"));
        return currencyFormat.format(estimatedPrice);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPickupCode() {
        return pickupCode;
    }

    public void setPickupCode(String pickupCode) {
        this.pickupCode = pickupCode;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public TipoTrabajo getTipoTrabajo() {
        return jobType;
    }

    public void setTipoTrabajo(TipoTrabajo jobType) {
        this.jobType = jobType;
    }

    public TipoTrabajo getJobType() {
        return jobType;
    }

    public void setJobType(TipoTrabajo jobType) {
        this.jobType = jobType;
    }

    public Integer getCopies() {
        return copies;
    }

    public void setCopies(Integer copies) {
        this.copies = copies;
    }

    public ModoColor getModoColor() {
        return colorMode;
    }

    public void setModoColor(ModoColor colorMode) {
        this.colorMode = colorMode;
    }

    public ModoColor getColorMode() {
        return colorMode;
    }

    public void setColorMode(ModoColor colorMode) {
        this.colorMode = colorMode;
    }

    public CaraImpresion getCaraImpresion() {
        return printSide;
    }

    public void setCaraImpresion(CaraImpresion printSide) {
        this.printSide = printSide;
    }

    public CaraImpresion getPrintSide() {
        return printSide;
    }

    public void setPrintSide(CaraImpresion printSide) {
        this.printSide = printSide;
    }

    public TamanoPapel getPaperSize() {
        return paperSize;
    }

    public void setPaperSize(TamanoPapel paperSize) {
        this.paperSize = paperSize;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }

    public BigDecimal getEstimatedPrice() {
        return estimatedPrice;
    }

    public void setEstimatedPrice(BigDecimal estimatedPrice) {
        this.estimatedPrice = estimatedPrice;
    }

    public String getPriceBreakdown() {
        return priceBreakdown;
    }

    public void setPriceBreakdown(String priceBreakdown) {
        this.priceBreakdown = priceBreakdown;
    }

    public EstadoPedidoCopisteria getStatus() {
        return status;
    }

    public void setStatus(EstadoPedidoCopisteria status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Usuario getUsuario() {
        return user;
    }

    public void setUsuario(Usuario user) {
        this.user = user;
    }

    public Usuario getUser() {
        return user;
    }

    public void setUser(Usuario user) {
        this.user = user;
    }

    public List<ArchivoPedidoCopisteria> getFiles() {
        return files;
    }

    public void setFiles(List<ArchivoPedidoCopisteria> files) {
        this.files = files;
    }
}

