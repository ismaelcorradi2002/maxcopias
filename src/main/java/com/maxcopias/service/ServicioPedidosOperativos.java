package com.maxcopias.service;

import com.maxcopias.model.EstadoPedidoCopisteria;
import com.maxcopias.model.EstadoPedidoTienda;
import com.maxcopias.model.CaraImpresion;
import com.maxcopias.model.ModoColor;
import com.maxcopias.model.MetodoEntregaPedidoTienda;
import com.maxcopias.model.PedidoCopisteria;
import com.maxcopias.model.PedidoTienda;
import com.maxcopias.model.TipoEncuadernacion;
import com.maxcopias.model.TipoPapel;
import com.maxcopias.model.TipoTrabajo;
import com.maxcopias.repository.RepositorioPedidoCopisteria;
import com.maxcopias.repository.RepositorioPedidoTienda;
import com.maxcopias.dto.DetallePedidoVista;
import com.maxcopias.dto.LineaPedidoTiendaVista;
import com.maxcopias.dto.LineaResumenEconomico;
import com.maxcopias.dto.ResumenFinancieroMensual;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioPedidosOperativos {

    private static final Locale LOCALE_ES = new Locale("es", "ES");

    private final RepositorioPedidoCopisteria repositorioPedidoCopisteria;
    private final RepositorioPedidoTienda repositorioPedidoTienda;
    private final ServicioAlmacenamientoArchivos servicioAlmacenamientoArchivos;
    private final ServicioPedidosTienda servicioPedidosTienda;

    public ServicioPedidosOperativos(
        RepositorioPedidoCopisteria repositorioPedidoCopisteria,
        RepositorioPedidoTienda repositorioPedidoTienda,
        ServicioAlmacenamientoArchivos servicioAlmacenamientoArchivos,
        ServicioPedidosTienda servicioPedidosTienda
    ) {
        this.repositorioPedidoCopisteria = repositorioPedidoCopisteria;
        this.repositorioPedidoTienda = repositorioPedidoTienda;
        this.servicioAlmacenamientoArchivos = servicioAlmacenamientoArchivos;
        this.servicioPedidosTienda = servicioPedidosTienda;
    }

    @Transactional(readOnly = true)
    public List<PedidoCopisteria> obtenerPedidosCopisteria() {
        return repositorioPedidoCopisteria.findAllByEliminadoFalseOrderByFechaCreacionDesc().stream()
            .sorted(comparadorPedidosCopisteria())
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PedidoCopisteria> obtenerPedidosCopisteriaConUsuario() {
        return repositorioPedidoCopisteria.findAllWithUsuarioOrderByFechaCreacionDesc();
    }

    @Transactional(readOnly = true)
    public List<PedidoTienda> obtenerPedidosTienda() {
        return repositorioPedidoTienda.findAllByEliminadoFalseOrderByFechaCreacionDesc().stream()
            .sorted(comparadorPedidosTienda())
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PedidoCopisteria> obtenerPedidosCopisteriaIncluyendoEliminados() {
        return repositorioPedidoCopisteria.findAllWithUsuarioIncludingDeletedOrderByFechaCreacionDesc().stream()
            .sorted(comparadorPedidosCopisteria())
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PedidoTienda> obtenerPedidosTiendaIncluyendoEliminados() {
        return repositorioPedidoTienda.findAllByOrderByFechaCreacionDesc().stream()
            .sorted(comparadorPedidosTienda())
            .toList();
    }

    @Transactional(readOnly = true)
    public PedidoCopisteria obtenerPedidoCopisteriaActivo(Long pedidoId) {
        return repositorioPedidoCopisteria.findByIdAndEliminadoFalse(pedidoId)
            .orElseThrow(() -> new IllegalArgumentException("No existe el pedido de copisteria indicado."));
    }

    @Transactional(readOnly = true)
    public PedidoTienda obtenerPedidoTiendaActivo(Long pedidoId) {
        return repositorioPedidoTienda.findByIdAndEliminadoFalse(pedidoId)
            .orElseThrow(() -> new IllegalArgumentException("No existe el pedido de tienda indicado."));
    }

    @Transactional(readOnly = true)
    public PedidoCopisteria obtenerPedidoCopisteriaIncluyendoEliminados(Long pedidoId) {
        return repositorioPedidoCopisteria.findById(pedidoId)
            .orElseThrow(() -> new IllegalArgumentException("No existe el pedido de copisteria indicado."));
    }

    @Transactional(readOnly = true)
    public PedidoTienda obtenerPedidoTiendaIncluyendoEliminados(Long pedidoId) {
        return repositorioPedidoTienda.findById(pedidoId)
            .orElseThrow(() -> new IllegalArgumentException("No existe el pedido de tienda indicado."));
    }

    @Transactional
    public void cambiarEstadoCopisteria(Long pedidoId, EstadoPedidoCopisteria estado) {
        PedidoCopisteria pedido = repositorioPedidoCopisteria.findByIdAndEliminadoFalse(pedidoId)
            .orElseThrow(() -> new IllegalArgumentException("No existe el pedido de copisteria indicado."));
        pedido.setEstado(estado);
        repositorioPedidoCopisteria.save(pedido);
    }

    @Transactional
    public void cambiarEstadoTienda(Long pedidoId, EstadoPedidoTienda estado) {
        servicioPedidosTienda.cambiarEstado(pedidoId, estado);
    }

    @Transactional
    public void marcarPagadoTienda(Long pedidoId, boolean pagado) {
        servicioPedidosTienda.marcarPagado(pedidoId, pagado);
    }

    @Transactional
    public void cambiarEstadoCopisteriaBulk(List<Long> ids, EstadoPedidoCopisteria estado) {
        for (Long id : ids) {
            cambiarEstadoCopisteria(id, estado);
        }
    }

    @Transactional
    public void cambiarEstadoTiendaBulk(List<Long> ids, EstadoPedidoTienda estado) {
        for (Long id : ids) {
            cambiarEstadoTienda(id, estado);
        }
    }

    @Transactional
    public void eliminarPedidoCopisteria(Long pedidoId, String eliminadoPor) {
        PedidoCopisteria pedido = repositorioPedidoCopisteria.findByIdAndEliminadoFalse(pedidoId)
            .orElseThrow(() -> new IllegalArgumentException("No existe el pedido de copisteria indicado."));
        pedido.setEliminado(true);
        pedido.setFechaEliminacion(LocalDateTime.now());
        pedido.setEliminadoPor(eliminadoPor);
        repositorioPedidoCopisteria.save(pedido);
    }

    @Transactional
    public void eliminarPedidoTienda(Long pedidoId, String eliminadoPor) {
        PedidoTienda pedido = repositorioPedidoTienda.findByIdAndEliminadoFalse(pedidoId)
            .orElseThrow(() -> new IllegalArgumentException("No existe el pedido de tienda indicado."));
        pedido.setEliminado(true);
        pedido.setFechaEliminacion(LocalDateTime.now());
        pedido.setEliminadoPor(eliminadoPor);
        repositorioPedidoTienda.save(pedido);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> obtenerContadoresTrabajo() {
        Map<String, Long> copisteria = obtenerContadoresCopisteriaResumen();
        Map<String, Long> tienda = obtenerContadoresTiendaResumen();

        return Map.of(
            "pendientes", copisteria.get("pendientes") + tienda.get("pendientes"),
            "preparando", copisteria.get("preparando") + tienda.get("preparando"),
            "listos", copisteria.get("listos") + tienda.get("listos"),
            "entregados", copisteria.get("entregados") + tienda.get("entregados")
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Long> obtenerContadoresCopisteriaResumen() {
        List<PedidoCopisteria> pedidosCopisteria = obtenerPedidosCopisteria();

        return Map.of(
            "pendientes", pedidosCopisteria.stream().filter(p -> p.getEstado() == EstadoPedidoCopisteria.PENDIENTE).count(),
            "preparando", pedidosCopisteria.stream().filter(p -> p.getEstado() == EstadoPedidoCopisteria.EN_PREPARACION).count(),
            "listos", pedidosCopisteria.stream().filter(p -> p.getEstado() == EstadoPedidoCopisteria.LISTO_PARA_RECOGER).count(),
            "entregados", pedidosCopisteria.stream().filter(p -> p.getEstado() == EstadoPedidoCopisteria.ENTREGADO).count()
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Long> obtenerContadoresTiendaResumen() {
        List<PedidoTienda> pedidosTienda = obtenerPedidosTienda();

        return Map.of(
            "pendientes", pedidosTienda.stream().filter(p -> p.getEstado() == EstadoPedidoTienda.PENDIENTE).count(),
            "preparando", pedidosTienda.stream().filter(p -> p.getEstado() == EstadoPedidoTienda.EN_PREPARACION).count(),
            "listos", pedidosTienda.stream().filter(p -> p.getEstado() == EstadoPedidoTienda.LISTO_PARA_RECOGER).count(),
            "entregados", pedidosTienda.stream().filter(p -> p.getEstado() == EstadoPedidoTienda.ENTREGADO).count()
        );
    }

    @Transactional(readOnly = true)
    public Map<EstadoPedidoCopisteria, Long> contarCopisteriaPorEstado() {
        Map<EstadoPedidoCopisteria, Long> contador = new EnumMap<>(EstadoPedidoCopisteria.class);
        for (EstadoPedidoCopisteria estado : EstadoPedidoCopisteria.values()) {
            contador.put(estado, 0L);
        }
        obtenerPedidosCopisteria().forEach(pedido ->
            contador.computeIfPresent(pedido.getEstado(), (estado, total) -> total + 1)
        );
        return contador;
    }

    @Transactional(readOnly = true)
    public Map<EstadoPedidoTienda, Long> contarTiendaPorEstado() {
        Map<EstadoPedidoTienda, Long> contador = new EnumMap<>(EstadoPedidoTienda.class);
        for (EstadoPedidoTienda estado : EstadoPedidoTienda.values()) {
            contador.put(estado, 0L);
        }
        obtenerPedidosTienda().forEach(pedido ->
            contador.computeIfPresent(pedido.getEstado(), (estado, total) -> total + 1)
        );
        return contador;
    }

    @Transactional(readOnly = true)
    public ResumenFinancieroMensual obtenerResumenFinancieroMensual(int anio, int mes) {
        YearMonth yearMonth = YearMonth.of(anio, mes);
        YearMonth previousMonth = yearMonth.minusMonths(1);

        List<PedidoCopisteria> pedidosCopisteriaMes = filtrarPedidosDelMes(obtenerPedidosCopisteria(), yearMonth);
        List<PedidoTienda> pedidosTiendaMes = filtrarPedidosTiendaDelMes(obtenerPedidosTienda(), yearMonth);
        List<PedidoCopisteria> pedidosCopisteriaMesAnterior = filtrarPedidosDelMes(obtenerPedidosCopisteria(), previousMonth);
        List<PedidoTienda> pedidosTiendaMesAnterior = filtrarPedidosTiendaDelMes(obtenerPedidosTienda(), previousMonth);

        BigDecimal ingresosCopisteria = sumarEntregadosCopisteria(pedidosCopisteriaMes);
        BigDecimal ingresosTienda = sumarEntregadosTienda(pedidosTiendaMes);
        BigDecimal ingresosTotales = ingresosCopisteria.add(ingresosTienda);
        BigDecimal ingresosMesAnterior = sumarEntregadosCopisteria(pedidosCopisteriaMesAnterior).add(sumarEntregadosTienda(pedidosTiendaMesAnterior));

        long pedidosEntregados = pedidosCopisteriaMes.stream().filter(this::esEntregadoCopisteria).count()
            + pedidosTiendaMes.stream().filter(this::esEntregadoTienda).count();
        long pedidosCancelados = pedidosCopisteriaMes.stream().filter(p -> p.getEstado() == EstadoPedidoCopisteria.CANCELADO).count()
            + pedidosTiendaMes.stream().filter(p -> p.getEstado() == EstadoPedidoTienda.CANCELADO).count();
        long pedidosPendientesOPreparacion = pedidosCopisteriaMes.stream()
            .filter(p -> p.getEstado() == EstadoPedidoCopisteria.PENDIENTE || p.getEstado() == EstadoPedidoCopisteria.EN_PREPARACION)
            .count()
            + pedidosTiendaMes.stream()
                .filter(p -> p.getEstado() == EstadoPedidoTienda.PENDIENTE || p.getEstado() == EstadoPedidoTienda.EN_PREPARACION)
                .count();

        BigDecimal ticketMedio = pedidosEntregados == 0
            ? BigDecimal.ZERO
            : ingresosTotales.divide(BigDecimal.valueOf(pedidosEntregados), 2, RoundingMode.HALF_UP);

        String servicioMasSolicitado = pedidosCopisteriaMes.stream()
            .filter(pedido -> pedido.getTrabajo() != null)
            .collect(Collectors.groupingBy(PedidoCopisteria::getTrabajo, Collectors.counting()))
            .entrySet()
            .stream()
            .max(Map.Entry.comparingByValue())
            .map(entry -> entry.getKey().getLabel())
            .orElse("No disponible");

        String productoMasVendido = calcularProductoMasVendido(pedidosTiendaMes);

        return new ResumenFinancieroMensual(
            mes,
            anio,
            ingresosTotales,
            ingresosCopisteria,
            ingresosTienda,
            pedidosEntregados,
            pedidosCancelados,
            pedidosPendientesOPreparacion,
            ticketMedio,
            productoMasVendido,
            servicioMasSolicitado,
            ingresosMesAnterior,
            ingresosTotales.subtract(ingresosMesAnterior)
        );
    }

    private List<PedidoCopisteria> filtrarPedidosDelMes(List<PedidoCopisteria> pedidos, YearMonth yearMonth) {
        return pedidos.stream()
            .filter(pedido -> pedido.getFechaCreacion() != null && YearMonth.from(pedido.getFechaCreacion()).equals(yearMonth))
            .toList();
    }

    private List<PedidoTienda> filtrarPedidosTiendaDelMes(List<PedidoTienda> pedidos, YearMonth yearMonth) {
        return pedidos.stream()
            .filter(pedido -> pedido.getFechaCreacion() != null && YearMonth.from(pedido.getFechaCreacion()).equals(yearMonth))
            .toList();
    }

    private BigDecimal sumarEntregadosCopisteria(List<PedidoCopisteria> pedidos) {
        return pedidos.stream()
            .filter(this::esEntregadoCopisteria)
            .map(PedidoCopisteria::getPrecio)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumarEntregadosTienda(List<PedidoTienda> pedidos) {
        return pedidos.stream()
            .filter(this::esEntregadoTienda)
            .map(PedidoTienda::getTotal)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean esEntregadoCopisteria(PedidoCopisteria pedido) {
        return pedido.getEstado() == EstadoPedidoCopisteria.ENTREGADO;
    }

    private boolean esEntregadoTienda(PedidoTienda pedido) {
        return pedido.getEstado() == EstadoPedidoTienda.ENTREGADO;
    }

    private String calcularProductoMasVendido(List<PedidoTienda> pedidosTiendaMes) {
        return pedidosTiendaMes.stream()
            .filter(this::esEntregadoTienda)
            .flatMap(pedido -> {
                if (pedido.getItems() != null && !pedido.getItems().isEmpty()) {
                    return pedido.getItems().stream().map(item -> item.getProductoNombre());
                }
                if (pedido.getResumenProductos() == null) {
                    return List.<String>of().stream();
                }
                return List.of(pedido.getResumenProductos().split(",")).stream().map(String::trim);
            })
            .filter(texto -> !texto.isBlank())
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
            .entrySet()
            .stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("No disponible");
    }

    @Transactional(readOnly = true)
    public DetallePedidoVista construirDetallePedidoCopisteria(PedidoCopisteria pedido) {
        List<String> extras = parsearExtrasCopisteria(pedido.getExtras());
        ArchivoDetalle archivoDetalle = resolverDetalleArchivo(pedido);
        List<LineaResumenEconomico> resumenEconomico = construirResumenEconomicoCopisteria(pedido, archivoDetalle.pageCount());
        BigDecimal precioExtras = calcularPrecioExtras(resumenEconomico);
        BigDecimal precioBase = calcularPrecioBase(resumenEconomico, precioExtras, pedido.getPrecio());

        return new DetallePedidoVista(
            pedido.getId(),
            "Copisteria",
            pedido.getCustomerName(),
            pedido.getEmail(),
            pedido.getPhone(),
            pedido.getTrabajo() != null ? pedido.getTrabajo().getLabel() : null,
            pedido.getTamano() != null ? pedido.getTamano().getLabel() : null,
            pedido.getColor() != null ? pedido.getColor().getLabel() : null,
            pedido.getCaras() != null ? pedido.getCaras().getLabel() : null,
            pedido.getCopias(),
            pedido.getPapel() != null ? pedido.getPapel().getLabel() : null,
            pedido.getEncuadernacion() != null ? pedido.getEncuadernacion().getLabel() : null,
            extras,
            extraerObservaciones(pedido.getExtras()),
            pedido.getNombreArchivo(),
            pedido.getRutaArchivo() != null ? "/pedidos/copisteria/" + pedido.getId() + "/archivo" : null,
            pedido.getRutaArchivo() != null ? "/pedidos/copisteria/" + pedido.getId() + "/archivo?download=true" : null,
            pedido.getRutaArchivo() != null ? "/pedidos/copisteria/" + pedido.getId() + "/archivo" : null,
            archivoDetalle.pageCount() > 0 ? archivoDetalle.pageCount() : null,
            archivoDetalle.readableSize(),
            precioBase,
            precioExtras,
            pedido.getPrecio(),
            true,
            resumenEconomico,
            pedido.getEstado().name(),
            pedido.getEstado().getLabel(),
            pedido.getCodigoRecoger(),
            pedido.getFechaCreacion(),
            pedido.getFechaEliminacion() != null ? pedido.getFechaEliminacion() : pedido.getFechaCreacion(),
            null,
            null,
            resolverMetodoEntregaCopisteria(pedido),
            resolverDireccionEntregaCopisteria(pedido),
            false,
            null,
            null,
            null,
            null,
            List.of(),
            pedido.isEliminado(),
            pedido.getEliminadoPor()
        );
    }

    @Transactional(readOnly = true)
    public DetallePedidoVista construirDetallePedidoTienda(PedidoTienda pedido) {
        List<LineaPedidoTiendaVista> lineas = pedido.getItems().stream()
            .map(item -> new LineaPedidoTiendaVista(
                item.getProductoNombre(),
                null,
                item.getProductoImagenUrl(),
                item.getCantidad(),
                item.getPrecioUnitario(),
                item.getSubtotal(),
                formatearDinero(item.getPrecioUnitario()),
                formatearDinero(item.getSubtotal())
            ))
            .toList();

        return new DetallePedidoVista(
            pedido.getId(),
            "Tienda",
            pedido.getClienteNombre(),
            pedido.getEmail(),
            pedido.getTelefono(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            List.of(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            pedido.getTotal(),
            null,
            pedido.getTotal(),
            true,
            construirResumenEconomicoTienda(pedido),
            pedido.getEstado().name(),
            pedido.getEstado().getLabel(),
            null,
            pedido.getFechaCreacion(),
            pedido.getFechaEliminacion() != null ? pedido.getFechaEliminacion() : pedido.getFechaCreacion(),
            pedido.getResumenProductos(),
            pedido.getCodigoPedido(),
            pedido.getMetodoEntrega() != null ? pedido.getMetodoEntrega().getLabel() : null,
            formatearDireccionEntrega(pedido),
            pedido.isPagado(),
            pedido.getMetodoPago() != null ? pedido.getMetodoPago().getLabel() : null,
            pedido.getFechaPago(),
            pedido.getSubtotal(),
            pedido.getGastosEnvio(),
            lineas,
            pedido.isEliminado(),
            pedido.getEliminadoPor()
        );
    }

    private List<String> parsearExtrasCopisteria(String extras) {
        if (extras == null || extras.isBlank()) {
            return List.of();
        }

        List<String> seleccionados = new ArrayList<>();
        if (extras.contains("plastificado=true")) {
            seleccionados.add("Plastificado");
        }
        if (extras.contains("urgente=true")) {
            seleccionados.add("Urgente");
        }
        if (extras.contains("escaneado=true")) {
            seleccionados.add("Escaneado");
        }
        return seleccionados;
    }

    private String extraerObservaciones(String extras) {
        if (extras == null || !extras.contains("observaciones='")) {
            return null;
        }

        int inicio = extras.indexOf("observaciones='");
        if (inicio < 0) {
            return null;
        }
        String resto = extras.substring(inicio + "observaciones='".length());
        int fin = resto.indexOf('\'');
        if (fin < 0) {
            return null;
        }
        String observaciones = resto.substring(0, fin).trim();
        return observaciones.isBlank() ? null : observaciones;
    }

    private List<LineaResumenEconomico> construirResumenEconomicoCopisteria(PedidoCopisteria pedido, int pageCount) {
        List<String> extras = parsearExtrasCopisteria(pedido.getExtras());
        if (pedido.getTrabajo() == null) {
            return construirResumenFallback(pedido.getPrecio(), extras);
        }

        return switch (pedido.getTrabajo()) {
            case IMPRESION -> construirResumenImpresion(pedido, pageCount, new BigDecimal("0.06"), new BigDecimal("0.45"), extras);
            case FOTOCOPIAS -> construirResumenImpresion(pedido, pageCount, new BigDecimal("0.05"), new BigDecimal("0.18"), extras);
            case PUBLICIDAD_IMPRENTA -> construirResumenPublicidad(pedido, pageCount, extras);
            case OTRO, ENCUADERNACION, PLASTIFICADO, PERSONALIZACION, SERVICIOS_ADICIONALES ->
                construirResumenTipoPresupuesto(pedido, pageCount, extras, new BigDecimal("12.00"), "Base del servicio");
        };
    }

    private List<LineaResumenEconomico> construirResumenImpresion(
        PedidoCopisteria pedido,
        int pageCount,
        BigDecimal precioBn,
        BigDecimal precioColor,
        List<String> extras
    ) {
        int copies = pedido.getCopias() != null && pedido.getCopias() > 0 ? pedido.getCopias() : 1;
        int pages = Math.max(pageCount, 1);
        boolean color = pedido.getColor() == ModoColor.COLOR;
        BigDecimal precioUnitario = color ? precioColor : precioBn;
        BigDecimal subtotalImpresion = money(precioUnitario.multiply(BigDecimal.valueOf(pages)).multiply(BigDecimal.valueOf(copies)));

        BigDecimal sizeMultiplier = sizeMultiplier(pedido);
        BigDecimal sideMultiplier = sideMultiplier(pedido);
        BigDecimal paperMultiplier = paperMultiplier(pedido);

        BigDecimal sizeExtra = money(subtotalImpresion.multiply(sizeMultiplier.subtract(BigDecimal.ONE)));
        BigDecimal afterSize = subtotalImpresion.add(sizeExtra);
        BigDecimal sideExtra = money(afterSize.multiply(sideMultiplier.subtract(BigDecimal.ONE)));
        BigDecimal afterSides = afterSize.add(sideExtra);
        BigDecimal paperExtra = money(afterSides.multiply(paperMultiplier.subtract(BigDecimal.ONE)));
        BigDecimal subtotalConfiguracion = subtotalImpresion.add(sizeExtra).add(sideExtra).add(paperExtra);

        BigDecimal bindingPrice = bindingPrice(pedido.getEncuadernacion());
        List<LineaResumenEconomico> lines = new ArrayList<>();
        lines.add(new LineaResumenEconomico(
            pedido.getTrabajo().getLabel() + " " + colorLabel(pedido) + " " + sizeLabel(pedido),
            "Precio por pagina segun configuracion base",
            pages + " pag. x " + copies + " cop.",
            precioUnitario,
            subtotalImpresion,
            false,
            false
        ));
        lines.add(new LineaResumenEconomico("Numero de paginas", null, "x" + pages, null, null, false, false));
        lines.add(new LineaResumenEconomico("Copias", null, "x" + copies, null, null, false, false));
        lines.add(new LineaResumenEconomico(
            "Tamano " + sizeLabel(pedido),
            "Ajuste por formato del papel",
            "x1",
            sizeExtra,
            sizeExtra,
            false,
            false
        ));
        lines.add(new LineaResumenEconomico(
            sideLabel(pedido),
            "Configuracion de impresion",
            "x1",
            sideExtra,
            sideExtra,
            false,
            false
        ));
        lines.add(new LineaResumenEconomico(
            "Papel " + paperLabel(pedido),
            "Acabado del papel",
            "x1",
            paperExtra,
            paperExtra,
            false,
            false
        ));
        lines.add(new LineaResumenEconomico("Subtotal impresion", null, null, null, subtotalConfiguracion, true, false));
        lines.add(new LineaResumenEconomico(
            "Encuadernacion " + bindingLabel(pedido.getEncuadernacion()),
            bindingPrice.compareTo(BigDecimal.ZERO) > 0 ? "Acabado adicional" : "Sin coste",
            "x1",
            bindingPrice,
            bindingPrice,
            false,
            false
        ));
        lines.addAll(construirLineasExtras(pedido, extras, pages));
        return finalizarResumen(lines, pedido.getPrecio());
    }

    private List<LineaResumenEconomico> construirResumenPublicidad(PedidoCopisteria pedido, int pageCount, List<String> extras) {
        int copies = pedido.getCopias() != null && pedido.getCopias() > 0 ? pedido.getCopias() : 1;
        int pages = Math.max(pageCount, 1);
        BigDecimal base = new BigDecimal("19.00");
        BigDecimal productionUnit = pedido.getColor() == ModoColor.COLOR ? new BigDecimal("0.22") : new BigDecimal("0.12");
        BigDecimal production = money(
            productionUnit
                .multiply(campaignSizeMultiplier(pedido))
                .multiply(paperMultiplier(pedido))
                .multiply(BigDecimal.valueOf(pages))
                .multiply(BigDecimal.valueOf(copies))
        );

        List<LineaResumenEconomico> lines = new ArrayList<>();
        lines.add(new LineaResumenEconomico("Base de imprenta", "Preparacion del encargo", "x1", base, base, false, false));
        lines.add(new LineaResumenEconomico(
            "Produccion " + colorLabel(pedido) + " " + sizeLabel(pedido),
            "Material principal del pedido",
            pages + " pag. x " + copies + " uds.",
            productionUnit,
            production,
            false,
            false
        ));
        lines.add(new LineaResumenEconomico("Numero de paginas", null, "x" + pages, null, null, false, false));
        lines.add(new LineaResumenEconomico("Copias", null, "x" + copies, null, null, false, false));
        lines.add(new LineaResumenEconomico(
            "Papel " + paperLabel(pedido),
            "Ajuste incluido en produccion",
            "x1",
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            false,
            false
        ));
        lines.addAll(construirLineasExtras(pedido, extras, pages));
        return finalizarResumen(lines, pedido.getPrecio());
    }

    private List<LineaResumenEconomico> construirResumenTipoPresupuesto(
        PedidoCopisteria pedido,
        int pageCount,
        List<String> extras,
        BigDecimal basePrice,
        String baseConcept
    ) {
        int fileCount = pedido.getFileCount();
        BigDecimal additionalFiles = money(BigDecimal.valueOf(Math.max(fileCount, 1) - 1).multiply(new BigDecimal("2.50")));
        List<LineaResumenEconomico> lines = new ArrayList<>();
        lines.add(new LineaResumenEconomico(baseConcept, pedido.getTrabajo().getLabel(), "x1", basePrice, basePrice, false, false));
        if (pageCount > 0) {
            lines.add(new LineaResumenEconomico("Numero de paginas", null, "x" + pageCount, null, null, false, false));
        }
        lines.add(new LineaResumenEconomico("Archivos adicionales", "Ficheros extra del encargo", "x" + Math.max(fileCount - 1, 0), additionalFiles, additionalFiles, false, false));
        lines.addAll(construirLineasExtras(pedido, extras, Math.max(pageCount, 1)));
        return finalizarResumen(lines, pedido.getPrecio());
    }

    private List<LineaResumenEconomico> construirLineasExtras(PedidoCopisteria pedido, List<String> extras, int pages) {
        List<LineaResumenEconomico> lines = new ArrayList<>();
        int fileCount = pedido.getFileCount();
        if (extras.contains("Plastificado")) {
            BigDecimal plastificado = money(new BigDecimal("1.80").multiply(BigDecimal.valueOf(Math.max(fileCount, 1))));
            lines.add(new LineaResumenEconomico(
                "Plastificado",
                "Proteccion del documento",
                "x" + Math.max(fileCount, 1),
                plastificado,
                plastificado,
                false,
                false
            ));
        }
        if (extras.contains("Urgente")) {
            BigDecimal urgente = new BigDecimal("2.00");
            lines.add(new LineaResumenEconomico(
                "Servicio urgente",
                "Prioridad de preparacion",
                "x1",
                urgente,
                urgente,
                false,
                false
            ));
        }
        if (extras.contains("Escaneado")) {
            BigDecimal escaneado = money(new BigDecimal("0.50").multiply(BigDecimal.valueOf(pages)));
            lines.add(new LineaResumenEconomico(
                "Escaneado",
                "Digitalizacion del pedido",
                "x" + pages,
                escaneado,
                escaneado,
                false,
                false
            ));
        }
        return List.copyOf(lines);
    }

    private List<LineaResumenEconomico> construirResumenFallback(BigDecimal total, List<String> extras) {
        List<LineaResumenEconomico> lines = new ArrayList<>();
        lines.add(new LineaResumenEconomico("Configuracion del pedido", "Importe base reconstruido", "x1", total, total, false, false));
        if (!extras.isEmpty()) {
            lines.add(new LineaResumenEconomico("Extras seleccionados", String.join(", ", extras), null, BigDecimal.ZERO, BigDecimal.ZERO, false, false));
        }
        return finalizarResumen(lines, total);
    }

    private List<LineaResumenEconomico> construirResumenEconomicoTienda(PedidoTienda pedido) {
        List<LineaResumenEconomico> lines = new ArrayList<>();
        if (pedido.getItems() != null && !pedido.getItems().isEmpty()) {
            pedido.getItems().forEach(item -> lines.add(new LineaResumenEconomico(
                item.getProductoNombre(),
                "Producto de tienda",
                "x" + item.getCantidad(),
                item.getPrecioUnitario(),
                item.getSubtotal(),
                false,
                false
            )));
        } else {
            lines.add(new LineaResumenEconomico(
                "Productos del pedido",
                pedido.getResumenProductos(),
                "x1",
                pedido.getTotal(),
                pedido.getSubtotal() != null ? pedido.getSubtotal() : pedido.getTotal(),
                false,
                false
            ));
        }

        if (money(pedido.getGastosEnvio()).compareTo(BigDecimal.ZERO) > 0) {
            lines.add(new LineaResumenEconomico(
                "Gastos de envio",
                pedido.getMetodoEntrega() != null ? pedido.getMetodoEntrega().getLabel() : "Envio",
                "x1",
                pedido.getGastosEnvio(),
                pedido.getGastosEnvio(),
                false,
                false
            ));
        }

        return finalizarResumen(lines, pedido.getTotal());
    }

    private List<LineaResumenEconomico> finalizarResumen(List<LineaResumenEconomico> baseLines, BigDecimal totalFinal) {
        List<LineaResumenEconomico> lines = new ArrayList<>(baseLines);
        BigDecimal subtotal = lines.stream()
            .map(LineaResumenEconomico::subtotal)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal safeTotal = totalFinal == null ? subtotal : money(totalFinal);
        BigDecimal ajuste = money(safeTotal.subtract(subtotal));

        if (ajuste.compareTo(BigDecimal.ZERO) != 0) {
            lines.add(new LineaResumenEconomico(
                "Ajuste del total",
                "Correccion para mantener el importe final guardado",
                "x1",
                ajuste,
                ajuste,
                false,
                false
            ));
            subtotal = subtotal.add(ajuste);
        }

        lines.add(new LineaResumenEconomico("Subtotal", null, null, null, subtotal, true, false));
        lines.add(new LineaResumenEconomico("Total final", null, null, null, safeTotal, false, true));
        return List.copyOf(lines);
    }

    private BigDecimal calcularPrecioExtras(List<LineaResumenEconomico> resumenEconomico) {
        return resumenEconomico.stream()
            .filter(line -> line.hasSubtotal())
            .filter(line -> !line.subtotalRow() && !line.totalRow())
            .filter(line -> line.concepto().startsWith("Encuadernacion")
                || line.concepto().equals("Plastificado")
                || line.concepto().equals("Servicio urgente")
                || line.concepto().equals("Escaneado")
                || line.concepto().equals("Ajuste del total"))
            .map(LineaResumenEconomico::subtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularPrecioBase(List<LineaResumenEconomico> resumenEconomico, BigDecimal extras, BigDecimal total) {
        BigDecimal subtotal = resumenEconomico.stream()
            .filter(LineaResumenEconomico::totalRow)
            .findFirst()
            .map(LineaResumenEconomico::subtotal)
            .orElse(total);
        if (subtotal == null) {
            return total;
        }
        return money(subtotal.subtract(extras == null ? BigDecimal.ZERO : extras));
    }

    private ArchivoDetalle resolverDetalleArchivo(PedidoCopisteria pedido) {
        if (pedido.getRutaArchivo() == null || pedido.getRutaArchivo().isBlank()) {
            return new ArchivoDetalle(0, null);
        }

        try {
            int totalPages = 0;
            long totalSize = 0L;

            for (String ruta : pedido.getRutasArchivo()) {
                Path path = servicioAlmacenamientoArchivos.resolveStoredPath(ruta);
                if (!Files.exists(path)) {
                    continue;
                }
                totalSize += Files.size(path);
                totalPages += resolverPaginasArchivo(path);
            }

            return new ArchivoDetalle(totalPages, formatFileSize(totalSize));
        } catch (Exception exception) {
            return new ArchivoDetalle(0, null);
        }
    }

    private int resolverPaginasArchivo(Path path) throws IOException {
        String filename = path.getFileName().toString().toLowerCase();
        if (filename.endsWith(".pdf")) {
            try (PDDocument document = PDDocument.load(path.toFile())) {
                return Math.max(document.getNumberOfPages(), 1);
            }
        }
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".png")) {
            return 1;
        }
        return 1;
    }

    private String formatFileSize(long sizeInBytes) {
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

    private boolean tieneExtra(String extras, String marker) {
        return extras != null && extras.contains(marker);
    }

    private BigDecimal sizeMultiplier(PedidoCopisteria pedido) {
        if (pedido.getTamano() == null) {
            return BigDecimal.ONE;
        }
        return switch (pedido.getTamano()) {
            case A5 -> new BigDecimal("0.72");
            case A3 -> new BigDecimal("1.85");
            default -> BigDecimal.ONE;
        };
    }

    private BigDecimal campaignSizeMultiplier(PedidoCopisteria pedido) {
        if (pedido.getTamano() == null) {
            return BigDecimal.ONE;
        }
        return switch (pedido.getTamano()) {
            case A5 -> new BigDecimal("0.78");
            case A3 -> new BigDecimal("1.40");
            default -> BigDecimal.ONE;
        };
    }

    private BigDecimal sideMultiplier(PedidoCopisteria pedido) {
        return pedido.getCaras() == CaraImpresion.DOUBLE_SIDED ? new BigDecimal("1.80") : BigDecimal.ONE;
    }

    private BigDecimal paperMultiplier(PedidoCopisteria pedido) {
        if (pedido.getPapel() == null) {
            return BigDecimal.ONE;
        }
        return switch (pedido.getPapel()) {
            case SATINADO -> new BigDecimal("1.35");
            case CARTULINA -> new BigDecimal("1.65");
            default -> BigDecimal.ONE;
        };
    }

    private BigDecimal bindingPrice(TipoEncuadernacion bindingType) {
        if (bindingType == null) {
            return BigDecimal.ZERO;
        }
        return switch (bindingType) {
            case ESPIRAL -> new BigDecimal("3.50");
            case TAPA_DURA -> new BigDecimal("7.50");
            case GRAPADO -> new BigDecimal("0.60");
            default -> BigDecimal.ZERO;
        };
    }

    private String colorLabel(PedidoCopisteria pedido) {
        return pedido.getColor() == ModoColor.COLOR ? "color" : "blanco y negro";
    }

    private String sizeLabel(PedidoCopisteria pedido) {
        return pedido.getTamano() != null ? pedido.getTamano().getLabel() : "A4";
    }

    private String sideLabel(PedidoCopisteria pedido) {
        return pedido.getCaras() == CaraImpresion.DOUBLE_SIDED ? "Doble cara" : "Una cara";
    }

    private String paperLabel(PedidoCopisteria pedido) {
        return pedido.getPapel() != null ? pedido.getPapel().getLabel() : TipoPapel.NORMAL.getLabel();
    }

    private String bindingLabel(TipoEncuadernacion bindingType) {
        return bindingType != null ? bindingType.getLabel() : TipoEncuadernacion.SIN_ENCUADERNACION.getLabel();
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String formatearDinero(BigDecimal valor) {
        return NumberFormat.getCurrencyInstance(LOCALE_ES).format(money(valor));
    }

    private String formatearDireccionEntrega(PedidoTienda pedido) {
        if (pedido.getMetodoEntrega() != MetodoEntregaPedidoTienda.ENVIO_DOMICILIO) {
            return null;
        }

        List<String> parts = new ArrayList<>();
        if (pedido.getDireccionEntrega() != null && !pedido.getDireccionEntrega().isBlank()) {
            parts.add(pedido.getDireccionEntrega().trim());
        }
        if (pedido.getCodigoPostalEntrega() != null && !pedido.getCodigoPostalEntrega().isBlank()) {
            parts.add(pedido.getCodigoPostalEntrega().trim());
        }
        if (pedido.getCiudadEntrega() != null && !pedido.getCiudadEntrega().isBlank()) {
            parts.add(pedido.getCiudadEntrega().trim());
        }

        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    private String resolverMetodoEntregaCopisteria(PedidoCopisteria pedido) {
        String value = extraValue(pedido.getExtras(), "deliveryMethod");
        return "HOME_DELIVERY".equalsIgnoreCase(value) ? "Envio a domicilio" : "Recogida en tienda";
    }

    private String resolverDireccionEntregaCopisteria(PedidoCopisteria pedido) {
        String value = extraValue(pedido.getExtras(), "deliveryAddress");
        return value == null || value.isBlank() ? null : value;
    }

    private String extraValue(String extras, String key) {
        if (extras == null || extras.isBlank()) {
            return null;
        }

        String quotedPrefix = key + "='";
        int quotedStart = extras.indexOf(quotedPrefix);
        if (quotedStart >= 0) {
            int valueStart = quotedStart + quotedPrefix.length();
            int valueEnd = extras.indexOf("'", valueStart);
            return valueEnd >= valueStart ? extras.substring(valueStart, valueEnd) : null;
        }

        String plainPrefix = key + "=";
        int plainStart = extras.indexOf(plainPrefix);
        if (plainStart >= 0) {
            int valueStart = plainStart + plainPrefix.length();
            int valueEnd = extras.indexOf(",", valueStart);
            return valueEnd >= valueStart ? extras.substring(valueStart, valueEnd) : extras.substring(valueStart);
        }

        return null;
    }

    private record ArchivoDetalle(int pageCount, String readableSize) {
    }

    private Comparator<PedidoCopisteria> comparadorPedidosCopisteria() {
        return Comparator
            .comparing((PedidoCopisteria pedido) -> pedido.getEstado() == EstadoPedidoCopisteria.ENTREGADO)
            .thenComparing(PedidoCopisteria::getFechaCreacion, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private Comparator<PedidoTienda> comparadorPedidosTienda() {
        return Comparator
            .comparing((PedidoTienda pedido) -> pedido.getEstado() == EstadoPedidoTienda.ENTREGADO)
            .thenComparing(PedidoTienda::getFechaCreacion, Comparator.nullsLast(Comparator.reverseOrder()));
    }
}
