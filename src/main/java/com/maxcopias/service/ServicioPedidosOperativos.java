package com.maxcopias.service;

import com.maxcopias.model.EstadoPedidoCopisteria;
import com.maxcopias.model.EstadoPedidoTienda;
import com.maxcopias.model.PedidoCopisteria;
import com.maxcopias.model.PedidoTienda;
import com.maxcopias.repository.RepositorioPedidoCopisteria;
import com.maxcopias.repository.RepositorioPedidoTienda;
import com.maxcopias.dto.ResumenFinancieroMensual;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioPedidosOperativos {

    private final RepositorioPedidoCopisteria repositorioPedidoCopisteria;
    private final RepositorioPedidoTienda repositorioPedidoTienda;

    public ServicioPedidosOperativos(
        RepositorioPedidoCopisteria repositorioPedidoCopisteria,
        RepositorioPedidoTienda repositorioPedidoTienda
    ) {
        this.repositorioPedidoCopisteria = repositorioPedidoCopisteria;
        this.repositorioPedidoTienda = repositorioPedidoTienda;
    }

    @Transactional(readOnly = true)
    public List<PedidoCopisteria> obtenerPedidosCopisteria() {
        return repositorioPedidoCopisteria.findAllByEliminadoFalseOrderByFechaCreacionDesc();
    }

    @Transactional(readOnly = true)
    public List<PedidoCopisteria> obtenerPedidosCopisteriaConUsuario() {
        return repositorioPedidoCopisteria.findAllWithUsuarioOrderByFechaCreacionDesc();
    }

    @Transactional(readOnly = true)
    public List<PedidoTienda> obtenerPedidosTienda() {
        return repositorioPedidoTienda.findAllByEliminadoFalseOrderByFechaCreacionDesc();
    }

    @Transactional(readOnly = true)
    public List<PedidoCopisteria> obtenerPedidosCopisteriaIncluyendoEliminados() {
        return repositorioPedidoCopisteria.findAllWithUsuarioIncludingDeletedOrderByFechaCreacionDesc();
    }

    @Transactional(readOnly = true)
    public List<PedidoTienda> obtenerPedidosTiendaIncluyendoEliminados() {
        return repositorioPedidoTienda.findAllByOrderByFechaCreacionDesc();
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
        PedidoTienda pedido = repositorioPedidoTienda.findByIdAndEliminadoFalse(pedidoId)
            .orElseThrow(() -> new IllegalArgumentException("No existe el pedido de tienda indicado."));
        pedido.setEstado(estado);
        repositorioPedidoTienda.save(pedido);
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
            .map(PedidoTienda::getResumenProductos)
            .filter(Objects::nonNull)
            .flatMap(resumen -> List.of(resumen.split(",")).stream())
            .map(String::trim)
            .filter(texto -> !texto.isBlank())
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
            .entrySet()
            .stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("No disponible");
    }
}
