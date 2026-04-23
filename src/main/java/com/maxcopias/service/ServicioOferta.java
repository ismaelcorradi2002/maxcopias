package com.maxcopias.service;

import com.maxcopias.dto.ResultadoOfertaProducto;
import com.maxcopias.model.Categoria;
import com.maxcopias.model.Oferta;
import com.maxcopias.model.Producto;
import com.maxcopias.model.TipoOferta;
import com.maxcopias.repository.RepositorioOferta;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ServicioOferta {

    private final RepositorioOferta repositorioOferta;

    public ServicioOferta(RepositorioOferta repositorioOferta) {
        this.repositorioOferta = repositorioOferta;
    }

    @Transactional(readOnly = true)
    public List<Oferta> obtenerTodas() {
        return repositorioOferta.findAllByOrderByFechaCreacionDesc();
    }

    @Transactional(readOnly = true)
    public List<Oferta> obtenerActivas() {
        return repositorioOferta.findAllByActivaTrueOrderByFechaCreacionDesc().stream()
            .filter(oferta -> oferta.isVigente(LocalDate.now()))
            .toList();
    }

    @Transactional(readOnly = true)
    public Optional<Oferta> obtenerOfertaPrincipalActiva() {
        return repositorioOferta.findFirstByActivaTrueAndPrincipalHomeTrueOrderByFechaCreacionDesc()
            .filter(oferta -> oferta.isVigente(LocalDate.now()))
            .or(() -> obtenerActivas().stream().findFirst());
    }

    @Transactional(readOnly = true)
    public Optional<Oferta> obtenerMejorOfertaParaProducto(Producto producto) {
        if (producto == null) {
            return Optional.empty();
        }

        return obtenerActivas().stream()
            .filter(oferta -> ofertaAplicaAProducto(oferta, producto))
            .min(
                Comparator.comparingInt(this::prioridadOferta)
                    .thenComparing(
                        Oferta::getFechaCreacion,
                        Comparator.nullsLast(Comparator.reverseOrder())
                    )
            );
    }

    @Transactional(readOnly = true)
    public ResultadoOfertaProducto calcularOfertaParaProducto(Producto producto) {
        BigDecimal precioOriginal = producto == null || producto.getPrecio() == null
            ? BigDecimal.ZERO
            : producto.getPrecio();

        Optional<Oferta> mejorOferta = obtenerMejorOfertaParaProducto(producto);

        if (mejorOferta.isEmpty()) {
            return new ResultadoOfertaProducto(null, precioOriginal, precioOriginal, 0, false);
        }

        Oferta oferta = mejorOferta.get();
        int porcentaje = normalizarPorcentaje(oferta.getPorcentajeDescuento());
        BigDecimal multiplicador = BigDecimal.valueOf(100L - porcentaje)
            .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal precioFinal = precioOriginal.multiply(multiplicador).setScale(2, RoundingMode.HALF_UP);

        return new ResultadoOfertaProducto(oferta, precioOriginal, precioFinal, porcentaje, true);
    }

    @Transactional(readOnly = true)
    public Oferta obtenerObligatoria(Long id) {
        return repositorioOferta.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("No existe la oferta indicada."));
    }

    @Transactional
    public Oferta guardar(Oferta oferta) {
        oferta.setTitulo(normalizar(oferta.getTitulo()));
        oferta.setDescripcion(normalizar(oferta.getDescripcion()));
        oferta.setImagenUrl(normalizar(oferta.getImagenUrl()));
        oferta.setActiva(oferta.getActiva() == null ? true : oferta.getActiva());
        oferta.setPrincipal(oferta.getPrincipal() == null ? false : oferta.getPrincipal());
        oferta.setTipoOferta(oferta.getTipoOferta() == null ? TipoOferta.GLOBAL : oferta.getTipoOferta());
        oferta.setPorcentajeDescuento(normalizarPorcentaje(oferta.getPorcentajeDescuento()));
        oferta.setPrecioDescuento(oferta.getPorcentajeDescuento() + "% OFF");

        if (oferta.getTipoOferta() == TipoOferta.PRODUCTO) {
            oferta.setCategoria(null);
        } else if (oferta.getTipoOferta() == TipoOferta.CATEGORIA) {
            oferta.setProducto(null);
            oferta.clearProductos();
        } else {
            oferta.setProducto(null);
            oferta.setCategoria(null);
            oferta.clearProductos();
        }

        validarDestinoOferta(oferta);
        validarOfertaDuplicada(oferta);

        if (Boolean.TRUE.equals(oferta.getPrincipal())) {
            desmarcarOfertasPrincipales(oferta.getId());
        }

        return repositorioOferta.save(oferta);
    }

    @Transactional
    public void cambiarActiva(Long id, boolean activa) {
        Oferta oferta = obtenerObligatoria(id);
        oferta.setActiva(activa);
        if (!activa) {
            oferta.setPrincipal(false);
        }
        repositorioOferta.save(oferta);
    }

    @Transactional
    public void marcarPrincipal(Long id) {
        Oferta oferta = obtenerObligatoria(id);
        desmarcarOfertasPrincipales(id);
        oferta.setActiva(true);
        oferta.setPrincipal(true);
        repositorioOferta.save(oferta);
    }

    public Oferta nuevaOfertaBase() {
        Oferta oferta = new Oferta();
        oferta.setActiva(true);
        oferta.setPrincipal(false);
        oferta.setTipoOferta(TipoOferta.GLOBAL);
        oferta.setPorcentajeDescuento(10);
        oferta.setPrecioDescuento("10% OFF");
        oferta.setFechaInicio(LocalDate.now());
        return oferta;
    }

    private boolean ofertaAplicaAProducto(Oferta oferta, Producto producto) {
        if (oferta.getTipoOferta() == TipoOferta.PRODUCTO) {
            boolean aplicaPorProductosMultiples = oferta.getProductos() != null && oferta.getProductos().stream()
                .anyMatch(productoOferta -> productoOferta.getId().equals(producto.getId()));
            boolean aplicaPorProductoLegacy = oferta.getProducto() != null && oferta.getProducto().getId().equals(producto.getId());
            return aplicaPorProductosMultiples || aplicaPorProductoLegacy;
        }

        if (oferta.getTipoOferta() == TipoOferta.CATEGORIA) {
            Categoria categoriaOferta = oferta.getCategoria();
            return categoriaOferta != null && producto.getCategorias().stream()
                .anyMatch(categoria -> categoriaOferta.getId().equals(categoria.getId()));
        }

        return oferta.getTipoOferta() == TipoOferta.GLOBAL;
    }

    private int prioridadOferta(Oferta oferta) {
        if (oferta.getTipoOferta() == TipoOferta.PRODUCTO) {
            return 1;
        }
        if (oferta.getTipoOferta() == TipoOferta.CATEGORIA) {
            return 2;
        }
        return 3;
    }

    private void validarDestinoOferta(Oferta oferta) {
        if (oferta.getTipoOferta() == TipoOferta.PRODUCTO
            && oferta.getProducto() == null
            && (oferta.getProductos() == null || oferta.getProductos().isEmpty())) {
            throw new IllegalArgumentException("Selecciona al menos un producto al que se aplica la oferta.");
        }

        if (oferta.getTipoOferta() == TipoOferta.CATEGORIA && oferta.getCategoria() == null) {
            throw new IllegalArgumentException("Selecciona la categoria a la que se aplica la oferta.");
        }
    }

    private void validarOfertaDuplicada(Oferta oferta) {
        boolean existeDuplicada = repositorioOferta.findAll().stream()
            .filter(ofertaExistente -> oferta.getId() == null || !oferta.getId().equals(ofertaExistente.getId()))
            .anyMatch(ofertaExistente -> esMismaOferta(oferta, ofertaExistente));

        if (existeDuplicada) {
            throw new IllegalArgumentException("Ya existe una oferta exactamente igual. No se ha creado ningun duplicado.");
        }
    }

    private boolean esMismaOferta(Oferta nueva, Oferta existente) {
        return nueva.getTipoOferta() == existente.getTipoOferta()
            && Objects.equals(nueva.getPorcentajeDescuento(), existente.getPorcentajeDescuento())
            && Objects.equals(nueva.getFechaInicio(), existente.getFechaInicio())
            && Objects.equals(nueva.getFechaFin(), existente.getFechaFin())
            && mismoDestinoOferta(nueva, existente);
    }

    private boolean mismoDestinoOferta(Oferta nueva, Oferta existente) {
        if (nueva.getTipoOferta() == TipoOferta.GLOBAL) {
            return true;
        }

        if (nueva.getTipoOferta() == TipoOferta.CATEGORIA) {
            Long nuevaCategoriaId = nueva.getCategoria() == null ? null : nueva.getCategoria().getId();
            Long existenteCategoriaId = existente.getCategoria() == null ? null : existente.getCategoria().getId();
            return Objects.equals(nuevaCategoriaId, existenteCategoriaId);
        }

        return obtenerIdsProductosOferta(nueva).equals(obtenerIdsProductosOferta(existente));
    }

    private Set<Long> obtenerIdsProductosOferta(Oferta oferta) {
        Set<Long> ids = oferta.getProductos() == null
            ? new java.util.LinkedHashSet<>()
            : oferta.getProductos().stream()
                .map(Producto::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));

        if (oferta.getProducto() != null && oferta.getProducto().getId() != null) {
            ids.add(oferta.getProducto().getId());
        }

        return ids;
    }

    private int normalizarPorcentaje(Integer porcentaje) {
        if (porcentaje == null) {
            return 10;
        }
        return Math.max(1, Math.min(99, porcentaje));
    }

    private void desmarcarOfertasPrincipales(Long ofertaActualId) {
        repositorioOferta.findAll().forEach(oferta -> {
            if (ofertaActualId == null || !ofertaActualId.equals(oferta.getId())) {
                oferta.setPrincipal(false);
                repositorioOferta.save(oferta);
            }
        });
    }

    private String normalizar(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : "";
    }
}
