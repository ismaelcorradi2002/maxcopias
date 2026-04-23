package com.maxcopias.service;

import com.maxcopias.model.EstadoPedidoCopisteria;
import com.maxcopias.model.EstadoPedidoTienda;
import com.maxcopias.model.PedidoCopisteria;
import com.maxcopias.model.PedidoTienda;
import com.maxcopias.repository.RepositorioPedidoCopisteria;
import com.maxcopias.repository.RepositorioPedidoTienda;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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
        return repositorioPedidoCopisteria.findAllByOrderByFechaCreacionDesc();
    }

    @Transactional(readOnly = true)
    public List<PedidoTienda> obtenerPedidosTienda() {
        return repositorioPedidoTienda.findAllByOrderByFechaCreacionDesc();
    }

    @Transactional
    public void cambiarEstadoCopisteria(Long pedidoId, EstadoPedidoCopisteria estado) {
        PedidoCopisteria pedido = repositorioPedidoCopisteria.findById(pedidoId)
            .orElseThrow(() -> new IllegalArgumentException("No existe el pedido de copisteria indicado."));
        pedido.setEstado(estado);
        repositorioPedidoCopisteria.save(pedido);
    }

    @Transactional
    public void cambiarEstadoTienda(Long pedidoId, EstadoPedidoTienda estado) {
        PedidoTienda pedido = repositorioPedidoTienda.findById(pedidoId)
            .orElseThrow(() -> new IllegalArgumentException("No existe el pedido de tienda indicado."));
        pedido.setEstado(estado);
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
            "pendientes", pedidosCopisteria.stream().filter(p -> p.getEstado() == EstadoPedidoCopisteria.RECIBIDO).count(),
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
            "preparando", pedidosTienda.stream().filter(p -> p.getEstado() == EstadoPedidoTienda.PREPARANDO).count(),
            "listos", pedidosTienda.stream().filter(p -> p.getEstado() == EstadoPedidoTienda.LISTO).count(),
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
}
