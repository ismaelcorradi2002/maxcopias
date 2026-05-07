package com.maxcopias.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

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
    @Column(name = "tama\u00f1o")
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
    private String extras;

    @Column(name = "ruta_archivo", columnDefinition = "TEXT")
    private String rutaArchivo;

    @Column(name = "archivo_nombres", columnDefinition = "TEXT")
    private String archivoNombres;

    @Column(name = "archivo_tipos", columnDefinition = "TEXT")
    private String archivoTipos;

    @Column(name = "archivo_tamanos", columnDefinition = "TEXT")
    private String archivoTamanos;

    @Column(name = "archivo_paginas", columnDefinition = "TEXT")
    private String archivoPaginas;

    @Column(precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Convert(converter = EstadoPedidoCopisteriaConverter.class)
    @Column(nullable = false, length = 40)
    private EstadoPedidoCopisteria estado = EstadoPedidoCopisteria.PENDIENTE;

    @Column(name = "codigo_recoger", length = 12, unique = true)
    private String codigoRecoger;

    @Column(nullable = false)
    private boolean eliminado = false;

    @Column(name = "fecha_eliminacion")
    private LocalDateTime fechaEliminacion;

    @Column(name = "eliminado_por", length = 160)
    private String eliminadoPor;

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
    public Integer getCopies() { return copias; }

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
    public String getArchivoNombres() { return archivoNombres; }
    public void setArchivoNombres(String archivoNombres) { this.archivoNombres = archivoNombres; }
    public String getArchivoTipos() { return archivoTipos; }
    public void setArchivoTipos(String archivoTipos) { this.archivoTipos = archivoTipos; }
    public String getArchivoTamanos() { return archivoTamanos; }
    public void setArchivoTamanos(String archivoTamanos) { this.archivoTamanos = archivoTamanos; }
    public String getArchivoPaginas() { return archivoPaginas; }
    public void setArchivoPaginas(String archivoPaginas) { this.archivoPaginas = archivoPaginas; }

    public List<String> getRutasArchivo() {
        if (rutaArchivo == null || rutaArchivo.isBlank()) {
            return List.of();
        }

        return Arrays.stream(rutaArchivo.split("\\R"))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .collect(Collectors.toList());
    }

    public void setRutasArchivo(List<String> rutasArchivo) {
        if (rutasArchivo == null || rutasArchivo.isEmpty()) {
            this.rutaArchivo = null;
            return;
        }

        this.rutaArchivo = rutasArchivo.stream()
            .filter(path -> path != null && !path.isBlank())
            .collect(Collectors.joining("\n"));
    }

    public List<String> getNombresArchivo() {
        return splitLines(archivoNombres);
    }

    public void setNombresArchivo(List<String> nombresArchivo) {
        this.archivoNombres = joinLines(nombresArchivo);
    }

    public List<String> getTiposArchivo() {
        return splitLines(archivoTipos);
    }

    public void setTiposArchivo(List<String> tiposArchivo) {
        this.archivoTipos = joinLines(tiposArchivo);
    }

    public List<Long> getTamanosArchivoLista() {
        return splitLines(archivoTamanos).stream()
            .map(value -> {
                try {
                    return Long.parseLong(value);
                } catch (NumberFormatException exception) {
                    return null;
                }
            })
            .collect(Collectors.toList());
    }

    public void setTamanosArchivoLista(List<Long> tamanosArchivo) {
        this.archivoTamanos = joinLines(
            tamanosArchivo == null ? List.of() : tamanosArchivo.stream()
                .map(value -> value == null ? null : String.valueOf(value))
                .collect(Collectors.toList())
        );
    }

    public List<Integer> getPaginasArchivoLista() {
        return splitLines(archivoPaginas).stream()
            .map(value -> {
                try {
                    return Integer.parseInt(value);
                } catch (NumberFormatException exception) {
                    return null;
                }
            })
            .collect(Collectors.toList());
    }

    public void setPaginasArchivoLista(List<Integer> paginasArchivo) {
        this.archivoPaginas = joinLines(
            paginasArchivo == null ? List.of() : paginasArchivo.stream()
                .map(value -> value == null ? null : String.valueOf(value))
                .collect(Collectors.toList())
        );
    }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public EstadoPedidoCopisteria getEstado() { return estado; }
    public void setEstado(EstadoPedidoCopisteria estado) { this.estado = estado; }

    public String getCodigoRecoger() { return codigoRecoger; }
    public void setCodigoRecoger(String codigoRecoger) { this.codigoRecoger = codigoRecoger; }

    public boolean isEliminado() { return eliminado; }
    public void setEliminado(boolean eliminado) { this.eliminado = eliminado; }

    public LocalDateTime getFechaEliminacion() { return fechaEliminacion; }
    public void setFechaEliminacion(LocalDateTime fechaEliminacion) { this.fechaEliminacion = fechaEliminacion; }

    public String getEliminadoPor() { return eliminadoPor; }
    public void setEliminadoPor(String eliminadoPor) { this.eliminadoPor = eliminadoPor; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

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
        return String.format(Locale.forLanguageTag("es-ES"), "%.2f EUR", precio);
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
        return getRutasArchivo().size();
    }

    public String getNombreArchivo() {
        List<String> nombres = getNombresArchivo();
        if (!nombres.isEmpty()) {
            return nombres.get(0);
        }

        List<String> rutas = getRutasArchivo();
        if (rutas.isEmpty()) {
            return null;
        }
        return extraerNombreDesdeRuta(rutas.get(0));
    }

    public String getTamanoArchivoFormateado() {
        List<Long> tamanos = getTamanosArchivoLista();
        if (tamanos.isEmpty() || tamanos.get(0) == null) {
            return null;
        }
        return formatearTamano(tamanos.get(0));
    }

    public int getTotalPageCount() {
        return getPaginasArchivoLista().stream()
            .filter(value -> value != null && value > 0)
            .mapToInt(Integer::intValue)
            .sum();
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

    private List<String> splitLines(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }

        return Arrays.stream(value.split("\\R"))
            .map(String::trim)
            .filter(item -> !item.isBlank())
            .collect(Collectors.toList());
    }

    private String joinLines(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        List<String> filteredValues = values.stream()
            .filter(item -> item != null && !item.isBlank())
            .toList();
        return filteredValues.isEmpty() ? null : String.join("\n", filteredValues);
    }

    private String extraerNombreDesdeRuta(String ruta) {
        if (ruta == null || ruta.isBlank()) {
            return null;
        }

        try {
            if (ruta.startsWith("http://") || ruta.startsWith("https://")) {
                String path = URI.create(ruta).getPath();
                String nombre = Path.of(path).getFileName().toString();
                return URLDecoder.decode(nombre, StandardCharsets.UTF_8);
            }
            return Path.of(ruta).getFileName().toString();
        } catch (Exception exception) {
            return ruta;
        }
    }

    private String formatearTamano(long sizeInBytes) {
        if (sizeInBytes <= 0) {
            return null;
        }
        if (sizeInBytes < 1024) {
            return sizeInBytes + " B";
        }
        double sizeInKb = sizeInBytes / 1024.0;
        if (sizeInKb < 1024) {
            return String.format("%.1f KB", sizeInKb);
        }
        return String.format("%.2f MB", sizeInKb / 1024.0);
    }
}
